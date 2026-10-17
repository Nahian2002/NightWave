package com.nightwave.player.v2.insights

import com.nightwave.player.net.VideoPreview

/**
 * Accurate insights from NightWave's existing bounded, deduplicated recent
 * tracks: these are NOT full play counts, minutes listened, or all-time trends.
 */
data class RecentCreator(val name: String, val distinctRecentTracks: Int)
data class ListeningInsights(
    val recentEntries: Int,
    val uniqueRecentTracks: Int,
    val distinctRecentCreators: Int,
    val likedTracks: Int,
    val playlists: Int,
    val creators: List<RecentCreator>
)

object ListeningInsightsEngine {
    fun calculate(
        recent: List<VideoPreview>,
        favorites: List<VideoPreview>,
        playlistNames: List<String>
    ): ListeningInsights {
        val seen = recent.take(100).distinctBy { it.url }
        val grouped = seen.groupBy { it.author.trim() }
            .filterKeys { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
        val creators = grouped.entries.sortedWith(
            compareByDescending<Map.Entry<String, List<VideoPreview>>> { it.value.size }
                .thenBy { it.key.lowercase() }
        ).take(3).map { RecentCreator(it.key.take(80), it.value.size) }
        return ListeningInsights(
            recentEntries = recent.size.coerceAtMost(100),
            uniqueRecentTracks = seen.size,
            distinctRecentCreators = grouped.size,
            likedTracks = favorites.distinctBy { it.url }.size,
            playlists = playlistNames.distinct().size,
            creators = creators
        )
    }
}
