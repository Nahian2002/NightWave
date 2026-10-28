package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
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
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = NightWavePremiumDesignTokens.SECTION_TITLE_SP.sp,
        lineHeight = NightWavePremiumDesignTokens.SECTION_LINE_SP.sp, letterSpacing = (-.4f).sp, fontWeight = FontWeight.Bold),
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
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp,
        lineHeight = 17.sp, letterSpacing = (.15f).sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun NightWaveMaterialTheme(
    choice: ThemeChoice,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = NightWaveDesign.palette(choice)
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(context, choice) {
        context.setTheme(when (choice) {
            ThemeChoice.AURORA -> com.nightwave.player.R.style.Theme_NightWave_Aurora
            ThemeChoice.PEARL -> com.nightwave.player.R.style.Theme_NightWave_Pearl
            ThemeChoice.AMOLED -> com.nightwave.player.R.style.Theme_NightWave_AMOLED
            else -> com.nightwave.player.R.style.Theme_NightWave
        })
        NightWaveMatteSystemBars.apply(context, palette)
        onDispose { }
    }
    val baseScheme = if (palette.isDark) darkColorScheme(
        primary = paletteColor(palette.accent), onPrimary = paletteColor(palette.contrastOnAccent()),
        primaryContainer = paletteColor(palette.accentSoft),
        onPrimaryContainer = paletteColor(palette.foreground),
        background = paletteColor(palette.background), onBackground = paletteColor(palette.foreground),
        surface = paletteColor(palette.surface), onSurface = paletteColor(palette.foreground),
        surfaceVariant = paletteColor(palette.raised), onSurfaceVariant = paletteColor(palette.secondary),
        secondary = paletteColor(palette.secondary), onSecondary = paletteColor(palette.background),
        secondaryContainer = paletteColor(palette.raised),
        onSecondaryContainer = paletteColor(palette.foreground),
        tertiary = paletteColor(palette.secondaryAccent), onTertiary = paletteColor(palette.contrastOnAccent()),
        tertiaryContainer = paletteColor(palette.secondarySoft),
        onTertiaryContainer = paletteColor(palette.foreground),
        surfaceTint = Color.Transparent,
        inverseSurface = paletteColor(palette.foreground),
        inverseOnSurface = paletteColor(palette.background),
        outline = paletteColor(palette.outline), outlineVariant = paletteColor(palette.outline).copy(alpha = .62f),
        error = paletteColor(palette.danger), onError = paletteColor(palette.background),
        // Keep required error text on an opaque, contrast-tested surface in every theme.
        errorContainer = paletteColor(palette.surface),
        onErrorContainer = paletteColor(palette.danger)
    ) else lightColorScheme(
        primary = paletteColor(palette.accent), onPrimary = paletteColor(palette.contrastOnAccent()),
        primaryContainer = paletteColor(palette.accentSoft),
        onPrimaryContainer = paletteColor(palette.foreground),
        background = paletteColor(palette.background), onBackground = paletteColor(palette.foreground),
        surface = paletteColor(palette.surface), onSurface = paletteColor(palette.foreground),
        surfaceVariant = paletteColor(palette.raised), onSurfaceVariant = paletteColor(palette.secondary),
        secondary = paletteColor(palette.secondary), onSecondary = paletteColor(palette.background),
        secondaryContainer = paletteColor(palette.raised),
        onSecondaryContainer = paletteColor(palette.foreground),
        tertiary = paletteColor(palette.secondaryAccent), onTertiary = paletteColor(palette.contrastOnAccent()),
        tertiaryContainer = paletteColor(palette.secondarySoft),
        onTertiaryContainer = paletteColor(palette.foreground),
        surfaceTint = Color.Transparent,
        inverseSurface = paletteColor(palette.foreground),
        inverseOnSurface = paletteColor(palette.background),
        outline = paletteColor(palette.outline), outlineVariant = paletteColor(palette.outline).copy(alpha = .62f),
        error = paletteColor(palette.danger), onError = paletteColor(palette.background),
        // Keep required error text on an opaque, contrast-tested surface in every theme.
        errorContainer = paletteColor(palette.surface),
        onErrorContainer = paletteColor(palette.danger)
    )
    // Material dialogs/menus use container roles, not the legacy surfaceVariant role.
    // Supplying all seven keeps them inside the selected palette instead of default purple/gray.
    val scheme = baseScheme.copy(
        surfaceDim = paletteColor(if (palette.isDark) palette.background else palette.raised),
        surfaceBright = paletteColor(if (palette.isDark) palette.raised else palette.surface),
        surfaceContainerLowest = paletteColor(if (palette.isDark) palette.background else palette.surface),
        surfaceContainerLow = paletteColor(palette.surface),
        surfaceContainer = paletteColor(palette.surface),
        surfaceContainerHigh = paletteColor(palette.raised),
        surfaceContainerHighest = paletteColor(palette.raised)
    )
    CompositionLocalProvider(
        LocalNightWavePalette provides palette,
        LocalNightWaveReduceMotion provides reduceMotion
    ) {
        MaterialTheme(colorScheme = scheme, typography = premiumTypography,
            shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(24.dp)), content = content)
    }
}
