package com.nightwave.player.playback

/**
 * Side-effect-free fences for the service's one optional transient network
 * refresh. A pause, timer expiry, queue replacement, offline switch or newer
 * lookup must prevent any delayed callback from restarting an old song.
 *
 * These decisions do not start playback or persist signed stream URLs.
 */
internal object PlaybackRecoveryPolicy {
    fun mayAutoRetry(
        expectedUrl: String?,
        selectedUrl: String?,
        loadedMediaId: String?,
        offline: Boolean,
        timerPaused: Boolean,
        wantsPlayback: Boolean,
        alreadyRetried: Boolean,
        retriableError: Boolean,
        resolvingUrl: String?
    ): Boolean =
        !offline && !timerPaused && wantsPlayback && !alreadyRetried &&
            retriableError && expectedUrl != null &&
            QueueRestorePolicy.isCanonicalPublicWatchUrl(expectedUrl) &&
            expectedUrl == selectedUrl && expectedUrl == loadedMediaId &&
            resolvingUrl == null

    fun acceptsPostedRetry(
        expectedUrl: String,
        scheduledGeneration: Int,
        currentGeneration: Int,
        selectedUrl: String?,
        loadedMediaId: String?,
        offline: Boolean,
        timerPaused: Boolean,
        wantsPlayback: Boolean,
        retryMarkerUrl: String?,
        resolvingUrl: String?
    ): Boolean =
        scheduledGeneration == currentGeneration &&
            !offline && !timerPaused && wantsPlayback &&
            QueueRestorePolicy.isCanonicalPublicWatchUrl(expectedUrl) &&
            expectedUrl == selectedUrl && expectedUrl == loadedMediaId &&
            expectedUrl == retryMarkerUrl && resolvingUrl == null

    /** A resolver must not substitute another public video for the selected one. */
    fun acceptsResolvedTrack(expectedUrl: String, receivedUrl: String): Boolean =
        QueueRestorePolicy.isCanonicalPublicWatchUrl(expectedUrl) &&
            expectedUrl == receivedUrl
}
