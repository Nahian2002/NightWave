package com.nightwave.player.playback

/**
 * Media3 session command authorization and meaningful queue ownership.
 * Controller identity is derived in PlaybackService from Android's
 * authenticated ControllerInfo and the actual notification controller.
 *
 * Does not expose files, signed media URLs or privileged app operations.
 */
internal object MediaSessionQueueControlPolicy {
    fun trustedController(sameAppPackage: Boolean, mediaNotification: Boolean): Boolean =
        sameAppPackage || mediaNotification

    fun knownAction(action: String?, next: String, previous: String): Boolean =
        action != null && (action == next || action == previous) &&
            next.isNotBlank() && previous.isNotBlank() && next != previous

    fun canNavigate(
        offlinePlaying: Boolean,
        offlineSelected: Boolean,
        publicSelected: Boolean
    ): Boolean = if (offlinePlaying) offlineSelected else publicSelected
}
