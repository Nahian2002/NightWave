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
        // Reuse Unicode/display preparation across strict, alias and typo
        // passes of THIS snapshot. No stale cross-query/library cache.
        val identities = tracks.map(SearchRelevancePolicy::prepareSaved)
        // Compute the expensive Unicode identity score once per saved row,
        // then select the BEST entry for duplicated URLs rather than retaining
        // a weaker early placeholder title. Stable ties keep saved order.
        val strict = tracks.withIndex().mapNotNull { (index, track) ->
            val score = SearchRelevancePolicy.savedScore(prepared, identities[index])
            if (score <= 0) null else Triple(track, score, index)
        }
        // A curated Romanized/Bangla spelling is a fallback ONLY if there
        // are no direct saved hits. This prevents approximate transliterations
        // burying an exact song title, while preserving every original URL.
        val aliasQueries = if (strict.isEmpty()) {
            SearchMultilingualAliasPolicy.alternatives(prepared.text)
                .map(SearchRelevancePolicy::prepare)
        } else emptyList()
        val aliasHits = if (aliasQueries.isEmpty()) emptyList() else
            tracks.withIndex().mapNotNull { (index, track) ->
                val best = aliasQueries.maxOf { SearchRelevancePolicy.savedScore(it, identities[index]) }
                val score = (best - 25).coerceAtLeast(0)
                if (score <= 0) null else Triple(track, score, index)
            }
        // Typos remain a LAST resort. Uncurated spelling and Bangla vowel
        // differences cannot become broad fuzzy library matches.
        val candidates = when {
            strict.isNotEmpty() -> strict
            aliasHits.isNotEmpty() -> aliasHits
            else -> tracks.withIndex().mapNotNull { (index, track) ->
                val score = SearchRelevancePolicy.savedScore(prepared, identities[index], allowTypo = true)
                if (score <= 0) null else Triple(track, score, index)
            }
        }
        return candidates.sortedWith(
            compareByDescending<Triple<VideoPreview, Int, Int>> { it.second }
                .thenBy { it.third }
        ).distinctBy { it.first.url }
            .take(limit.coerceAtMost(24)).map { it.first }
    }
}
