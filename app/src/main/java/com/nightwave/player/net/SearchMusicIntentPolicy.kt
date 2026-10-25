package com.nightwave.player.net

/**
 * Conservative scoring hints for VIDEO search results.
 *
 * Uploader titles do not verify song identity, performers, ownership, or licensing.
 * Never rewrite VideoPreview/URL, filter an item, or label a channel "official".
 */
object SearchMusicIntentPolicy {
    private val partNumber = Regex(
        """\b(?:pt|part)\s+(one|two|three|four|five|i|ii|iii|iv|v|[1-5])\b"""
    )
    private val numbers = mapOf(
        "one" to "1", "i" to "1", "two" to "2", "ii" to "2",
        "three" to "3", "iii" to "3", "four" to "4", "iv" to "4",
        "five" to "5", "v" to "5"
    )
    private val separator = Regex("""\s+[-–—]\s+""")
    // Search uses both "don't" and "dont"; keep common contractions together
    // in scoring only, preserving all original displayed titles and URLs.
    private val contractions = mapOf(
        "don t" to "dont", "can t" to "cant", "won t" to "wont",
        "isn t" to "isnt", "aren t" to "arent", "doesn t" to "doesnt"
    )
    private val variants = listOf(
        "cover" to -70, "remix" to -48, "nightcore" to -65,
        "sped up" to -65, "slowed" to -55, "reverb" to -38,
        "8d" to -38, "instrumental" to -45, "karaoke" to -70,
        "live" to -25, "acoustic" to -25, "reaction" to -120,
        "review" to -120, "trailer" to -90, "movie clip" to -95,
        "movie" to -45, "scene" to -70, "tutorial" to -120
    )

    fun canonical(raw: String): String {
        var normalized = SearchRelevancePolicy.normalize(raw)
        contractions.forEach { (spaced, joined) ->
            normalized = normalized.replace(
                Regex("""\b""" + spaced + """\b"""), joined)
        }
        return partNumber.replace(normalized) { match ->
            numbers[match.groupValues[1]] ?: match.groupValues[1]
        }
    }

    /**
     * A dash-delimited credit is usable as an alternate title when ranking,
     * but never becomes a verified performer or a stored title.
     */
    fun titleCandidates(track: VideoPreview): List<String> {
        val display = MusicIdentity.display(track.title, track.author)
        val full = canonical(display.title)
        val credited = separator.split(display.title, limit = 2).getOrNull(1)
            ?.let(::canonical)
            ?.takeIf { it.isNotBlank() }
        return listOfNotNull(full, credited).distinct()
    }

    private fun hasPhrase(haystack: String, phrase: String): Boolean =
        (" " + haystack + " ").contains(" " + phrase + " ")

    fun variantPenalty(query: String, track: VideoPreview): Int {
        val requested = canonical(query)
        val title = canonical(track.title)
        return variants.sumOf { (cue, cost) ->
            if (hasPhrase(title, cue) && !hasPhrase(requested, cue)) cost else 0
        }.coerceAtLeast(-230)
    }

    fun titleSignal(query: String, track: VideoPreview): Int {
        val wanted = canonical(query)
        if (wanted.isBlank()) return 0
        val candidates = titleCandidates(track)
        return candidates.maxOfOrNull { title ->
            when {
                title == wanted -> 190
                title.startsWith("$wanted ") -> 115
                title.endsWith(" $wanted") -> 90
                title.contains(" $wanted ") -> 85
                title.contains(wanted) -> 58
                else -> 0
            }
        } ?: 0
    }
}
