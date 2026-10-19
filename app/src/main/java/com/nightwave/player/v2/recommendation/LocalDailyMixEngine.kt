package com.nightwave.player.v2.recommendation

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.insights.RecordedTrack
import java.util.Locale

/**
 * Daily mixes built ONLY from real device-local saved/recent public track links.
 * Deterministic for a day, without network access, tracking, persisted lists,
 * synthetic genres, fabricated artists or inferred listening sessions.
 *
 * Distinct daily mixes do not repeat a video across mix boundaries.
 */
object LocalDailyMixEngine {
    const val MAX_POOL = 500
    const val MAX_DAILY_MIXES = 3
    const val MAX_PER_MIX = 12
    const val MIN_PER_MIX = 3

    fun make(
        favorites: List<VideoPreview>,
        recent: List<VideoPreview>,
        playlistTracks: List<VideoPreview>,
        dayNumber: Long,
        memoryTracks: List<RecordedTrack> = emptyList(),
        memoryEnabled: Boolean = false
    ): List<PersonalMix> {
        val likes = favorites.take(250)
            .filter { VideoLink.normalize(it.url) == it.url }
        val history = recent.take(100)
            .filter { VideoLink.normalize(it.url) == it.url }
        val saved = playlistTracks.take(350)
            .filter { VideoLink.normalize(it.url) == it.url }
        val pool = (likes + history + saved).distinctBy { it.url }.take(MAX_POOL)
        val count = when {
            pool.size >= 9 -> MAX_DAILY_MIXES
            pool.size >= 6 -> 2
            pool.size >= MIN_PER_MIX -> 1
            else -> 0
        }
        if (count == 0) return emptyList()
        val perMix = minOf(MAX_PER_MIX, pool.size / count)
        val favoriteIds = likes.mapTo(hashSetOf()) { it.url }
        val recentIds = history.mapTo(hashSetOf()) { it.url }
        val savedIds = saved.mapTo(hashSetOf()) { it.url }
        // Only opt-in, stored, actually eligible catalog IDs are useful.
        val optInMemory = if (memoryEnabled) memoryTracks.take(100)
            .filter { it.id in pool.map(VideoPreview::url).toHashSet() }
            .associateBy { it.id }
            else emptyMap()
        val creatorCounts = likes.groupingBy {
            MusicIdentity.display(it.title, it.author)
                .artistOrCreator.trim().lowercase(Locale.ROOT)
        }.eachCount()
        fun memoryAffinity(track: VideoPreview): Int {
            val record = optInMemory[track.url] ?: return 0
            val plays = record.qualifiedPlays.coerceIn(0, 4) * 2
            val minutes = (record.listenedMs.coerceAtLeast(0L) / 180_000L)
                .coerceAtMost(4L).toInt()
            return plays + minutes
        }
        val used = hashSetOf<String>()
        val titles = listOf(
            "Daily Mix 1", "Daily Mix 2", "Daily Mix 3")
        val descriptions = listOf(
            "Favorites and familiar listening picks",
            "Saved tracks with an emphasis on rediscovery",
            "Creator-aware variety from your library")
        val explanations = listOf(
            "Prioritizes favorites, recent songs and opt-in listening history",
            "Prioritizes saved music outside your recent rotation",
            "Balances familiar creator names and track variety")
        return (0 until count).mapNotNull { lane ->
            val available = pool.filterNot { it.url in used }
            if (available.size < MIN_PER_MIX) return@mapNotNull null
            val selected = MixDiversityPolicy.select(
                available, dayNumber + (lane * 7919L),
                limit = perMix
            ) { track ->
                val creator = MusicIdentity.display(track.title, track.author)
                    .artistOrCreator.trim().lowercase(Locale.ROOT)
                val affinity = (creatorCounts[creator] ?: 0).coerceAtMost(4)
                when (lane) {
                    0 -> (if (track.url in favoriteIds) 12 else 0) +
                        (if (track.url in recentIds) 6 else 0) +
                        memoryAffinity(track) + affinity
                    1 -> (if (track.url !in recentIds) 9 else 0) +
                        (if (track.url in savedIds) 5 else 0) +
                        (if (track.url in favoriteIds) 2 else 0)
                    else -> affinity * 2 +
                        (if (track.url in savedIds) 3 else 0) +
                        (if (track.url in favoriteIds) 2 else 0) +
                        memoryAffinity(track) / 2
                }
            }
            if (selected.size < MIN_PER_MIX) return@mapNotNull null
            used.addAll(selected.map { it.url })
            PersonalMix(
                key = "daily_mix_${lane + 1}",
                title = titles[lane],
                description = descriptions[lane],
                reason = if (memoryEnabled && lane != 1 && optInMemory.isNotEmpty())
                    explanations[lane] + " · Playback history enabled"
                else explanations[lane] + " · Based on saved music",
                tracks = selected
            )
        }
    }
}
