package com.nightwave.player.net

/**
 * Pure checks for correct result visibility and provider-empty messaging.
 * A stale song search must never show when the listener switches to Videos.
 */
object SearchResultPresentationPolicy {
    fun matches(
        typedQuery: String, typedMode: SearchMode,
        resultQuery: String, resultMode: SearchMode
    ): Boolean {
        val input = SearchRelevancePolicy.prepare(typedQuery).text
        val result = SearchRelevancePolicy.prepare(resultQuery).text
        return input.isNotBlank() && input == result && typedMode == resultMode
    }

    data class Groups(
        val bestMatches: List<VideoPreview>,
        val moreResults: List<VideoPreview>,
        // All alternatives remain separately playable with their own URLs.
        val otherVersions: List<VideoPreview> = emptyList()
    )

    /**
     * Group ONLY SONGS mode; Videos keeps its own ranking and doesn't pretend
     * to be music. Every returned result remains visible and keeps its URL.
     * "Best" means metadata-likelihood, not verified recording identity.
     */
    fun groupMusic(query: String, mode: SearchMode, videos: List<VideoPreview>): Groups {
        if (mode != SearchMode.SONGS || videos.isEmpty())
            return Groups(emptyList(), videos)
        val prepared = SearchRelevancePolicy.prepare(query)
        if (!prepared.valid) return Groups(emptyList(), videos)
        val ranked = SearchRelevancePolicy.rankPublic(query, videos)
        val (versions, primary) = ranked.partition { SearchVersionGroupingPolicy.isOtherVersion(query, it) }
        val highlightable = primary.filter { SearchMusicIntentPolicy.canHighlightInSongs(prepared.text, it) }
        val strongest = highlightable.firstOrNull()?.let { SearchRelevancePolicy.publicScore(prepared, it) } ?: 0
        if (strongest < 160) return Groups(emptyList(), ranked)
        val best = highlightable.take(5).takeWhile { candidate ->
            SearchRelevancePolicy.publicScore(prepared, candidate) >=
                maxOf(160, strongest - 55)
        }
        return Groups(best, primary.filterNot(best.toHashSet()::contains), versions)
    }

    fun emptyHint(videos: List<VideoPreview>): String =
        if (videos.isEmpty()) {
            "No public results yet. Try another title or artist, change Songs/Videos, " +
                "or open YouTube search."
        } else ""
}
