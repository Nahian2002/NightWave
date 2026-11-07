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
    val isDark: Boolean,
    /** Decorative/secondary actions only; never a new stored theme or metadata identity. */
    val secondaryAccent: Long = accent,
    val secondarySoft: Long = accentSoft
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
            0xFF090F1E, 0xFF13233C, 0xFF1B3254, 0xFFF4F8FF, 0xFFBCCAE0,
            0xFF82BCFF, 0xFF183C68, 0xFF879FBE, 0xFFFFB4B9, true,
            0xFFB8ADFF, 0xFF342D55
        )
        NightWaveTheme.AURORA -> Palette(
            0xFF1B0F29, 0xFF2E1A43, 0xFF43275A, 0xFFFAF5FF, 0xFFD3C0E5,
            0xFFD1ACFF, 0xFF543378, 0xFFAD94C2, 0xFFFFB9C9, true,
            0xFFFFB8D0, 0xFF542C48
        )
        NightWaveTheme.PEARL -> Palette(
            0xFFF8F2E7, 0xFFFFFFFF, 0xFFE9E8FC, 0xFF14223C, 0xFF4E6080,
            0xFF1C47AD, 0xFFDDE7FF, 0xFF758199, 0xFFAA3042, false,
            0xFF7C3BA9, 0xFFF0E1F8
        )
        NightWaveTheme.AMOLED -> Palette(
            0xFF000000, 0xFF091A15, 0xFF102D24, 0xFFF1FFF9, 0xFFB0D2C5,
            0xFF66F0BE, 0xFF143D30, 0xFF75998C, 0xFFFFB4AB, true,
            0xFF9ABEFF, 0xFF203551
        )
    }
}
