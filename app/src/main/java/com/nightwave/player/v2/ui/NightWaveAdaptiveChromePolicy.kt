package com.nightwave.player.v2.ui

/**
 * Pure responsive layout policy. All values are in dp except the font scale.
 * Clamp malformed/unbounded measurement inputs before feeding Compose sizes.
 *
 * No playback, navigation, permissions or persistence decisions belong here.
 */
object NightWaveAdaptiveChromePolicy {
    private fun safeWidth(widthDp: Float): Float =
        if (widthDp.isFinite()) widthDp.coerceAtLeast(0f) else 0f

    private fun safeHeight(heightDp: Float): Float =
        if (heightDp.isFinite()) heightDp.coerceAtLeast(0f) else 0f

    private fun safeFontScale(scale: Float): Float =
        if (scale.isFinite()) scale.coerceIn(0.7f, 3f) else 1f

    fun navHeightDp(fontScale: Float): Int = when {
        // At 240–300% system text, 84dp nav clips labels below its icons.
        safeFontScale(fontScale) >= 2.4f -> 112
        safeFontScale(fontScale) >= 1.6f -> 84
        safeFontScale(fontScale) >= 1.3f -> 72
        else -> NightWaveChromePolicy.NAV_HEIGHT_DP
    }

    fun miniPlayerHeightDp(fontScale: Float): Int = when {
        safeFontScale(fontScale) >= 1.6f -> 78
        safeFontScale(fontScale) >= 1.3f -> 64
        else -> 52
    }

    fun homeHeroHeightDp(fontScale: Float): Int = when {
        safeFontScale(fontScale) >= 1.6f -> 198
        safeFontScale(fontScale) >= 1.3f -> 170
        else -> 140
    }

    fun horizontalPaddingDp(widthDp: Float): Int = when {
        safeWidth(widthDp) < 360f -> 14
        safeWidth(widthDp) >= 700f -> 28
        else -> 20
    }

    /** Artwork always fits both bounds; tiny windows never produce a negative dp. */
    fun artworkSizeDp(widthDp: Float, heightDp: Float, fontScale: Float): Float {
        val w = safeWidth(widthDp)
        val h = safeHeight(heightDp)
        val inset = horizontalPaddingDp(w) * 2 + 8
        val remainingWidth = (w - inset).coerceAtLeast(0f)
        val heightFraction = if (safeFontScale(fontScale) >= 1.6f) 0.26f
            else if (safeFontScale(fontScale) >= 1.3f) 0.30f else 0.34f
        return minOf(remainingWidth, h * heightFraction, 360f).coerceAtLeast(0f)
    }

    /** Reduced motion also covers animators used in card/UI transitions. */
    fun motionDurationMs(reducedMotion: Boolean, regularMs: Int): Int =
        if (reducedMotion) 0 else regularMs.coerceIn(0, 600)

    fun nativeRowHeightDp(fontScale: Float): Int =
        if (safeFontScale(fontScale) >= 1.6f) 96 else
            if (safeFontScale(fontScale) >= 1.3f) 82 else 72
}
