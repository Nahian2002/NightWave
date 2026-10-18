package com.nightwave.player.v2.library

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import kotlin.random.Random

/** One bounded, read-only user-initiated public playlist playback request. */
data class LibraryQuickPlayPlan(
    val songs: List<VideoPreview>,
    val totalPlayable: Int
) {
    val first: VideoPreview? get() = songs.firstOrNull()
    val truncated: Boolean get() = totalPlayable > songs.size
}

/**
 * Does not own ExoPlayer or touch legacy playlist storage.
 * It only selects up to the Media3 service's existing 100-song queue capacity.
 * Shuffle is a one-time randomized play order, not a persistent shuffle toggle.
 */
object LibraryQuickPlayPolicy {
    const val MAX_QUEUE = 100

    fun plan(source: List<VideoPreview>, shuffle: Boolean, seed: Long): LibraryQuickPlayPlan {
        val safe = source.filter { VideoLink.normalize(it.url) == it.url }
            .distinctBy { it.url }
        val ordered = if (shuffle) safe.shuffled(Random(seed)) else safe
        return LibraryQuickPlayPlan(ordered.take(MAX_QUEUE), safe.size)
    }
}
