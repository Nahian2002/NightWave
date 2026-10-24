package com.nightwave.player.net

/** Bounded suggestions extracted exclusively from the user's saved public-video references.
 * No history is written and no provider is contacted while the user types.
 */
data class LocalSearchSuggestion(
    val query: String,
    val label: String,
    val detail: String,
    val kind: Kind
) {
    enum class Kind { SONG, CREATOR }
}

object LocalSearchSuggestions {
    fun suggest(query: String, saved: List<VideoPreview>, limit: Int = 6): List<LocalSearchSuggestion> {
        val prepared = SearchRelevancePolicy.prepare(query)
        if (!prepared.valid || limit <= 0 || saved.isEmpty()) return emptyList()
        val capped = limit.coerceAtMost(8)
        val output = mutableListOf<LocalSearchSuggestion>()
        val seenQueries = mutableSetOf<String>()

        // NW26: cover a much larger saved library rather than silently cutting\n        // off search suggestions after the first 400 references.\n        // Same local ranking used by saved-library search. Never invent an
        // "official" artist or album from a YouTube uploader label.
        SavedTrackSearch.match(query, saved.take(5_000), limit = 24).forEach { song ->
            val display = MusicIdentity.display(song.title, song.author)
            val title = display.title.trim()
            val key = SearchRelevancePolicy.normalize(title)
            if (key.isNotBlank() && seenQueries.add(key) && output.size < capped) {
                output.add(LocalSearchSuggestion(title.take(100), title.take(100),
                    display.artistOrCreator.take(70), LocalSearchSuggestion.Kind.SONG))
            }
        }
        if (output.size == capped) return output

        // Creator names are sourced only from actually saved distinct videos.
        // Match all terms to avoid irrelevant popular channels.
        MusicIdentity.creatorsFrom(saved.take(5_000), limit = 12).forEach { creator ->
            val key = SearchRelevancePolicy.normalize(creator.name)
            if (key.isNotBlank() &&
                prepared.terms.all { it in key } &&
                seenQueries.add(key) && output.size < capped) {
                output.add(LocalSearchSuggestion(
                    creator.name.take(100), creator.name.take(100),
                    "${creator.trackCount} saved videos · local creator",
                    LocalSearchSuggestion.Kind.CREATOR
                ))
            }
        }
        return output
    }
}
