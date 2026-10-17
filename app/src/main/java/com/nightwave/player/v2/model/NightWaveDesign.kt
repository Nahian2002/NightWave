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
    const val NAV_HEIGHT_DP = 60
    const val MINI_PLAYER_HEIGHT_DP = 60

    fun palette(theme: NightWaveTheme): Palette = when (theme) {
        NightWaveTheme.MIDNIGHT -> Palette(
            0xFF090D14, 0xFF111B28, 0xFF1B2939, 0xFFF4F8FD, 0xFFA7B7CA,
            0xFF8BE4D6, 0xFF1C4B4B, 0xFF29394B, 0xFFFFB0B9, true
        )
        NightWaveTheme.AURORA -> Palette(
            0xFF11101D, 0xFF1B1A2E, 0xFF292742, 0xFFF8F6FF, 0xFFBEB6D5,
            0xFFC6B6FF, 0xFF4A3E6C, 0xFF363251, 0xFFFFB1B5, true
        )
        NightWaveTheme.PEARL -> Palette(
            0xFFF9F7F2, 0xFFF0EAE2, 0xFFE4D9CB, 0xFF29262B, 0xFF6C646A,
            0xFFB48883, 0xFFE8D4CF, 0xFFCEC4C0, 0xFFB53D51, false
        )
        NightWaveTheme.AMOLED -> Palette(
            0xFF000000, 0xFF121518, 0xFF1B2228, 0xFFFFFFFF, 0xFFA8B6C0,
            0xFF9CE9DF, 0xFF1A3939, 0xFF313D43, 0xFFFFA9B5, true
        )
    }
}
