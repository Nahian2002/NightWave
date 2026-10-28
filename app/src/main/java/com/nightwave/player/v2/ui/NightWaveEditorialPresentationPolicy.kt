package com.nightwave.player.v2.ui

import com.nightwave.player.net.MusicDisplayIdentity
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.v2.recommendation.PersonalMix

/** Display-only headlines. Original captions, identities, URLs and playback records are never rewritten. */
internal object NightWaveEditorialPresentationPolicy {
    private val edition = Regex("(?iu)(?:\\b(?:live|remix|acoustic|cover|karaoke|instrumental|remaster(?:ed)?|sped[ -]?up|slowed|version|edit)\\b|লাইভ|রিমিক্স|কভার)")
    private val promotion = Regex("(?iu)(?:official (?:music )?(?:video|audio)|full (?:video|song)|(?:bengali|hindi|bangla) (?:movie )?song|movie song|lyrics? video|music video)")
    fun identity(title: String, creator: String): MusicDisplayIdentity {
        val base = MusicIdentity.display(title, creator)
        val parts = base.title.split(Regex("\\s+\\|\\s+"))
        if (parts.size < 3 || parts.first().length !in 4..96 || !promotion.containsMatchIn(title)) return base
        val editions = parts.drop(1).filter { edition.containsMatchIn(it) }.distinct()
        return base.copy(title = (listOf(parts.first().trim()) + editions).joinToString(" · "))
    }

    /** Real complete durations only; partial/unknown durations never imply a full mix length. */
    fun mixFacts(mix: PersonalMix): String {
        val tracks = mix.tracks.distinctBy { it.url }
        val creators = tracks.map { it.author.trim().lowercase(java.util.Locale.ROOT) }
            .filter { it.isNotBlank() && !it.startsWith("unknown") && it != "youtube creator" }.distinct().size
        val duration = if (tracks.isNotEmpty() && tracks.all { it.durationSeconds in 1..604800 }) {
            val seconds = tracks.sumOf { it.durationSeconds }
            if (seconds < 60) "$seconds sec" else "≈${(seconds + 30) / 60} min"
        } else ""
        val people = when (creators) { 0 -> ""; 1 -> "1 creator"; else -> "$creators creators" }
        return listOf(duration, people).filter { it.isNotEmpty() }.joinToString(" · ")
    }
}
