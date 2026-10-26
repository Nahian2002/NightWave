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

    /**
     * Reassert Play only for an explicit same-selection request during an
     * already pending extraction. Queue-only actions must not auto-resume.
     */
    fun shouldResumePendingExplicitPlay(
        explicitPlay: Boolean,
        sameSelection: Boolean,
        loaded: Boolean,
        pendingMatching: Boolean,
        alreadyWantsPlayback: Boolean
    ): Boolean = explicitPlay && sameSelection && !loaded &&
        pendingMatching && !alreadyWantsPlayback

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
