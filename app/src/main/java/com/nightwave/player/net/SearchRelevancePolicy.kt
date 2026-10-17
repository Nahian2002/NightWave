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
    private val combining = Regex("\\p{M}+")
    private val spaces = Regex("\\s+")
    private val unhelpful = listOf(
        "reaction", "tutorial", "review", "commentary", "explained",
        "interview", "podcast", "behind the scenes"
    )

    fun normalize(value: String): String {
        val plain = Normalizer.normalize(value.take(512).lowercase(Locale.ROOT),
            Normalizer.Form.NFKD).replace(combining, "")
        return plain.map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("").replace(spaces, " ").trim()
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
        val covered = query.terms.count { it in id.searchable }
        var score = covered * 13
        if (covered == query.terms.size) score += 75
        if (id.title == query.text) score += 140
        else if (id.title.startsWith(query.text)) score += 100
        else if (query.text in id.title) score += 72
        if (id.artist == query.text) score += 85
        else if (id.artist.startsWith(query.text)) score += 40
        else if (query.text in id.artist) score += 26
        if (query.terms.all { it in id.title }) score += 30
        if (track.durationSeconds in 90L..660L) score += 6
        val original = normalize(track.title)
        if ("official audio" in original || "official music video" in original) score += 8
        // If the listener actually searches for "reaction", do not suppress it.
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
