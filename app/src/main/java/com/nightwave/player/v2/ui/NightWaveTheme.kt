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

private fun color(raw: Long) = Color(raw.toULong())
val LocalNightWavePalette = staticCompositionLocalOf { NightWaveDesign.palette(ThemeChoice.MIDNIGHT) }

private val premiumTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 42.sp, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun NightWaveMaterialTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val palette = NightWaveDesign.palette(choice)
    val scheme = if (palette.isDark) darkColorScheme(
        primary = color(palette.accent), onPrimary = color(palette.contrastOnAccent()),
        background = color(palette.background), onBackground = color(palette.foreground),
        surface = color(palette.surface), onSurface = color(palette.foreground),
        surfaceVariant = color(palette.raised), onSurfaceVariant = color(palette.secondary),
        outline = color(palette.outline), error = color(palette.danger)
    ) else lightColorScheme(
        primary = color(palette.accent), onPrimary = color(palette.contrastOnAccent()),
        background = color(palette.background), onBackground = color(palette.foreground),
        surface = color(palette.surface), onSurface = color(palette.foreground),
        surfaceVariant = color(palette.raised), onSurfaceVariant = color(palette.secondary),
        outline = color(palette.outline), error = color(palette.danger)
    )
    CompositionLocalProvider(LocalNightWavePalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = premiumTypography, content = content)
    }
}
