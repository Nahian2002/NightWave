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
/** User motion preference shared by all Compose surfaces, including modal sheets. */
val LocalNightWaveReduceMotion = staticCompositionLocalOf { false }

private val premiumTypography = Typography(
    // One complete hierarchy, including the formerly default Material3
    // styles. Otherwise labels, dialogs and cards silently fall back to
    // mismatched Material typography and look like different applications.
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 44.sp,
        lineHeight = 51.sp, letterSpacing = (-1.4f).sp, fontWeight = FontWeight.ExtraBold),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 38.sp,
        lineHeight = 44.sp, letterSpacing = (-1.0f).sp, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 33.sp,
        lineHeight = 39.sp, letterSpacing = (-.8f).sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 31.sp,
        lineHeight = 38.sp, letterSpacing = (-.7f).sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 26.sp,
        lineHeight = 33.sp, letterSpacing = (-.5f).sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp,
        lineHeight = 29.sp, letterSpacing = (-.3f).sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp,
        lineHeight = 27.sp, letterSpacing = (-.4f).sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp,
        lineHeight = 23.sp, letterSpacing = (-.1f).sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp,
        lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp,
        lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp,
        lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp,
        lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp,
        lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp,
        lineHeight = 17.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp,
        lineHeight = 16.sp, letterSpacing = (.2f).sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun NightWaveMaterialTheme(
    choice: ThemeChoice,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = NightWaveDesign.palette(choice)
    val scheme = if (palette.isDark) darkColorScheme(
        primary = paletteColor(palette.accent), onPrimary = paletteColor(palette.contrastOnAccent()),
        background = paletteColor(palette.background), onBackground = paletteColor(palette.foreground),
        surface = paletteColor(palette.surface), onSurface = paletteColor(palette.foreground),
        surfaceVariant = paletteColor(palette.raised), onSurfaceVariant = paletteColor(palette.secondary),
        secondary = paletteColor(palette.secondary), onSecondary = paletteColor(palette.background),
        tertiary = paletteColor(palette.accent), onTertiary = paletteColor(palette.contrastOnAccent()),
        surfaceTint = paletteColor(palette.accent),
        inverseSurface = paletteColor(palette.foreground),
        inverseOnSurface = paletteColor(palette.background),
        outline = paletteColor(palette.outline), outlineVariant = paletteColor(palette.outline).copy(alpha = .62f),
        error = paletteColor(palette.danger)
    ) else lightColorScheme(
        primary = paletteColor(palette.accent), onPrimary = paletteColor(palette.contrastOnAccent()),
        background = paletteColor(palette.background), onBackground = paletteColor(palette.foreground),
        surface = paletteColor(palette.surface), onSurface = paletteColor(palette.foreground),
        surfaceVariant = paletteColor(palette.raised), onSurfaceVariant = paletteColor(palette.secondary),
        secondary = paletteColor(palette.secondary), onSecondary = paletteColor(palette.background),
        tertiary = paletteColor(palette.accent), onTertiary = paletteColor(palette.contrastOnAccent()),
        surfaceTint = paletteColor(palette.accent),
        inverseSurface = paletteColor(palette.foreground),
        inverseOnSurface = paletteColor(palette.background),
        outline = paletteColor(palette.outline), outlineVariant = paletteColor(palette.outline).copy(alpha = .62f),
        error = paletteColor(palette.danger)
    )
    CompositionLocalProvider(
        LocalNightWavePalette provides palette,
        LocalNightWaveReduceMotion provides reduceMotion
    ) {
        MaterialTheme(colorScheme = scheme, typography = premiumTypography, content = content)
    }
}
