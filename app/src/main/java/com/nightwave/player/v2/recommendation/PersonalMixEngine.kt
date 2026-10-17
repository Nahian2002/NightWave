package com.nightwave.player.v2.recommendation

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.insights.RecordedTrack

/** Recommendations only from real device-local saved public video metadata. */
data class PersonalMix(
    val key: String,
    val title: String,
    val description: String,
    val reason: String,
    val tracks: List<VideoPreview>
)

object PersonalMixEngine {
    /** A bounded, deterministic day-aware ranking; no servers, AI claims or invented URLs. */
    fun make(
        favorites: List<VideoPreview>,
        recent: List<VideoPreview>,
        playlistTracks: List<VideoPreview>,
        dayNumber: Long,
        memoryTracks: List<RecordedTrack> = emptyList(),
        memoryEnabled: Boolean = false
    ): List<PersonalMix> {
        val likes = favorites.take(250).filter { VideoLink.normalize(it.url) == it.url }
        val plays = recent.take(100).filter { VideoLink.normalize(it.url) == it.url }
        val lists = playlistTracks.take(350).filter { VideoLink.normalize(it.url) == it.url }
        val pool = (likes + plays + lists).distinctBy { it.url }.take(500)
        if (pool.isEmpty()) return emptyList()

        val liked = likes.map { it.url }.toSet()
        val latest = plays.map { it.url }.toSet()
        val curated = lists.map { it.url }.toSet()
        // Do not consider this signal unless the user still explicitly opts in.
        // The ranking can only reorder/add REAL catalog candidates from pool.
        val learned = if (memoryEnabled) memoryTracks.take(100)
            .filter { VideoLink.normalize(it.id) == it.id }
            .associateBy { it.id } else emptyMap()
        val favArtists = likes.map { it.author.trim().lowercase() }
            .filter { it.isNotBlank() && it != "unknown" }
            .groupingBy { it }.eachCount()
        fun affinity(track: VideoPreview): Int =
            favArtists[track.author.trim().lowercase()]?.coerceAtMost(4) ?: 0
        fun memoryAffinity(track: VideoPreview): Int {
            val signal = learned[track.url] ?: return 0
            val sessions = signal.qualifiedPlays.coerceIn(0, 5) * 2
            val time = (signal.listenedMs.coerceAtLeast(0L) / 180_000L)
                .coerceAtMost(5L).toInt()
            return sessions + time
        }
        fun rank(track: VideoPreview): Int =
            (if (track.url in liked) 6 else 0) +
            (if (track.url in latest) 4 else 0) +
            (if (track.url in curated) 2 else 0) +
            affinity(track) + memoryAffinity(track)
        // Keep strong saved/liked signals while rotating equally ranked tracks
        // and preventing a single creator from monopolizing a daily mix.
        fun ordered(source: List<VideoPreview>) =
            MixDiversityPolicy.select(source, dayNumber, 12, ::rank)
        val outputs = mutableListOf<PersonalMix>()
        val signatures = mutableSetOf<String>()
        fun add(key: String, title: String, description: String, reason: String,
                songs: List<VideoPreview>) {
            val tracks = songs.take(12)
            if (tracks.isEmpty()) return
            val signature = tracks.map { it.url }.sorted().joinToString("|")
            if (!signatures.add(signature)) return
            outputs += PersonalMix(key, title, description, reason, tracks)
        }

        add("daily", "Your daily rotation", "Your saved soundtrack, reshuffled each day",
            if (learned.isEmpty()) "Based on liked songs, recent plays and saved playlists"
            else "Based on saved tracks and your opted-in playback memory",
            ordered(pool))
        if (learned.isNotEmpty()) {
            val repeatCandidates = pool.filter { song ->
                val stored = learned[song.url]
                stored != null && (stored.qualifiedPlays > 0 || stored.listenedMs >= 60_000L)
            }.let { candidates ->
                MixDiversityPolicy.select(candidates, dayNumber, 12, ::memoryAffinity)
            }
            if (repeatCandidates.isNotEmpty()) {
                add("memory", "Your listening rotation",
                    "Music you actually spent time listening to",
                    "30s+ observed sessions and playback time from saved songs",
                    repeatCandidates)
            }
        }
        if (plays.isNotEmpty()) {
            // Preserve actual listening order instead of claiming random songs are recent.
            add("replay", "Back in rotation", "Continue with songs you played recently",
                "Only tracks from your local listening history",
                plays.distinctBy { it.url }.take(12))
        }
        val rediscover = (likes + lists).distinctBy { it.url }
            .filter { it.url !in latest }
        if (rediscover.isNotEmpty()) {
            add("rediscover", "Rediscover your library", "Saved music you haven't played recently",
                "Liked or playlist tracks outside your recent listening list",
                ordered(rediscover))
        }
        val familiarArtists = pool.filter { affinity(it) > 0 }
        if (familiarArtists.size >= 3 && outputs.size < 4) {
            add("artists", "From your favorite creators", "More from creators in your favorites",
                "Artists matched by creator name within your saved tracks",
                ordered(familiarArtists))
        }
        return outputs.take(4)
    }
}
