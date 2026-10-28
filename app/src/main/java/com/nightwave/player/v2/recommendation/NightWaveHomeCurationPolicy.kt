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
    const val MAX_HOME_RECENT = 100
    private const val MAX_RECENT_EXCLUSION = 150

    data class Plan(
        val nextListens: List<VideoPreview>,
        val mixes: List<PersonalMix>
    )

    /**
     * Home is a song shelf, not a record of repeated playback events.
     * Keep the newest (first) instance of each actual history URL to avoid
     * duplicate Compose LazyRow keys without rewriting the saved history.
     * Maintain original metadata and chronological order.
     */
    fun distinctRecent(recent: List<VideoPreview>): List<VideoPreview> =
        recent.distinctBy { it.url }

    /**
     * Bound only the in-memory Home shelf / tap-to-play queue, never the saved
     * listening history. Read until N distinct identities have been seen,
     * rather than N raw history entries, so repeated plays do not crowd out
     * valid older tracks. A single additional identity proves "N+" for copy.
     *
     * Returning the original VideoPreview objects preserves real metadata.
     * No IO, metadata rewriting, source changes or history mutations.
     */
    data class RecentWindow(val tracks: List<VideoPreview>, val hasMore: Boolean)

    fun recentWindow(
        recent: List<VideoPreview>, maxVisible: Int = MAX_HOME_RECENT
    ): RecentWindow {
        val limit = maxVisible.coerceIn(0, MAX_RECENT_EXCLUSION)
        if (recent.isEmpty()) return RecentWindow(emptyList(), false)
        if (limit == 0) return RecentWindow(emptyList(), true)
        val seen = HashSet<String>(limit + 1)
        val tracks = ArrayList<VideoPreview>(minOf(limit, recent.size))
        for (track in recent) {
            if (!seen.add(track.url)) continue
            if (tracks.size == limit) return RecentWindow(tracks, true)
            tracks.add(track)
        }
        return RecentWindow(tracks, false)
    }

    fun curate(
        favorites: List<VideoPreview>,
        recent: List<VideoPreview>,
        playlistTracks: List<VideoPreview>,
        dailyMixes: List<PersonalMix>,
        personalMixes: List<PersonalMix>,
        dayNumber: Long
    ): Plan {
        // Exclude 150 DISTINCT recent song identities, not 150 play events.
        // A repeatedly played song must never consume the whole budget.
        val recentUrls = recentWindow(recent, MAX_RECENT_EXCLUSION)
            .tracks.mapTo(hashSetOf()) { it.url }
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
        val nextUrls = next.mapTo(hashSetOf()) { it.url }
        for (mix in dailyMixes + personalMixes) {
            if (chosen.size >= MAX_MIX_CARDS) break
            val ids = mix.tracks.asSequence()
                .map { it.url }
                .filter { VideoLink.normalize(it) == it }
                .toSet()
            // Do not reserve a key for a card that might be rejected as a
            // duplicate. A later genuinely different card with the same key
            // must remain eligible for display.
            if (ids.isEmpty() || mix.key.isBlank() || mix.key in usedKeys) continue
            // A tiny mix repeating the song shelf adds no useful destination.
            // Compare against the candidate's size: a larger mix with new songs
            // must not disappear merely because it contains the six quick picks.
            if (nextUrls.isNotEmpty() && ids.count { it in nextUrls } * 5 >= ids.size * 4) continue
            // A small Replay mix can be wholly contained in a larger Daily Mix.
            // Both remain accessible through their source library/history, but
            // rendering near-clones together makes Home feel repetitive.
            val duplicated = signatures.any { other ->
                val overlap = ids.count { it in other }
                overlap * 5 >= minOf(ids.size, other.size) * 4
            }
            if (duplicated) continue
            usedKeys.add(mix.key)
            signatures.add(ids)
            chosen.add(mix)
        }
        return Plan(next, chosen)
    }

    fun sourceLabel(track: VideoPreview, favorites: List<VideoPreview>): String =
        if (favorites.any { it.url == track.url }) "Liked song" else "From your playlists"
}
