package com.nightwave.player.v2.ui

/**
 * NW84: compact music-first Home presentation. Layout only: no user history,
 * autoplay, network access, service commands or saved preferences.
 */
object NightWaveHomeLayoutPolicy {
    fun mixColumns(windowWidthDp: Float, fontScale: Float): Int {
        val width = windowWidthDp.takeIf { it.isFinite() } ?: 0f
        val scale = fontScale.takeIf { it.isFinite() } ?: 1f
        // Two 146dp cards become illegible on small phones and with large text.
        return if (width < 390f || scale >= 1.3f) 1 else 2
    }

    fun mixCardCompact(columnCount: Int): Boolean = columnCount == 1
}
