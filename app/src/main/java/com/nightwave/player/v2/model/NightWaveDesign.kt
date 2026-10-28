package com.nightwave.player.v2.model

/** Pixel-neutral design contract. Compose converts ARGB values at the UI boundary. */
data class Palette(
    val background: Long,
    val surface: Long,
    val raised: Long,
    val foreground: Long,
    val secondary: Long,
    val accent: Long,
    val accentSoft: Long,
    val outline: Long,
    val danger: Long,
    val isDark: Boolean
) {
    // All current light palettes use a dark accent to keep small labels
    // readable on cards. Light lettering is required on that dark accent.
    fun contrastOnAccent(): Long = if (isDark) 0xFF100D20 else 0xFFF9F7F2
}

object NightWaveDesign {
    val spacing = intArrayOf(4, 8, 12, 16, 20, 24, 32, 40)
    const val TOUCH_TARGET_DP = 48
    const val PAGE_HORIZONTAL_GUTTER_DP = 20
    const val ALBUM_ART_ASPECT = 1f
    const val VIDEO_ART_ASPECT = 16f / 9f
    const val CARD_CORNER_DP = 20
    const val HERO_CORNER_DP = 28
    const val NAV_HEIGHT_DP = 60
    const val MINI_PLAYER_HEIGHT_DP = 60

    fun palette(theme: NightWaveTheme): Palette = when (theme) {
        NightWaveTheme.MIDNIGHT -> Palette(
            0xFF0A0A0A, 0xFF121212, 0xFF1A1A1A, 0xFFFFFFFF, 0xFF8E8E93,
            0xFFA1D6C9, 0xFF1E3330, 0xFF737378, 0xFFFFB4AB, true
        )
        NightWaveTheme.AURORA -> Palette(
            0xFF100F16, 0xFF17151F, 0xFF22202B, 0xFFFAF8FF, 0xFFB3ADBF,
            0xFFCEC0EC, 0xFF332B44, 0xFF81778E, 0xFFFFB4AB, true
        )
        NightWaveTheme.PEARL -> Palette(
            0xFFF9F7F2, 0xFFF0EDE7, 0xFFE5E0D8, 0xFF24221F, 0xFF5F5A55,
            0xFF61527F, 0xFFE3DDEB, 0xFF777067, 0xFFAA3042, false
        )
        NightWaveTheme.AMOLED -> Palette(
            0xFF000000, 0xFF0E0E0E, 0xFF181818, 0xFFFFFFFF, 0xFFA0A0A5,
            0xFFB1CEC5, 0xFF1E2C28, 0xFF737378, 0xFFFFB4AB, true
        )
    }
}
