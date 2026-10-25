package com.nightwave.player.v2.library

import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import java.util.Locale

/** Read-only dynamic collections. Never persist snapshots or invent play counts. */
data class SmartLibraryCollection(
    val id: String, val title: String, val description: String,
    val songs: List<VideoPreview>
)

object SmartLibraryPolicy {
    const val PREFIX = "nightwave://smart/"
    const val MAX_SOURCE_TRACKS = 600
    const val MAX_COLLECTIONS = 8

    fun generate(
        favorites: List<VideoPreview>,
        recent: List<VideoPreview>,
        playlists: List<List<VideoPreview>>
    ): List<SmartLibraryCollection> {
        // Each source has a fair budget: even a very large Favorites/Recent
        // list cannot starve playlist-only tracks. Reject malformed public
        // references without rewriting any stored playlist or playback URL.
        val eligible: (VideoPreview) -> Boolean = { song ->
            VideoLink.normalize(song.url) == song.url
        }
        val pool = SmartDiscoveryPoolPolicy.gather(
            favorites, recent, playlists, VideoPreview::url, eligible
        )
        if (pool.isEmpty()) return emptyList()
        val result = mutableListOf<SmartLibraryCollection>()
        val recentSafe = recent.asSequence().filter(eligible)
            .distinctBy(VideoPreview::url).take(100).toList()
        if (recentSafe.isNotEmpty()) result.add(SmartLibraryCollection(
            PREFIX + "recent", "Recent rotation",
            "Recently played tracks from your local history", recentSafe))
        val likedUrls = favorites.asSequence().filter(eligible)
            .mapTo(hashSetOf()) { it.url }
        val recentUrls = recentSafe.mapTo(hashSetOf()) { it.url }
        val rediscover = SmartDiscoveryPoolPolicy.rediscover(
            pool, likedUrls, recentUrls, VideoPreview::url
        )
        if (rediscover.isNotEmpty()) result.add(SmartLibraryCollection(
            PREFIX + "liked", "Rediscover your likes",
            "Liked tracks outside your recent listening rotation", rediscover))
        val longTracks = pool.filter { it.durationSeconds >= 240L }.take(80)
        if (longTracks.size >= 2) result.add(SmartLibraryCollection(
            PREFIX + "long", "Long listens",
            "Tracks at least four minutes long", longTracks))
        val creators = pool.groupBy { song ->
            val name = MusicIdentity.display(song.title, song.author)
                .artistOrCreator.trim()
            name.lowercase(Locale.ROOT).takeIf {
                it.isNotBlank() && it !in setOf(
                    "unknown creator", "unknown artist", "various artists", "youtube creator")
            }
        }
        val orderedCreators = creators.entries.filter { it.key != null &&
            it.value.distinctBy(VideoPreview::url).size >= 2 }
            .sortedWith(compareByDescending<Map.Entry<String?, List<VideoPreview>>> {
                it.value.size
            }.thenBy { it.key ?: "" })
        for ((key, songs) in orderedCreators.take((MAX_COLLECTIONS - result.size).coerceAtLeast(0))) {
            val label = MusicIdentity.display(songs.first().title, songs.first().author)
                .artistOrCreator.trim().take(48)
            result.add(SmartLibraryCollection(PREFIX + "creator/" + key.orEmpty(),
                "Creator mix · $label",
                "Tracks grouped by saved creator metadata", songs.take(100)))
        }
        return result.take(MAX_COLLECTIONS)
    }

    fun belongs(id: String): Boolean = id.startsWith(PREFIX)
}
