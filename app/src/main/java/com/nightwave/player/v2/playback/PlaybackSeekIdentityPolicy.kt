package com.nightwave.player.v2.playback

/** Keep stale seek gestures from being applied to a newly selected Media3 item. */
object PlaybackSeekIdentityPolicy {
    fun accepts(expected: String, shown: String, mediaId: String?): Boolean =
        expected.isNotBlank() && expected == shown && expected == mediaId

    /**
     * NW80: one identity and duration decision at the actual seek command.
     * A gesture for another song, a failed player or an unknown duration must
     * never seek whichever MediaItem happened to load a moment later.
     */
    fun target(
        expected: String?, shown: String?, loadedId: String?,
        durationMs: Long, requestedMs: Long, playerHasError: Boolean = false
    ): Long? {
        if (playerHasError || expected.isNullOrBlank() ||
            expected != shown || expected != loadedId || durationMs <= 0L) return null
        return requestedMs.coerceIn(0L, durationMs)
    }
}
