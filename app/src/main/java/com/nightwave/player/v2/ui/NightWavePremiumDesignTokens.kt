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
    const val NAV_CORNER_DP = 20
    const val CARD_CORNER_DP = 20
    const val SEARCH_CARD_CORNER_DP = 20
    const val HERO_CORNER_DP = 20
    const val ARTWORK_CORNER_DP = 16
    const val ICON_SIZE_DP = 22
    const val ROW_ARTWORK_DP = 52
    const val ROW_GAP_DP = 12
    const val ROW_HORIZONTAL_INSET_DP = 12
    const val ROW_VERTICAL_INSET_DP = 9
    const val SECTION_SPACING_DP = 16
    // NW127: density comes from shared insets and removed duplication, never
    // smaller type or touch bounds. Minimums permit intrinsic text growth.
    const val PAGE_TOP_DP = 8
    const val PAGE_BOTTOM_DP = 24
    const val ITEM_SPACING_DP = 8
    const val CARD_PADDING_DP = 16
    const val CAPTION_GAP_DP = 4
    const val SECTION_TITLE_SP = 20
    const val SECTION_LINE_SP = 27
    const val HEADER_MIN_HEIGHT_DP = 52

    /** Available window, not physical screen width; safe for split-screen. */
    fun stackActions(widthDp: Float, fontScale: Float): Boolean =
        !widthDp.isFinite() || widthDp < 360f ||
            (fontScale.isFinite() && fontScale >= 1.3f)

    fun shelfWidthDp(widthDp: Float, fontScale: Float): Int {
        val width = if (widthDp.isFinite()) widthDp.coerceAtLeast(0f) else 320f
        val scale = if (fontScale.isFinite()) fontScale else 1f
        return if (scale >= 1.3f) (width - 48f).toInt().coerceIn(156, 260)
            else 144
    }

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

    /** Support two artist/metadata lines at large system font sizes. */
    fun supportingLines(fontScale: Float): Int =
        if (fontScale.isFinite() && fontScale >= 1.8f) 2 else 1

    fun controlTouchTargetDp(fontScale: Float): Int {
        val scale = if (fontScale.isFinite()) fontScale else 1f
        return if (scale >= 1.8f) 56 else MIN_TOUCH_TARGET_DP
    }

    /** Restrained outline; selection is also shown with readable text. */
    fun rowBorderAlpha(selected: Boolean): Float = if (selected) 1f else NightWaveMattePolicy.BORDER_ALPHA

    fun selectedFillAlpha(selected: Boolean): Float = if (selected) .13f else .05f
}
