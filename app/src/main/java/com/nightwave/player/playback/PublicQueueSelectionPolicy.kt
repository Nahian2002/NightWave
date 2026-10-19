package com.nightwave.player.playback

/**
 * Service-side identity resolution must use the same pre-deduplication
 * selection semantics as PlaybackQueue.replace().
 *
 * A selection index is in the original displayed list, not the shortened
 * queue after duplicate URLs have been removed.
 */
object PublicQueueSelectionPolicy {
    fun selectedIndex(original: List<QueueEntry>, deduplicated: List<QueueEntry>, requested: Int): Int {
        if (deduplicated.isEmpty()) return -1
        val wantedUrl = original.getOrNull(requested)?.url
        val matching = deduplicated.indexOfFirst { it.url == wantedUrl }
        return if (matching >= 0) matching else requested.coerceIn(0, deduplicated.lastIndex)
    }
}
