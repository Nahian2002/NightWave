package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.Palette

/** Presentation only. No settings migration, data mutation or generated listening claims. */
object NightWaveMattePolicy {
    const val CARD_RADIUS_DP = 20
    const val ART_RADIUS_DP = 16
    const val GRID_GAP_DP = 8
    const val CARD_PADDING_DP = 16
    const val BORDER_ALPHA = .08f
    const val PRESS_MS = 100
    const val BLUR_DP = 60
    const val BACKDROP_BITMAP_PX = 96

    /** Receives measured content width, after page insets. */
    fun bentoColumns(contentWidthDp: Float, fontScale: Float): Int =
        if (contentWidthDp.isFinite() && contentWidthDp >= 320f &&
            fontScale.isFinite() && fontScale >= .7f && fontScale < 1.3f) 2 else 1

    fun pressScale(pressed: Boolean, reduceMotion: Boolean): Float =
        if (pressed && !reduceMotion) .95f else 1f

    fun pressDuration(reduceMotion: Boolean): Int = if (reduceMotion) 0 else PRESS_MS

    fun showSeekThumb(pressed: Boolean, focused: Boolean, touchExploration: Boolean): Boolean =
        pressed || focused || touchExploration

    fun supportsBlur(sdk: Int): Boolean = sdk >= 31
    fun backdropAlpha(dark: Boolean): Float = if (dark) .15f else .08f
    fun transportFill(palette: Palette): Long = palette.accent
    fun transportInk(palette: Palette): Long = palette.contrastOnAccent()
    // Readable even over the brightest/darkest permitted artwork background.
    fun playerSecondary(palette: Palette): Long = if (palette.isDark) 0xFFB9BAC0 else palette.secondary
}
