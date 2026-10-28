package com.nightwave.player.net

import java.util.PriorityQueue

/** Query-local ranking over immutable prepared metadata; original source objects survive. */
object SavedTrackSearch {
    data class Matches(val tracks: List<VideoPreview>, val total: Int)
    private data class Hit(val track: VideoPreview, val score: Int, val position: Int)
    private val bestFirst = compareByDescending<Hit> { it.score }.thenBy { it.position }

    class Prepared internal constructor(
        private val tracks: List<VideoPreview>,
        private val identities: List<SearchRelevancePolicy.SavedIdentity>
    ) {
        fun find(query: String, limit: Int = 24, checkpoint: () -> Unit = {}): Matches {
            return findImpl(query, limit.coerceAtMost(24), checkpoint)
        }

        /** Full ranked membership for the paged local screen, not a playback queue. */
        fun all(query: String, checkpoint: () -> Unit = {}): Matches = findImpl(query, tracks.size, checkpoint)

        private fun findImpl(query: String, limit: Int, checkpoint: () -> Unit): Matches {
            val prepared = SearchRelevancePolicy.prepare(query)
            if (!prepared.valid || limit <= 0) return Matches(emptyList(), 0)
            fun collect(queries: List<SearchRelevancePolicy.Query>, typo: Boolean, penalty: Int): Map<String, Hit> {
                val best = linkedMapOf<String, Hit>()
                tracks.forEachIndexed { i, track ->
                    if (i % 128 == 0) checkpoint()
                    val score = (queries.maxOf { SearchRelevancePolicy.savedScore(it, identities[i], typo) } - penalty)
                        .coerceAtLeast(0)
                    if (score > 0) {
                        val hit = Hit(track, score, i)
                        val previous = best[track.url]
                        if (previous == null || bestFirst.compare(hit, previous) < 0) best[track.url] = hit
                    }
                }
                return best
            }
            // Strict -> curated alias -> one-edit fallback, across the WHOLE
            // corpus. A later duplicate must be scored before deduplication.
            var candidates = collect(listOf(prepared), false, 0)
            if (candidates.isEmpty()) {
                val aliases = SearchMultilingualAliasPolicy.alternatives(prepared.text).map(SearchRelevancePolicy::prepare)
                if (aliases.isNotEmpty()) candidates = collect(aliases, false, 25)
            }
            if (candidates.isEmpty()) candidates = collect(listOf(prepared), true, 0)
            val capacity = minOf(limit, candidates.size)
            if (capacity == 0) return Matches(emptyList(), 0)
            if (capacity == candidates.size) {
                checkpoint()
                return Matches(candidates.values.sortedWith(bestFirst).map { it.track }, candidates.size)
            }
            val heap = PriorityQueue(capacity, bestFirst.reversed())
            candidates.values.forEachIndexed { i, hit ->
                if (i % 128 == 0) checkpoint()
                if (heap.size < capacity) heap.add(hit)
                else if (bestFirst.compare(hit, heap.peek()) < 0) { heap.poll(); heap.add(hit) }
            }
            checkpoint()
            return Matches(heap.sortedWith(bestFirst).map { it.track }, candidates.size)
        }
    }

    fun prepare(tracks: List<VideoPreview>, checkpoint: () -> Unit = {}): Prepared {
        val copy = tracks.toList()
        val identities = copy.mapIndexed { i, track ->
            if (i % 128 == 0) checkpoint()
            SearchRelevancePolicy.prepareSaved(track)
        }
        checkpoint()
        return Prepared(copy, identities)
    }

    fun match(query: String, tracks: List<VideoPreview>, limit: Int = 24): List<VideoPreview> {
        if (limit <= 0 || !SearchRelevancePolicy.prepare(query).valid) return emptyList()
        return prepare(tracks).find(query, limit).tracks
    }
}
