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
        if (limit <= 0 || saved.isEmpty() || !SearchRelevancePolicy.prepare(query).valid) return emptyList()
        return fromMatches(query, saved,
            SavedTrackSearch.match(query, saved, limit = 24), limit)
    }

    /** Caller must provide the same query's complete-corpus ranked matches. */
    internal fun fromMatches(query: String, saved: List<VideoPreview>,
        matches: List<VideoPreview>, limit: Int = 6): List<LocalSearchSuggestion> {
        if (saved.isEmpty()) return emptyList()
        return fromPrepared(query, matches, prepareCreators(MusicIdentity.allCreatorsFrom(saved)), limit)
    }

    internal data class PreparedCreator(val creator: SavedCreatorShortcut, val key: String, val words: List<String>)
    internal fun prepareCreators(creators: List<SavedCreatorShortcut>): List<PreparedCreator> = creators.map {
        val key = SearchRelevancePolicy.normalize(it.name)
        PreparedCreator(it, key, SearchTokenMatchingPolicy.tokens(key))
    }

    internal fun fromPrepared(query: String, matches: List<VideoPreview>, creators: List<PreparedCreator>,
        limit: Int = 6, provenance: String = "saved videos · local creator"): List<LocalSearchSuggestion> {
        val prepared = SearchRelevancePolicy.prepare(query)
        if (!prepared.valid || limit <= 0) return emptyList()
        val capped = limit.coerceAtMost(8)
        val output = mutableListOf<LocalSearchSuggestion>()
        val seenQueries = mutableSetOf<String>()

        // Same local ranking used by saved-library search. Never invent an
        // "official" artist or album from a YouTube uploader label.
        matches.take(24).forEach { song ->
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
        creators.forEach { candidate ->
            val creator = candidate.creator
            val key = candidate.key
            if (key.isNotBlank() &&
                SearchTokenMatchingPolicy.allCovered(
                    prepared.terms, candidate.words) &&
                seenQueries.add(key) && output.size < capped) {
                output.add(LocalSearchSuggestion(
                    creator.name.take(100), creator.name.take(100),
                    "${creator.trackCount} $provenance",
                    LocalSearchSuggestion.Kind.CREATOR
                ))
            }
        }
        return output
    }
}
