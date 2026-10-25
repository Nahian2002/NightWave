package com.nightwave.player.net

import java.text.Normalizer
import java.util.Locale

/**
 * Shared pure ranking policy for saved public references and provider results.
 * It only compares supplied labels; it cannot create artists, albums, URLs or
 * playback entries and never accesses network, private audio or preferences.
 */
object SearchRelevancePolicy {
    data class Query(val text: String, val terms: List<String>) {
        val valid: Boolean get() = terms.isNotEmpty()
    }

    // Bounded normalization: punctuation becomes word boundaries and Latin
    // combining accents are removed. Original saved/provider metadata is intact.
    // Only fold marks attached to Latin base letters. Removing *all* marks
    // destroyed Bengali vowel signs and made distinct Bangla titles collide.
    private val latinDiacritic = Regex("(?<=\\p{IsLatin})\\p{M}+")
    private val boundaries = Regex("[^\\p{L}\\p{M}\\p{N}]+")
    private val spaces = Regex("\\s+")
    private val unhelpful = listOf(
        "reaction", "tutorial", "review", "commentary", "explained",
        "interview", "podcast", "behind the scenes"
    )

    fun normalize(value: String): String {
        val plain = Normalizer.normalize(value.take(512).lowercase(Locale.ROOT),
            Normalizer.Form.NFKD)
        return latinDiacritic.replace(plain, "").replace(boundaries, " ")
            .replace(spaces, " ").trim()
    }

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

    fun savedMatches(query: Query, track: VideoPreview): Boolean {
        if (!query.valid) return false
        val all = identity(track).searchable
        return query.terms.all { it in all }
    }

    /** Deterministic evidence-based ordering, with no model-generated artist IDs. */
    fun savedScore(query: Query, track: VideoPreview): Int {
        if (!query.valid) return 0
        val id = identity(track)
        if (!query.terms.all { it in id.searchable }) return 0
        val exactTitle = id.title == query.text
        val exactArtist = id.artist == query.text
        return when {
            exactTitle -> 250
            exactArtist -> 205
            id.title.startsWith(query.text) -> 180
            id.artist.startsWith(query.text) -> 155
            query.text in id.title -> 125
            query.text in id.artist -> 115
            query.terms.all { it in id.title } -> 95
            query.terms.all { it in id.artist } -> 85
            else -> 65
        }
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
        val covered = terms.count { term ->
            if (term.all(Char::isDigit))
                searchable.split(' ').any { it == term }
            else term in searchable
        }
        var score = covered * 13
        if (covered == terms.size) score += 75
        if (title == wanted) score += 140
        else if (title.startsWith(wanted)) score += 100
        else if (wanted in title) score += 72
        if (artist == wanted) score += 85
        else if (artist.startsWith(wanted)) score += 40
        else if (wanted in artist) score += 26
        if (terms.all { it in title }) score += 30
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
