package com.nightwave.player.net

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject

internal enum class DiscoverySource(val displayName: String, val kind: DiscoveryKind, val available: Boolean = true) {
    AUDIUS("Audius", DiscoveryKind.TRACK),
    PEERTUBE("PeerTube", DiscoveryKind.VIDEO),
    RADIO("Radio Browser", DiscoveryKind.LIVE_RADIO),
    JAMENDO("Jamendo", DiscoveryKind.TRACK, available = false)
}

internal enum class DiscoveryKind { TRACK, VIDEO, LIVE_RADIO }
internal enum class DiscoveryPermission { AVAILABLE, UNAVAILABLE, UNKNOWN }

internal data class DiscoveryCapabilities(
    val search: DiscoveryPermission = DiscoveryPermission.UNKNOWN,
    val openExternally: DiscoveryPermission = DiscoveryPermission.UNKNOWN,
    val playInApp: DiscoveryPermission = DiscoveryPermission.UNKNOWN,
    val offline: DiscoveryPermission = DiscoveryPermission.UNKNOWN
)

internal data class DiscoveryItemId(val source: DiscoverySource, val remoteId: String)

internal data class DiscoveryItem(
    val id: DiscoveryItemId,
    val title: String,
    val creator: String?,
    val canonicalUrl: HttpUrl,
    val durationSeconds: Int?,
    val capabilities: DiscoveryCapabilities
)

internal object DiscoverySources {
    private const val MAX_RESULTS = 20
    private val uuid = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    private val audiusId = Regex("[A-Za-z0-9]{1,32}")
    private val shortUuid = Regex("[A-Za-z0-9]{1,40}")
    private val hostLabel = Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")
    private val privateSuffixes = setOf("local", "localhost", "internal", "test", "invalid", "example", "onion")
    private val capabilities = DiscoveryCapabilities(
        search = DiscoveryPermission.AVAILABLE,
        openExternally = DiscoveryPermission.AVAILABLE,
        offline = DiscoveryPermission.UNAVAILABLE
    )

    fun query(raw: String): String? = raw.trim().takeIf {
        it.isNotEmpty() && it.length <= 120 && it.none(Char::isISOControl)
    }

    fun endpoint(source: DiscoverySource, query: String): HttpUrl? {
        val base = when (source) {
            DiscoverySource.AUDIUS -> "https://api.audius.co/v1/tracks/search"
            DiscoverySource.PEERTUBE -> "https://sepiasearch.org/api/v1/search/videos"
            DiscoverySource.RADIO -> "https://de1.api.radio-browser.info/json/stations/search"
            DiscoverySource.JAMENDO -> return null
        }
        val builder = base.toHttpUrl().newBuilder()
        when (source) {
            DiscoverySource.AUDIUS -> builder.addQueryParameter("query", query)
                .addQueryParameter("limit", MAX_RESULTS.toString()).addQueryParameter("app_name", "NightWave")
            DiscoverySource.PEERTUBE -> builder.addQueryParameter("search", query)
                .addQueryParameter("count", MAX_RESULTS.toString()).addQueryParameter("nsfw", "false")
            DiscoverySource.RADIO -> builder.addQueryParameter("name", query)
                .addQueryParameter("limit", MAX_RESULTS.toString()).addQueryParameter("hidebroken", "true")
                .addQueryParameter("order", "votes").addQueryParameter("reverse", "true")
            DiscoverySource.JAMENDO -> return null
        }
        return builder.build()
    }

    fun decode(source: DiscoverySource, payload: String): List<DiscoveryItem> {
        val data = when (source) {
            DiscoverySource.RADIO -> JSONArray(payload)
            DiscoverySource.AUDIUS, DiscoverySource.PEERTUBE -> JSONObject(payload).getJSONArray("data")
            DiscoverySource.JAMENDO -> return emptyList()
        }
        return (0 until minOf(data.length(), MAX_RESULTS)).mapNotNull { index ->
            val row = data.optJSONObject(index) ?: return@mapNotNull null
            when (source) {
                DiscoverySource.AUDIUS -> audius(row)
                DiscoverySource.PEERTUBE -> peerTube(row)
                DiscoverySource.RADIO -> radio(row)
                DiscoverySource.JAMENDO -> null
            }
        }.distinctBy(DiscoveryItem::id)
    }

    private fun audius(row: JSONObject): DiscoveryItem? {
        val id = (row.opt("id") as? String)?.takeIf(audiusId::matches) ?: return null
        if (row.optBoolean("is_unlisted", true) || row.optBoolean("is_delete")) return null
        val path = row.optString("permalink")
        if (!path.startsWith("/") || path.startsWith("//") || path.contains('\\')) return null
        val url = externalUrl("https://audius.co$path") ?: return null
        if (url.host != "audius.co" || url.pathSegments.size != 2 || url.pathSegments.any(String::isBlank)) return null
        return item(DiscoverySource.AUDIUS, id, row.text("title"), row.optJSONObject("user")?.text("name"),
            url, row.duration())
    }

    private fun peerTube(row: JSONObject): DiscoveryItem? {
        val id = row.optString("uuid").takeIf(uuid::matches) ?: return null
        if (row.optBoolean("nsfw", true) || row.optJSONObject("privacy")?.optInt("id") != 1) return null
        val url = externalUrl(row.optString("url")) ?: return null
        val short = row.optString("shortUUID").takeIf(shortUuid::matches)
        val path = url.pathSegments
        if (path != listOf("videos", "watch", id) && (short == null || path != listOf("w", short))) return null
        return item(DiscoverySource.PEERTUBE, id.lowercase(java.util.Locale.ROOT), row.text("name"),
            row.optJSONObject("channel")?.text("displayName") ?: row.optJSONObject("account")?.text("displayName"),
            url, row.duration().takeUnless { row.optBoolean("isLive") })
    }

    private fun radio(row: JSONObject): DiscoveryItem? {
        val id = row.optString("stationuuid").takeIf(uuid::matches) ?: return null
        if (row.optInt("lastcheckok") != 1) return null
        val homepage = externalUrl(row.optString("homepage")) ?: return null
        return item(DiscoverySource.RADIO, id.lowercase(java.util.Locale.ROOT), row.text("name"),
            row.text("countrycode"), homepage, null)
    }

    private fun item(source: DiscoverySource, id: String, title: String?, creator: String?, url: HttpUrl,
        duration: Int?): DiscoveryItem? = title?.let {
        DiscoveryItem(DiscoveryItemId(source, id), it, creator, url, duration, capabilities)
    }

    private fun JSONObject.text(key: String): String? = (opt(key) as? String)?.trim()?.take(160)
        ?.takeIf { it.isNotEmpty() && it.none { char ->
            char.isISOControl() || char in '\u202a'..'\u202e' || char in '\u2066'..'\u2069'
        } }

    private fun JSONObject.duration() = optInt("duration").takeIf { it in 1..86_400 }

    internal fun externalUrl(raw: String): HttpUrl? {
        if (raw.length > 2_048 || raw.any { it.isISOControl() || it == '\\' }) return null
        val url = raw.toHttpUrlOrNull() ?: return null
        val labels = url.host.split('.')
        if (!url.isHttps || url.port != 443 || url.username.isNotEmpty() || url.password.isNotEmpty() ||
            labels.size < 2 || labels.any { !hostLabel.matches(it) } ||
            labels.last().none(Char::isLetter) || labels.last() in privateSuffixes) {
            return null
        }
        return url
    }
}
