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

    fun emptyHint(videos: List<VideoPreview>): String =
        if (videos.isEmpty()) {
            "No public results returned for this search. Try a different title, " +
                "switch Songs/Videos, or use the official YouTube search."
        } else ""
}
