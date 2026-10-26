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
        val (versions, remaining) = ranked.drop(best.size).partition { track ->
            SearchVersionGroupingPolicy.isOtherVersion(query, track)
        }
        return Groups(best, remaining, versions)
    }

    fun emptyHint(videos: List<VideoPreview>): String =
        if (videos.isEmpty()) {
            "No public results yet. Try another title or artist, change Songs/Videos, " +
                "or open YouTube search."
        } else ""
}
