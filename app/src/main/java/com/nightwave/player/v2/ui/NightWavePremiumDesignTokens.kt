package com.nightwave.player.v2.ui

/**
 * NW83: one deterministic premium visual contract used across navigation,
 * discovery, library, queue and player surfaces.
 *
 * These are layout-only values: no network calls, playback, saved library
 * mutations or dynamic font downloads. All dimensions are in dp.
 */
object NightWavePremiumDesignTokens {
    const val MAX_CONTENT_WIDTH_DP = 740
    const val MIN_TOUCH_TARGET_DP = 48
    const val NAV_CORNER_DP = 24
    const val CARD_CORNER_DP = 18
    const val SEARCH_CARD_CORNER_DP = 16
    const val HERO_CORNER_DP = 24
    const val ARTWORK_CORNER_DP = 12
    const val ICON_SIZE_DP = 22
    const val ROW_ARTWORK_DP = 52
    const val ROW_GAP_DP = 12
    const val ROW_HORIZONTAL_INSET_DP = 12
    const val ROW_VERTICAL_INSET_DP = 9
    const val SECTION_SPACING_DP = 16

    /** No negative/NaN measurement can become a Compose dimension. */
    fun pageGutterDp(widthDp: Float): Int =
        NightWaveAdaptiveChromePolicy.horizontalPaddingDp(widthDp)

    /**
     * Keep song rows readable at accessibility font sizes: never fix row
     * heights that could clip a wrapped title or multiline artist.
     */
    fun rowMinHeightDp(fontScale: Float): Int {
        val scale = if (fontScale.isFinite()) fontScale else 1f
        return when {
            scale >= 1.8f -> 108
            scale >= 1.3f -> 88
            else -> 72
        }
    }

    fun titleLines(fontScale: Float, widthDp: Float): Int {
        val scale = if (fontScale.isFinite()) fontScale else 1f
        val width = if (widthDp.isFinite()) widthDp else 0f
        return if (scale >= 1.3f || width >= 430f) 2 else 1
    }

    fun controlTouchTargetDp(fontScale: Float): Int {
        val scale = if (fontScale.isFinite()) fontScale else 1f
        return if (scale >= 1.8f) 56 else MIN_TOUCH_TARGET_DP
    }

    /** Restrained outline; selection is also shown with readable text. */
    fun rowBorderAlpha(selected: Boolean): Float = if (selected) .50f else .19f

    fun selectedFillAlpha(selected: Boolean): Float = if (selected) .13f else .05f
}
