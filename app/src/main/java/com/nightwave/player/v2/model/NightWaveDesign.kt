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
    fun contrastOnAccent(): Long = if (isDark) 0xFF100D20 else 0xFF17111A
}

object NightWaveDesign {
    val spacing = intArrayOf(4, 8, 12, 16, 20, 24, 32, 40)
    const val TOUCH_TARGET_DP = 48
    const val PAGE_HORIZONTAL_GUTTER_DP = 20
    const val ALBUM_ART_ASPECT = 1f
    const val VIDEO_ART_ASPECT = 16f / 9f
    const val CARD_CORNER_DP = 20
    const val HERO_CORNER_DP = 28
    const val NAV_HEIGHT_DP = 68
    const val MINI_PLAYER_HEIGHT_DP = 76

    fun palette(theme: NightWaveTheme): Palette = when (theme) {
        NightWaveTheme.MIDNIGHT -> Palette(
            0xFF090B13, 0xFF151929, 0xFF21253A, 0xFFF7F6FF, 0xFFABB0C7,
            0xFFB6A4FF, 0xFF3C345E, 0xFF343A51, 0xFFFF9FAF, true
        )
        NightWaveTheme.AURORA -> Palette(
            0xFF091215, 0xFF132327, 0xFF1B353A, 0xFFE8FFF8, 0xFFA3C7C2,
            0xFF72E3D0, 0xFF205652, 0xFF335B5B, 0xFFFFB1B5, true
        )
        NightWaveTheme.PEARL -> Palette(
            0xFFF9F7F2, 0xFFF0EAE2, 0xFFE4D9CB, 0xFF29262B, 0xFF6C646A,
            0xFFB48883, 0xFFE8D4CF, 0xFFCEC4C0, 0xFFB53D51, false
        )
        NightWaveTheme.AMOLED -> Palette(
            0xFF000000, 0xFF111113, 0xFF1A1A20, 0xFFFFFFFF, 0xFFB4B4BC,
            0xFFDFD9FF, 0xFF282530, 0xFF393943, 0xFFFFA9B5, true
        )
    }
}
