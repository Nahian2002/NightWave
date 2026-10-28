package com.nightwave.player.v2.ui

internal object NightWaveArtworkFramePolicy {
    /** Only landscape images shorten the frame. Portrait/square art retains safe square containment. */
    fun heightDp(widthDp: Float, sourceWidth: Int, sourceHeight: Int, offline: Boolean): Float {
        val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val ratio = if (sourceWidth > 0 && sourceHeight > 0)
            (sourceWidth.toFloat() / sourceHeight).coerceIn(1f, 1.9f)
        else if (offline) 1f else 4f / 3f
        return width / ratio
    }
}
