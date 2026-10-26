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
        """\s*[\[(]\s*(?:official\s+(?:music\s+)?video|official\s+audio|(?:official\s+)?lyric(?:s)?\s+video|lyrics?|(?:official\s+)?visuali[sz]er)\s*[\])]\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val plainPromotion = Regex(
        """\s+[-–—|]\s+(?:official\s+(?:music\s+)?video|official\s+audio|(?:official\s+)?lyric(?:s)?\s+video|lyrics?|(?:official\s+)?visuali[sz]er)\s*$""",
        RegexOption.IGNORE_CASE
    )
    // A separator immediately before a stripped bracketed badge is part of
    // the badge, not the song name: "Artist - Song | [Official Video]".
    private val orphanedBadgeSeparator = Regex("""\s+[-–—|]\s*$""")
    private val creditedTitle = Regex("""^(.{2,72}?)\s+[-–—]\s+(.{2,180})$""")
    private val creatorSuffix = Regex("""(?i)(?:\s*[-–—]\s*topic|\s*vevo)$""")
    private val genericCreators = setOf(
        "youtube creator", "unknown creator", "unknown artist", "various artists"
    )

    private fun key(value: String): String =
        value.lowercase(Locale.ROOT).filter { char ->
            // Kotlin isLetterOrDigit alone removes Bangla dependent vowels.
            // Retain Unicode combining marks or two different artist names
            // could falsely corroborate each other.
            val category = Character.getType(char)
            char.isLetterOrDigit() ||
                category == Character.NON_SPACING_MARK.toInt() ||
                category == Character.COMBINING_SPACING_MARK.toInt() ||
                category == Character.ENCLOSING_MARK.toInt()
        }

    private fun safeLabel(value: String, fallback: String): String =
        value.replace('\u00A0', ' ').trim().ifEmpty { fallback }

    /** Videos-mode search retains the original provider caption, not a song guess. */
    fun videoDisplay(title: String, uploader: String): MusicDisplayIdentity =
        MusicDisplayIdentity(safeLabel(title, "Untitled video"),
            safeLabel(uploader, "Unknown creator"))

    fun display(title: String, uploader: String): MusicDisplayIdentity {
        val original = safeLabel(title, "Untitled video")
        val source = safeLabel(uploader, "Unknown creator")
        var clean = original
        // Several independent video-only labels may trail the same caption.
        // Bounded repetitions prevent stripping genuine Live, Remix, Covers,
        // featured artists, album names or arbitrary title text.
        repeat(4) {
            val withoutBadges = clean.replace(bracketedPromotion, "")
                .replace(plainPromotion, "").trim()
            val next = if (withoutBadges != clean)
                withoutBadges.replace(orphanedBadgeSeparator, "").trim() else clean
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
