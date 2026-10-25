package com.nightwave.player.net

/**
 * Deterministic, device-local search over already-saved public references.
 * Relevance is shared with optional music quick results. The actual VideoPreview
 * and playable URL are returned unchanged; no network access or playlist edits.
 */
object SavedTrackSearch {
    fun match(query: String, tracks: List<VideoPreview>, limit: Int = 24): List<VideoPreview> {
        val prepared = SearchRelevancePolicy.prepare(query)
        if (!prepared.valid) return emptyList()
        if (limit <= 0) return emptyList()
        // Compute the expensive Unicode identity score once per saved row,
        // then select the BEST entry for duplicated URLs rather than retaining
        // a weaker early placeholder title. Stable ties keep saved order.
        return tracks.withIndex().mapNotNull { (index, track) ->
            val score = SearchRelevancePolicy.savedScore(prepared, track)
            if (score <= 0) null else Triple(track, score, index)
        }.sortedWith(compareByDescending<Triple<VideoPreview, Int, Int>> { it.second }
            .thenBy { it.third })
            .distinctBy { it.first.url }
            .take(limit.coerceAtMost(24))
            .map { it.first }
    }
}
