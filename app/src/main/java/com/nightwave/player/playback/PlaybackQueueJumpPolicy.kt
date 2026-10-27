package com.nightwave.player.playback

/**
 * NW115: when the listener explicitly taps the exact song already loaded,
 * restart locally instead of discarding a healthy Media3 timeline and
 * launching a new public-stream lookup.
 *
 * Different selections, unresolved/idle players and decoder failures must
 * still follow the normal queue-authoritative resolution path.
 * This policy never authorizes playback of a cached signed network URL.
 */
internal object PlaybackQueueJumpPolicy {
    fun canRestartLoaded(
        previousSelectedUrl: String?,
        requestedUrl: String?,
        loadedMediaId: String?,
        hasPlayerError: Boolean,
        playerIdle: Boolean
    ): Boolean =
        !hasPlayerError && !playerIdle &&
            !previousSelectedUrl.isNullOrBlank() &&
            previousSelectedUrl == requestedUrl &&
            requestedUrl == loadedMediaId
}
