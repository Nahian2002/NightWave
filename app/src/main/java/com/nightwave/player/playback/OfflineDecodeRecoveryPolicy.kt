package com.nightwave.player.playback

/**
 * NW60 — bounded recovery after a local Media3 decoder error.
 *
 * These are app-private indexed song IDs, never URLs or filesystem paths.
 * An error does not delete the file, rewrite the index, retry a network
 * extractor or silently insert recommendations into the user's queue.
 */
internal object OfflineDecodeRecoveryPolicy {
    private const val PREFIX = "offline:"
    const val MAX_QUEUE_SIZE = 400

    fun belongsToSelection(mediaId: String?, selectedId: String?,
                           queueIds: List<String>, alreadyFailed: Set<String>): Boolean =
        !selectedId.isNullOrBlank() &&
            mediaId == PREFIX + selectedId &&
            queueIds.size in 1..MAX_QUEUE_SIZE &&
            queueIds.contains(selectedId) &&
            selectedId !in alreadyFailed

    /** Find a later, *different*, unfailed track without changing the queue. */
    fun nextLinearIndex(ids: List<String>, currentIndex: Int,
                        alreadyFailed: Set<String>, repeatAll: Boolean): Int? {
        if (ids.size !in 2..MAX_QUEUE_SIZE || currentIndex !in ids.indices)
            return null
        for (offset in 1 until ids.size) {
            val candidate = currentIndex + offset
            if (!repeatAll && candidate >= ids.size) return null
            val at = candidate % ids.size
            if (ids[at] !in alreadyFailed) return at
        }
        return null
    }

    /** Media3 owns shuffle order: accept only its actual next queue slot. */
    fun safeShuffledIndex(ids: List<String>, currentIndex: Int,
                          media3NextIndex: Int, alreadyFailed: Set<String>): Int? =
        media3NextIndex.takeIf { it in ids.indices &&
            it != currentIndex && ids.size in 2..MAX_QUEUE_SIZE &&
            ids[it] !in alreadyFailed }
}
