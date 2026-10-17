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
        return tracks.withIndex()
            .filter { (_, track) -> SearchRelevancePolicy.savedMatches(prepared, track) }
            .distinctBy { it.value.url }
            .sortedWith(
                compareByDescending<IndexedValue<VideoPreview>> {
                    SearchRelevancePolicy.savedScore(prepared, it.value)
                }.thenBy { it.index }
            )
            .take(limit.coerceIn(1, 24))
            .map { it.value }
    }
}
