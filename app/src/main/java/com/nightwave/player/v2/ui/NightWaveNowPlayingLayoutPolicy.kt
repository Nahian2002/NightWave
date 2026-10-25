package com.nightwave.player.v2.ui

/**
 * NW46 - presentation-only sizing for the full-screen player.
 *
 * Uses dp and scale-independent input and deliberately does not read playback
 * state. Compact, split-screen, landscape and enlarged-text windows must still
 * leave controls reachable through the existing scroll container.
 */
object NightWaveNowPlayingLayoutPolicy {
    data class Layout(
        val artworkDp: Float,
        val horizontalPaddingDp: Int,
        val compact: Boolean,
        val artworkCornerDp: Int,
        val headerBottomSpaceDp: Int
    )

    fun resolve(widthDp: Float, heightDp: Float, fontScale: Float): Layout {
        val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val height = heightDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val scale = fontScale.takeIf { it.isFinite() }?.coerceIn(0.7f, 3f) ?: 1f
        val padding = when {
            width < 360f -> 14
            width >= 700f -> 28
            else -> 20
        }
        val compact = height < 680f || width < 350f || scale >= 1.3f
        val heightRatio = when {
            scale >= 1.6f -> 0.24f
            scale >= 1.3f -> 0.30f
            compact -> 0.36f
            else -> 0.42f
        }
        val availableWidth = (width - padding * 2 - 12).coerceAtLeast(0f)
        val artwork = minOf(availableWidth, height * heightRatio, 420f)
            .coerceAtLeast(0f)
        return Layout(
            artworkDp = artwork,
            horizontalPaddingDp = padding,
            compact = compact,
            artworkCornerDp = if (compact) 24 else 30,
            headerBottomSpaceDp = if (compact) 9 else 20
        )
    }
}
