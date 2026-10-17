package com.nightwave.player.v2.recommendation

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

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
        dayNumber: Long
    ): List<PersonalMix> {
        val likes = favorites.take(250).filter { VideoLink.normalize(it.url) == it.url }
        val plays = recent.take(100).filter { VideoLink.normalize(it.url) == it.url }
        val lists = playlistTracks.take(350).filter { VideoLink.normalize(it.url) == it.url }
        val pool = (likes + plays + lists).distinctBy { it.url }.take(500)
        if (pool.isEmpty()) return emptyList()

        val liked = likes.map { it.url }.toSet()
        val latest = plays.map { it.url }.toSet()
        val curated = lists.map { it.url }.toSet()
        val favArtists = likes.map { it.author.trim().lowercase() }
            .filter { it.isNotBlank() && it != "unknown" }
            .groupingBy { it }.eachCount()
        fun affinity(track: VideoPreview): Int =
            favArtists[track.author.trim().lowercase()]?.coerceAtMost(4) ?: 0
        fun rank(track: VideoPreview): Int =
            (if (track.url in liked) 6 else 0) +
            (if (track.url in latest) 4 else 0) +
            (if (track.url in curated) 2 else 0) + affinity(track)
        fun dailyTie(track: VideoPreview): Int =
            track.url.hashCode() xor (dayNumber.toInt() * 31)
        fun ordered(source: List<VideoPreview>) = source.distinctBy { it.url }
            .sortedWith(compareByDescending<VideoPreview> { rank(it) }
                .thenBy { dailyTie(it) })
            .take(12)
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
            "Based on liked songs, recent plays and saved playlists",
            ordered(pool))
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
        if (familiarArtists.size >= 2 && outputs.size < 3) {
            add("artists", "Your familiar voices", "More from artists in your favorites",
                "Artists matched by creator name within your saved tracks",
                ordered(familiarArtists))
        }
        return outputs.take(3)
    }
}
