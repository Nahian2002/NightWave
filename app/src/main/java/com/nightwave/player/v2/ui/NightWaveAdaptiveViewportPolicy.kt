package com.nightwave.player.v2.ui

/** Measured viewport only. Does not alter system text size, navigation or stored state. */
internal object NightWaveAdaptiveViewportPolicy {
    private fun height(value: Float) = if (value.isFinite()) value.coerceAtLeast(0f) else 0f
    fun headerMaxHeightDp(availableHeightDp: Float): Float =
        minOf(height(availableHeightDp) * .25f, 160f)
    fun footerMaxHeightDp(availableHeightDp: Float): Float =
        minOf(height(availableHeightDp) * .35f, 220f)
    fun stackDetail(widthDp: Float, fontScale: Float): Boolean =
        !widthDp.isFinite() || widthDp < 400f ||
            (fontScale.isFinite() && fontScale >= 1.3f)
    const val DIALOG_MAX_WIDTH_DP = 600
}
