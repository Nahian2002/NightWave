package com.nightwave.player.playback

/**
 * Pure, media-independent guardrails used by PlaybackService.
 * A signed stream is never persisted or reused across process restarts.
 */
internal object PlaybackResolutionPolicy {
    /** Extractor-only deadline; stream buffering/read timeouts are separate. */
    const val RESOLUTION_TIMEOUT_MS = 35_000L

    fun reusesLoadedTrack(
        desiredId: String?, loadedId: String?, hasError: Boolean
    ): Boolean = !hasError && desiredId != null && desiredId == loadedId

    fun awaitsSameResolution(
        desiredId: String?, pendingId: String?
    ): Boolean = desiredId != null && desiredId == pendingId

    fun acceptsResult(
        requestGeneration: Int, currentGeneration: Int,
        expectedUrl: String, selectedUrl: String?,
        offlineMode: Boolean
    ): Boolean = requestGeneration == currentGeneration &&
        !offlineMode && expectedUrl == selectedUrl

    fun timedOut(startElapsedMs: Long, nowElapsedMs: Long): Boolean =
        nowElapsedMs >= startElapsedMs &&
            nowElapsedMs - startElapsedMs >= RESOLUTION_TIMEOUT_MS
}
