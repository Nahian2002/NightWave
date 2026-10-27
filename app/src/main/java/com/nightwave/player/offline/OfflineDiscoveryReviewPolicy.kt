package com.nightwave.player.offline

import java.util.Locale

/**
 * NW124: advisory ordering of *already eligible* Commons audio entries.
 * Filename word overlap is NOT song identity, license approval, authorship,
 * or permission to download a YouTube recording. Never hide a candidate,
 * rewrite a source URL or automatically initiate playback/download.
 */
object OfflineDiscoveryReviewPolicy {
    private fun words(raw: String): List<String> =
        FreeAudioCatalogPolicy.safeSearchTerm(raw)
            .lowercase(Locale.ROOT).split(' ')
            .filter { it.isNotBlank() }.take(12)

    private fun overlap(request: Set<String>, title: String): Int {
        if (request.isEmpty()) return 0
        return words(title).toSet().count { it in request }
    }

    /** Stable ties preserve Commons' provider order. Result is a new list. */
    fun rank(requestTitle: String, candidates: List<FreeAudioCandidate>): List<FreeAudioCandidate> {
        val requested = words(requestTitle).toSet()
        if (requested.isEmpty() || candidates.size < 2) return candidates.toList()
        return candidates.withIndex()
            .sortedWith(compareByDescending<IndexedValue<FreeAudioCandidate>> {
                overlap(requested, it.value.title)
            }.thenBy { it.index })
            .map { it.value }
    }

    /** Honest evidence description, never an assertion of recording equivalence. */
    fun reviewHint(requestTitle: String, candidateTitle: String): String {
        val requested = words(requestTitle).toSet()
        if (requested.isEmpty()) return "Independent recording. Review title, license and source."
        val matches = overlap(requested, candidateTitle)
        return if (matches == 0)
            "Different listing title. Review this recording and its license."
        else "$matches of ${requested.size} title words appear. This may be a different recording."
    }
}
