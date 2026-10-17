package com.nightwave.player.v2.ui

/** Gesture policy is pure Kotlin so accidental short swipes never change a song. */
internal enum class PlayerArtworkSwipe { NONE, PREVIOUS, NEXT }

internal object PlayerGestureRules {
    const val MIN_SWIPE_DP = 88f

    /**
     * Negative horizontal travel skips forward; positive goes backward.
     * Vertical movement, short drags, disconnected players and empty queues
     * are not interpreted as playback requests.
     */
    fun decideHorizontalSwipe(
        distancePx: Float,
        thresholdPx: Float,
        connected: Boolean,
        buffering: Boolean,
        queueSize: Int
    ): PlayerArtworkSwipe {
        if (!connected || buffering || queueSize < 2 || !distancePx.isFinite() ||
            !thresholdPx.isFinite() || thresholdPx <= 0f ||
            kotlin.math.abs(distancePx) < thresholdPx
        ) return PlayerArtworkSwipe.NONE
        return if (distancePx < 0) PlayerArtworkSwipe.NEXT
            else PlayerArtworkSwipe.PREVIOUS
    }
}
