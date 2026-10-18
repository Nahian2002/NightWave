package com.nightwave.player.v2.playback

/**
 * Foreground-only controller recovery budget.
 * The application never replays an old song merely because a controller reconnects.
 */
internal object MediaReconnectPolicy {
    fun mayAttempt(
        released: Boolean,
        foregroundActive: Boolean,
        oneShotAvailable: Boolean,
        controllerAttached: Boolean,
        connectionPending: Boolean
    ): Boolean =
        !released && foregroundActive && oneShotAvailable &&
            !controllerAttached && !connectionPending
}
