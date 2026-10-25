package com.nightwave.player.net

import java.util.Locale

/**
 * Conservative DISPLAY-ONLY identity for public video tracks.
 *
 * YouTube supplies video titles and uploader/channel names, not verified
 * recording, label, artist or album IDs. This intentionally never rewrites a
 * VideoPreview, stored library record, public URL or playback/backup metadata.
 */
data class MusicDisplayIdentity(
    val title: String,
    val artistOrCreator: String
)

data class SavedCreatorShortcut(val name: String, val trackCount: Int)

/** Safe formatting and locally derived creator shortcuts, without network I/O. */
object MusicIdentity {
    // Only explicitly labeled *trailing* wrappers. Do not strip live,
    // acoustic, remix, cover, karaoke, or other important song distinctions.
    private val bracketedPromotion = Regex(
        """\s*[\[(]\s*(?:official\s+(?:music\s+)?video|official\s+audio|lyric\s+video|lyrics?)\s*[\])]\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val plainPromotion = Regex(
        """\s+[-|]\s+(?:official\s+(?:music\s+)?video|official\s+audio)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val creditedTitle = Regex("""^(.{2,72}?)\s+[-–—]\s+(.{2,180})$""")
    private val creatorSuffix = Regex("""(?i)(?:\s*-\s*topic|\s*vevo)$""")
    private val genericCreators = setOf(
        "youtube creator", "unknown creator", "unknown artist", "various artists"
    )

    private fun key(value: String): String =
        value.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    fun display(title: String, uploader: String): MusicDisplayIdentity {
        val original = title.trim().ifEmpty { "Untitled video" }
        val source = uploader.trim().ifEmpty { "Unknown creator" }
        var clean = original
        // Repeated suffixes sometimes appear; no broad keyword stripping.
        repeat(2) {
            val next = clean.replace(bracketedPromotion, "").replace(plainPromotion, "").trim()
            if (next.isNotBlank()) clean = next
        }

        val parsed = creditedTitle.matchEntire(clean)
        if (parsed != null) {
            val prefix = parsed.groupValues[1].trim()
            val remainder = parsed.groupValues[2].trim()
            val normalizedSource = source.replace(creatorSuffix, "").trim()
            // Only attribute a split title to an artist when source identity
            // independently corroborates it (e.g. EllieGouldingVEVO).
            if (key(prefix).isNotBlank() && key(prefix) == key(normalizedSource) &&
                remainder.isNotBlank()) {
                return MusicDisplayIdentity(remainder, prefix)
            }
        }
        return MusicDisplayIdentity(clean, source)
    }

    /** Counts DISTINCT, actually saved/recent public videos, never inferred plays. */
    fun creatorsFrom(
        tracks: List<VideoPreview>, limit: Int = 8, scanLimit: Int = 350
    ): List<SavedCreatorShortcut> {
        if (limit <= 0 || scanLimit <= 0) return emptyList()
        val names = linkedMapOf<String, Pair<String, Int>>()
        // The usual Home shortcuts stay at the existing 350-item budget.
        // NW70 can explicitly scan more saved references on a background
        // dispatcher, without silently losing creators past item 350.
        tracks.take(scanLimit.coerceAtMost(5_000))
            .distinctBy { it.url }.forEach { video ->
            val label = display(video.title, video.author).artistOrCreator.trim()
            val normalized = label.lowercase(Locale.ROOT)
            if (normalized.isBlank() || normalized in genericCreators) return@forEach
            val id = key(label)
            if (id.isBlank()) return@forEach
            val prev = names[id]
            names[id] = if (prev == null) label to 1 else prev.first to (prev.second + 1)
        }
        return names.values
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }
                .thenBy { it.first.lowercase(Locale.ROOT) })
            .take(limit.coerceAtMost(12))
            .map { SavedCreatorShortcut(it.first, it.second) }
    }
}
