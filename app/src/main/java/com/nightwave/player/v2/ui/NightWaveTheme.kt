package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nightwave.player.v2.model.NightWaveDesign
import com.nightwave.player.v2.model.NightWaveTheme as ThemeChoice

// Palette values are 32-bit ARGB (0xAARRGGBB), NOT Compose's packed ULong colors.
// Color(ULong) interprets the low bits as a color-space ID and can crash at startup.
internal fun paletteColor(raw: Long) = Color(raw.toInt())
val LocalNightWavePalette = staticCompositionLocalOf { NightWaveDesign.palette(ThemeChoice.MIDNIGHT) }

private val premiumTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-1.3f).sp, fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 32.sp, lineHeight = 39.sp, letterSpacing = (-.65f).sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-.45f).sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 27.sp, letterSpacing = (-.3f).sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun NightWaveMaterialTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val palette = NightWaveDesign.palette(choice)
    val scheme = if (palette.isDark) darkColorScheme(
        primary = paletteColor(palette.accent), onPrimary = paletteColor(palette.contrastOnAccent()),
        background = paletteColor(palette.background), onBackground = paletteColor(palette.foreground),
        surface = paletteColor(palette.surface), onSurface = paletteColor(palette.foreground),
        surfaceVariant = paletteColor(palette.raised), onSurfaceVariant = paletteColor(palette.secondary),
        outline = paletteColor(palette.outline), error = paletteColor(palette.danger)
    ) else lightColorScheme(
        primary = paletteColor(palette.accent), onPrimary = paletteColor(palette.contrastOnAccent()),
        background = paletteColor(palette.background), onBackground = paletteColor(palette.foreground),
        surface = paletteColor(palette.surface), onSurface = paletteColor(palette.foreground),
        surfaceVariant = paletteColor(palette.raised), onSurfaceVariant = paletteColor(palette.secondary),
        outline = paletteColor(palette.outline), error = paletteColor(palette.danger)
    )
    CompositionLocalProvider(LocalNightWavePalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = premiumTypography, content = content)
    }
}
