package com.nightwave.player.net


/**
 * Shared pure ranking policy for saved public references and provider results.
 * It only compares supplied labels; it cannot create artists, albums, URLs or
 * playback entries and never accesses network, private audio or preferences.
 */
object SearchRelevancePolicy {
    data class Query(val text: String, val terms: List<String>) {
        val valid: Boolean get() = terms.isNotEmpty()
    }

    // Share the exact same search key for UI queries, saved music and
    // online music-score comparison. Unicode vowel marks are not disposable.
    private val unhelpful = listOf(
        "reaction", "tutorial", "review", "commentary", "explained",
        "interview", "podcast", "behind the scenes"
    )

    fun normalize(value: String): String = SearchScriptNormalization.normalize(value)

    fun prepare(raw: String): Query {
        val text = normalize(raw.take(160))
        return Query(text, text.split(' ').filter { it.isNotBlank() }.take(16))
    }

    private data class Identity(val title: String, val artist: String, val searchable: String)

    private fun identity(track: VideoPreview): Identity {
        val display = MusicIdentity.display(track.title, track.author)
        val title = normalize(display.title)
        val artist = normalize(display.artistOrCreator)
        // Unmodified uploader and raw titles remain searchable even when
        // conservative display cleanup removes only promotional suffixes.
        val all = listOf(title, artist, normalize(track.title), normalize(track.author))
            .joinToString(" ")
        return Identity(title, artist, all)
    }

    fun savedMatches(query: Query, track: VideoPreview): Boolean =
        savedScore(query, track) > 0

    /**
     * Prefer full words and real long-prefix matches. A typo candidate is
     * available only as a separate fallback when no strict saved hits exist.
     * Neither mode merges Bangla vowel spellings or matches short substrings.
     */
    fun savedScore(query: Query, track: VideoPreview, allowTypo: Boolean = false): Int {
        if (!query.valid) return 0
        val id = identity(track)
        val allWords = SearchTokenMatchingPolicy.tokens(id.searchable)
        val strengths = query.terms.map {
            SearchTokenMatchingPolicy.evidence(it, allWords, allowTypo)
        }
        if (strengths.any { it == 0 }) return 0
        val exactTitle = id.title == query.text
        val exactArtist = id.artist == query.text
        val base = when {
            exactTitle -> 250
            exactArtist -> 205
            id.title.startsWith(query.text) -> 180
            id.artist.startsWith(query.text) -> 155
            SearchTokenMatchingPolicy.allCovered(
                query.terms, SearchTokenMatchingPolicy.tokens(id.title)) -> 125
            SearchTokenMatchingPolicy.allCovered(
                query.terms, SearchTokenMatchingPolicy.tokens(id.artist)) -> 115
            else -> 65
        }
        // Typo-only fallbacks must never outrank a genuine title/artist match.
        return if (strengths.any { it == 1 }) minOf(base, 70) else base
    }

    /**
     * Rank optional quick *music* results only. VIDEO mode deliberately uses
     * original provider order; nothing is removed or marked as verified music.
     */
    fun publicScore(query: Query, track: VideoPreview): Int {
        if (!query.valid) return 0
        val id = identity(track)
        // Canonicalize only for scoring, never for the actual song metadata.
        // A user searching "Alone 2" can reach "Alone, Pt. II" without
        // treating unrelated Home Alone 2 clips as the best music match.
        val wanted = SearchMusicIntentPolicy.canonical(query.text)
        val title = SearchMusicIntentPolicy.canonical(id.title)
        val artist = SearchMusicIntentPolicy.canonical(id.artist)
        val searchable = SearchMusicIntentPolicy.canonical(id.searchable)
        val terms = prepare(wanted).terms
        if (terms.isEmpty()) return 0
        val allWords = SearchTokenMatchingPolicy.tokens(searchable)
        // Do not treat "alo" as contained in "alone"; only long Latin
        // tokens can use single-edit typo evidence. Digits match exactly.
        val strength = terms.map { term ->
            if (term.all(Char::isDigit)) {
                if (term in allWords) 3 else 0
            } else SearchTokenMatchingPolicy.evidence(term, allWords, allowTypo = true)
        }
        val covered = strength.count { it > 0 }
        var score = strength.sum() * 9
        if (covered == terms.size) {
            score += 75
            if (strength.any { it == 1 }) score += 55
        }
        // Exact/prefix song and artist boosts require full token coverage.
        // Without this, "alo" earns a misleading 100-point boost on "alone".
        if (covered == terms.size) {
            if (title == wanted) score += 140
            else if (title.startsWith(wanted)) score += 100
            else if (wanted.length >= 4 && wanted in title) score += 72
            if (artist == wanted) score += 85
            else if (artist.startsWith(wanted)) score += 40
            else if (wanted.length >= 4 && wanted in artist) score += 26
        }
        if (SearchTokenMatchingPolicy.allCovered(
                terms, SearchTokenMatchingPolicy.tokens(title))) score += 30
        if (track.durationSeconds in 90L..660L) score += 6
        val original = normalize(track.title)
        if ("official audio" in original || "official music video" in original) score += 8
        // Metadata is unverified: these cues are weak ranking hints only.
        score += SearchMusicIntentPolicy.titleSignal(wanted, track)
        score += SearchMusicIntentPolicy.variantPenalty(wanted, track)
        // If the listener explicitly requests a reaction, do not suppress it.
        if (unhelpful.any { phrase ->
            phrase in original && phrase !in query.text
        }) score -= 110
        return score
    }

    fun rankPublic(query: String, results: List<VideoPreview>): List<VideoPreview> {
        val prepared = prepare(query)
        if (!prepared.valid) return results
        return results.withIndex().sortedWith(
            compareByDescending<IndexedValue<VideoPreview>> { publicScore(prepared, it.value) }
                .thenBy { it.index }
        ).map { it.value }
    }
}
