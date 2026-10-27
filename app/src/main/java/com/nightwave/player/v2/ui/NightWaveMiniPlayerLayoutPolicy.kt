package com.nightwave.player.v2.ui

/**
 * NW116: presentation-only mini-player sizing. Protect the readable track
 * title and the two essential touch actions (Play/Pause and Hide) on compact
 * windows and at high Android font scales; never change playback or queue.
 */
object NightWaveMiniPlayerLayoutPolicy {
    data class Layout(
        val rowHeightDp: Int,
        val artworkDp: Int,
        val showArtwork: Boolean,
        val showNext: Boolean,
        val titleLines: Int,
        val labelGapDp: Int
    )

    fun resolve(widthDp: Float, fontScale: Float, queueCount: Int): Layout {
        val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val scale = fontScale.takeIf { it.isFinite() }?.coerceIn(.7f, 3f) ?: 1f
        val rowHeight = when {
            scale >= 2.2f -> 124
            scale >= 1.8f -> 104
            else -> NightWaveAdaptiveChromePolicy.miniPlayerHeightDp(scale)
        }
        // Hide the decorative thumbnail in very narrow split-screen windows.
        // The full player and both primary transport actions remain reachable.
        val showArtwork = width >= 280f && !(scale >= 2.2f && width < 360f)
        val artwork = when {
            width < 320f || scale >= 2.2f -> 36
            scale >= 1.3f -> 44
            else -> 40
        }
        // At extreme magnification, let the title use space normally occupied
        // by mini Next; expanded Now Playing still provides its Next control.
        val showNext = NightWaveChromePolicy.showMiniNext(queueCount, width) &&
            !(scale >= 1.8f && width < 460f)
        return Layout(
            rowHeightDp = rowHeight,
            artworkDp = artwork,
            showArtwork = showArtwork,
            showNext = showNext,
            titleLines = if (scale >= 1.3f) 2 else 1,
            labelGapDp = if (width < 340f || scale >= 2.2f) 5 else 9
        )
    }
}
