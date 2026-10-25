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
        val moreResults: List<VideoPreview>
    )

    /**
     * Group ONLY SONGS mode; Videos retains provider order and doesn't pretend
     * to be music. Every returned result remains visible and keeps its URL.
     * "Best" means metadata-likelihood, not verified recording identity.
     */
    fun groupMusic(query: String, mode: SearchMode, videos: List<VideoPreview>): Groups {
        if (mode != SearchMode.SONGS || videos.isEmpty())
            return Groups(emptyList(), videos)
        val prepared = SearchRelevancePolicy.prepare(query)
        if (!prepared.valid) return Groups(emptyList(), videos)
        val ranked = SearchRelevancePolicy.rankPublic(query, videos)
        val strongest = SearchRelevancePolicy.publicScore(prepared, ranked.first())
        if (strongest < 160) return Groups(emptyList(), ranked)
        val best = ranked.take(5).takeWhile { candidate ->
            SearchRelevancePolicy.publicScore(prepared, candidate) >=
                maxOf(160, strongest - 55)
        }
        return Groups(best, ranked.drop(best.size))
    }

    fun emptyHint(videos: List<VideoPreview>): String =
        if (videos.isEmpty()) {
            "No public results returned for this search. Try a different title, " +
                "switch Songs/Videos, or use the official YouTube search."
        } else ""
}
