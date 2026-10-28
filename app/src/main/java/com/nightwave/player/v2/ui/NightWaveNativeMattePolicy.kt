package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.Palette

/** Explicit legacy presentation roles. Status text/actions and all native behavior remain intact. */
object NightWaveNativeMattePolicy {
    private val accents = setOf(0xFF91E2D7, 0xFF94E2D8, 0xFF98E5DB, 0xFF9FE3DF,
        0xFF9CE9DF, 0xFF98E0D5, 0xFFB1EBE3, 0xFFE0F7F3)
    private val alerts = setOf(0xFFFFC07D, 0xFFE2C99A, 0xFFF1BDBD, 0xFFF0B7B7)
    private val darkInk = setOf(0xFF09131B, 0xFF091B20, 0xFF0C1B20, 0xFF101A21)
    fun text(original: Long, palette: Palette): Long = when (original and 0xFFFFFFFF) {
        0xFFFFFFFF -> palette.foreground
        in accents -> palette.accent
        in alerts -> palette.danger
        in darkInk -> palette.contrastOnAccent()
        else -> palette.secondary
    }
    fun surface(original: Long, palette: Palette): Long = when (original and 0xFFFFFFFF) {
        0xFF0C131B, 0xFF090D14 -> palette.background
        in accents -> palette.accent
        else -> palette.surface
    }
}
