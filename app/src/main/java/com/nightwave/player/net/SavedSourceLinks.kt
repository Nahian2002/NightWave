package com.nightwave.player.net

import android.content.Context
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A user bookmark contains the address only, with no cached provider media or metadata. */
internal data class SavedSourceLink(val source: DiscoverySource, val url: HttpUrl) {
    companion object {
        fun from(source: DiscoverySource, address: String): SavedSourceLink? {
            if (source != DiscoverySource.PEERTUBE && source != DiscoverySource.RADIO) return null
            val url = DiscoverySources.externalUrl(address) ?: return null
            if (source == DiscoverySource.PEERTUBE) {
                val path = url.pathSegments
                val watch = path.size == 3 && path.take(2) == listOf("videos", "watch")
                val short = path.size == 2 && path.first() == "w"
                if ((!watch && !short) || !path.last().matches(Regex("[a-zA-Z0-9-]{1,40}"))) return null
            }
            return SavedSourceLink(source, url)
        }
    }
}

internal object SavedSourceLinksCodec {
    const val MAX_LINKS = 100

    fun decode(raw: String): List<SavedSourceLink>? {
        if (raw.length > 230_000) return null
        return try {
            val rows = JSONArray(raw)
            if (rows.length() > MAX_LINKS) return null
            (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                val source = DiscoverySource.entries.firstOrNull { it.name == row.getString("source") }
                    ?: return null
                SavedSourceLink.from(source, row.getString("url")) ?: return null
            }.takeIf { it.distinct().size == it.size }
        } catch (_: JSONException) {
            null
        }
    }

    fun encode(links: List<SavedSourceLink>): String = JSONArray().apply {
        links.forEach { link ->
            put(JSONObject().put("source", link.source.name).put("url", link.url.toString()))
        }
    }.toString()
}

internal class SavedSourceLinks(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nightwave_source_links_v1", Context.MODE_PRIVATE)

    fun read(): List<SavedSourceLink>? {
        return try {
            SavedSourceLinksCodec.decode(prefs.getString("links", "[]") ?: return null)
        } catch (_: ClassCastException) {
            null
        }
    }

    fun toggle(link: SavedSourceLink): List<SavedSourceLink>? = synchronized(lock) {
        val current = read() ?: return@synchronized null
        val updated = if (link in current) current - link else {
            if (current.size == SavedSourceLinksCodec.MAX_LINKS) return@synchronized null
            listOf(link) + current
        }
        if (prefs.edit().putString("links", SavedSourceLinksCodec.encode(updated)).commit()) updated else null
    }

    companion object {
        private val lock = Any()
    }
}
