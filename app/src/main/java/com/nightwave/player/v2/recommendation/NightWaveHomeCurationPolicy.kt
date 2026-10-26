package com.nightwave.player.v2.recommendation

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

/**
 * NW84: a music-first, read-only Home presentation. Never generates a song,
 * fetches recommendations, infers tastes or writes listening history.
 *
 * Real saved liked/playlist references that are not in Recent are featured
 * first. Today's rotation varies ties; original metadata/URLs are untouched.
 * Listening memory is not accepted here, so disabled/private statistics
 * can never change these selections.
 */
object NightWaveHomeCurationPolicy {
    const val MAX_QUICK_PICKS = 6
    const val MAX_MIX_CARDS = 5

    data class Plan(
        val nextListens: List<VideoPreview>,
        val mixes: List<PersonalMix>
    )

    fun curate(
        favorites: List<VideoPreview>,
        recent: List<VideoPreview>,
        playlistTracks: List<VideoPreview>,
        dailyMixes: List<PersonalMix>,
        personalMixes: List<PersonalMix>,
        dayNumber: Long
    ): Plan {
        val recentUrls = recent.asSequence().take(150).map { it.url }.toSet()
        val liked = favorites.asSequence().take(300)
            .filter { VideoLink.normalize(it.url) == it.url }.toList()
        val playlists = playlistTracks.asSequence().take(400)
            .filter { VideoLink.normalize(it.url) == it.url }.toList()
        val favoriteUrls = liked.mapTo(hashSetOf()) { it.url }
        val eligible = (liked + playlists).distinctBy { it.url }
            .filterNot { it.url in recentUrls }
        val next = MixDiversityPolicy.select(
            eligible, dayNumber, limit = MAX_QUICK_PICKS
        ) { track -> if (track.url in favoriteUrls) 12 else 6 }

        // Existing Daily Mixes and personal mixes keep their own real tracks,
        // keys, reasons and independent tap-to-play behavior. Suppress cards
        // with identical or nearly identical playlists; don't edit the mix.
        val chosen = ArrayList<PersonalMix>(MAX_MIX_CARDS)
        val usedKeys = HashSet<String>()
        val signatures = ArrayList<Set<String>>(MAX_MIX_CARDS)
        for (mix in dailyMixes + personalMixes) {
            if (chosen.size >= MAX_MIX_CARDS) break
            val ids = mix.tracks.asSequence()
                .map { it.url }
                .filter { VideoLink.normalize(it) == it }
                .toSet()
            if (ids.isEmpty() || mix.key.isBlank() || !usedKeys.add(mix.key)) continue
            // A small Replay mix can be wholly contained in a larger Daily Mix.
            // Both remain accessible through their source library/history, but
            // rendering near-clones together makes Home feel repetitive.
            val duplicated = signatures.any { other ->
                val overlap = ids.count { it in other }
                overlap * 5 >= minOf(ids.size, other.size) * 4
            }
            if (duplicated) continue
            signatures.add(ids)
            chosen.add(mix)
        }
        return Plan(next, chosen)
    }

    fun sourceLabel(track: VideoPreview, favorites: List<VideoPreview>): String =
        if (favorites.any { it.url == track.url }) "Liked song" else "From your playlists"
}
