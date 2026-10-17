package com.nightwave.player.v2.social

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

/** User-initiated text shares of canonical PUBLIC links, never local files. */
object PlaylistShareFormatter {
    const val MAX_LINKS = 30
    private const val MAX_INPUT_TRACKS = 500

    private fun safeLabel(value: String, limit: Int): String = value
        .filter { it.code >= 32 && it.code != 127 }
        .trim().take(limit)

    fun format(name: String, tracks: List<VideoPreview>): String? {
        val shareable = tracks.take(MAX_INPUT_TRACKS).mapNotNull { video ->
            // Refuse extra URL params, external hosts, offline IDs and signed streams.
            if (VideoLink.normalize(video.url) != video.url) null else video
        }.distinctBy { it.url }.take(MAX_LINKS)
        if (shareable.isEmpty()) return null
        val title = safeLabel(name, 60).ifBlank { "My playlist" }
        val lines = mutableListOf(
            "NightWave playlist: " + title,
            "Public song links (" + shareable.size + " shared):"
        )
        shareable.forEachIndexed { index, video ->
            val label = safeLabel(video.title, 90).ifBlank { "Untitled" }
            val creator = safeLabel(video.author, 60)
            val caption = if (creator.isBlank()) label else label + " — " + creator
            lines += (index + 1).toString() + ". " + caption
            lines += video.url
        }
        if (tracks.size > shareable.size) {
            lines += "Only the first 30 eligible public links are shared; " +
                "duplicates, local audio and unsupported links are omitted."
        }
        lines += "Shared by choice from NightWave. No offline audio is included."
        return lines.joinToString("\n")
    }
}
