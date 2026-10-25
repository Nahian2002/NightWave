package com.nightwave.player.v2.ui

/**
 * Central motion contract: restrained, consistent and genuinely optional.
 * Values are free of Compose dependencies so phone/CI smoke tests can run.
 */
object NightWaveMotionPolicy {
    const val NAV_MS = 220
    const val SURFACE_MS = 240
    const val CONTENT_MS = 280
    const val FEEDBACK_MS = 160
    const val MAX_MS = 450
    const val COVER_MS = 190
    const val PLAYER_ENTER_MS = 260
    const val PLAYER_EXIT_MS = 180
    const val TAB_HAPTIC_MAX_JUMP = 1

    fun duration(reduceMotion: Boolean, requested: Int): Int =
        if (reduceMotion) 0 else requested.coerceIn(0, MAX_MS)

    fun tabScale(active: Boolean, reduceMotion: Boolean): Float =
        if (reduceMotion) 1f else if (active) 1f else 0.93f

    fun artworkScale(playing: Boolean, reduceMotion: Boolean): Float =
        if (reduceMotion) 1f else if (playing) 1f else .985f

    fun transitionEnabled(reduceMotion: Boolean) = !reduceMotion

    /**
     * Distant tabs should jump directly rather than animating through every
     * expensive page. Nearby tabs can use short, predictable motion.
     */
    fun animateTabChange(
        fromIndex: Int, toIndex: Int, pageCount: Int,
        reduceMotion: Boolean
    ): Boolean = !reduceMotion &&
        pageCount > 0 && fromIndex in 0 until pageCount &&
        toIndex in 0 until pageCount &&
        kotlin.math.abs(fromIndex.toLong() - toIndex.toLong()) in
            1L..TAB_HAPTIC_MAX_JUMP.toLong()

    /** Haptics are strictly optional, only for intentional user navigation. */
    fun tabHaptic(
        fromIndex: Int, toIndex: Int, pageCount: Int,
        reduceMotion: Boolean
    ): Boolean = !reduceMotion &&
        pageCount > 0 && fromIndex in 0 until pageCount &&
        toIndex in 0 until pageCount && fromIndex != toIndex

    /** Motion offsets never take the full screen across on player open. */
    fun playerEntryOffsetPx(availableHeightPx: Int): Int =
        availableHeightPx.coerceAtLeast(0).toLong().div(6L)
            .coerceAtMost(240L).toInt()

    fun playerExitOffsetPx(availableHeightPx: Int): Int =
        availableHeightPx.coerceAtLeast(0).toLong().div(7L)
            .coerceAtMost(200L).toInt()

}
