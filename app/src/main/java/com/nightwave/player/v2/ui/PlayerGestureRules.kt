package com.nightwave.player.v2.ui

/** Gesture policy is pure Kotlin so accidental short swipes never change a song. */
internal enum class PlayerArtworkSwipe { NONE, PREVIOUS, NEXT }
internal enum class PlayerVerticalSwipe { NONE, EXPAND, MINIMIZE }

internal object PlayerGestureRules {
    const val MIN_SWIPE_DP = 88f
    const val MIN_VERTICAL_SWIPE_DP = 72f

    /**
     * Use a caller-provided dp-scaled threshold, not raw pixels.
     * On high-density phones a constant 70px would become an accidental flick.
     */
    fun decideVerticalSwipe(distancePx: Float, thresholdPx: Float): PlayerVerticalSwipe {
        if (!distancePx.isFinite() || !thresholdPx.isFinite() ||
            thresholdPx <= 0f || kotlin.math.abs(distancePx) < thresholdPx
        ) return PlayerVerticalSwipe.NONE
        return if (distancePx < 0f) PlayerVerticalSwipe.EXPAND
            else PlayerVerticalSwipe.MINIMIZE
    }


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
