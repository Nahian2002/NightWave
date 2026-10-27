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
        val headerBottomSpaceDp: Int,
        /** Keep Play/Previous/Next reachable when all five icons cannot fit. */
        val splitTransportControls: Boolean,
        /** Give offline/lyrics action labels their own row at large font sizes. */
        val stackQuickActions: Boolean,
        /** Even the three primary 48/64/48dp controls cannot share one row. */
        val stackPrimaryTransportControls: Boolean,
        /** Two 48dp mode buttons cannot share one row in extreme multiwindow. */
        val stackSecondaryTransportControls: Boolean
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
        val availableContentWidth = (width - padding * 2).coerceAtLeast(0f)
        val availableWidth = (availableContentWidth - 12).coerceAtLeast(0f)
        // Standard transport requires 4 x 48dp targets + 64/72dp Play,
        // plus breathing room. Below 284dp, split out secondary modes.
        // Font magnification gets the same layout even on a wide display.
        val splitControls = availableContentWidth < 284f || scale >= 1.8f
        val stackActions = availableContentWidth < 320f || scale >= 1.6f
        // 48dp Previous + 64dp Play + 48dp Next require 160dp.
        // Add 16dp breathing room or stack them in truly tiny windows.
        // Shuffle + Repeat each need a full 48dp touch surface.
        val stackPrimary = availableContentWidth < 176f
        val stackSecondary = availableContentWidth < 104f
        val artwork = minOf(availableWidth, height * heightRatio, 420f)
            .coerceAtLeast(0f)
        return Layout(
            artworkDp = artwork,
            horizontalPaddingDp = padding,
            compact = compact,
            artworkCornerDp = if (compact) 24 else 30,
            headerBottomSpaceDp = if (compact) 9 else 20,
            splitTransportControls = splitControls,
            stackQuickActions = stackActions,
            stackPrimaryTransportControls = stackPrimary,
            stackSecondaryTransportControls = stackSecondary
        )
    }
}
