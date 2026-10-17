package com.nightwave.player.v2.social

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

/** Strictly parsed 6M1 shares, never fetched or trusted as verified metadata. */
data class SharedPlaylist(val name: String, val tracks: List<VideoPreview>)

object PlaylistShareParser {
    const val PREFIX = "NightWave playlist: "
    const val MAX_TEXT_CHARS = 12_000
    private const val FOOTER =
        "Shared by choice from NightWave. No offline audio is included."
    private const val OMITTED =
        "Only the first 30 eligible public links are shared; " +
        "duplicates, local audio and unsupported links are omitted."
    private val countLine = Regex("""Public song links \(([1-9]|[12][0-9]|30) shared\):""")

    fun parse(input: String): SharedPlaylist? {
        if (input.length !in 1..MAX_TEXT_CHARS) return null
        val normalized = input.trim().replace("\r\n", "\n")
        if ('\r' in normalized) return null
        val rows = normalized.split('\n')
        if (rows.size !in 5..64 || !rows[0].startsWith(PREFIX)) return null
        val name = rows[0].removePrefix(PREFIX)
        if (name.isBlank() || name.length > 60 ||
            name.any { it.code < 32 || it.code == 127 }
        ) return null
        val count = countLine.matchEntire(rows[1])?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val extraAt = 2 + count * 2
        if (rows.size != extraAt + 1 && rows.size != extraAt + 2) return null
        if (rows.last() != FOOTER) return null
        if (rows.size == extraAt + 2 && rows[extraAt] != OMITTED) return null

        val found = ArrayList<VideoPreview>(count)
        val urls = HashSet<String>()
        for (i in 0 until count) {
            val itemLine = rows[2 + 2 * i]
            val itemPrefix = (i + 1).toString() + ". "
            if (!itemLine.startsWith(itemPrefix)) return null
            val caption = itemLine.removePrefix(itemPrefix)
            if (caption.isBlank() || caption.length > 153 ||
                caption.any { it.code < 32 || it.code == 127 }
            ) return null
            val url = rows[3 + 2 * i]
            if (VideoLink.normalize(url) != url || !urls.add(url)) return null
            // Captions are unverified hints. No network request is made here.
            val divider = caption.lastIndexOf(" — ")
            val title = if (divider >= 0) caption.substring(0, divider) else caption
            val creator = if (divider >= 0) caption.substring(divider + 3)
                else "Shared via NightWave"
            if (title.isBlank() || title.length > 153 ||
                creator.isBlank() || creator.length > 60
            ) return null
            found += VideoPreview(title, creator, url, "", 0L)
        }
        return SharedPlaylist(name, found)
    }
}
