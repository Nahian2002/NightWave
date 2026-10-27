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

    /**
     * An IDLE Media3 player can still retain a MediaItem whose temporary
     * network stream URI has expired. Never prepare that stale URI merely
     * because the public song ID happens to match. Re-resolve on explicit Play.
     * Existing ID-only comparisons remain available for non-resume use.
     */
    fun canResumeLoadedPublic(
        desiredId: String?, loadedId: String?,
        hasError: Boolean, playerIdle: Boolean
    ): Boolean = !playerIdle &&
        desiredId != null &&
        QueueRestorePolicy.isCanonicalPublicWatchUrl(desiredId) &&
        reusesLoadedTrack(desiredId, loadedId, hasError)

    /** Same saved queue after process recovery: preserve its Shuffle/Play Next
     * state instead of replacing it just to look up a fresh media stream. */
    fun shouldResolveSavedSelection(
        sameQueue: Boolean, explicitPlay: Boolean,
        selectedId: String?, reusableLoaded: Boolean,
        pendingMatching: Boolean
    ): Boolean = sameQueue && explicitPlay && !reusableLoaded &&
        !pendingMatching && selectedId != null &&
        QueueRestorePolicy.isCanonicalPublicWatchUrl(selectedId)

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
