package com.nightwave.player.offline

/**
 * A streamed audio file is staged first and then copied into app-private
 * Offline Library storage. Require enough headroom for both copies when the
 * server declares size. Unknown sizes are still bounded during transfer.
 */
object OfflineSaveStoragePolicy {
    const val MIN_FREE_BYTES = 24L * 1024L * 1024L
    private const val RESERVED_BYTES = 16L * 1024L * 1024L

    fun canStart(availableBytes: Long, declaredAudioBytes: Long?): Boolean {
        if (availableBytes < MIN_FREE_BYTES) return false
        val expected = declaredAudioBytes ?: return true
        if (expected !in 1..OfflineDownloadLimits.MAX_AUDIO_BYTES) return false
        return availableBytes >= expected * 2L + RESERVED_BYTES
    }
}
