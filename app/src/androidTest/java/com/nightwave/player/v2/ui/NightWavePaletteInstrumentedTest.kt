package com.nightwave.player.v2.ui

import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.v2.model.NightWaveDesign
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NightWavePaletteInstrumentedTest {
    @Test fun allThemeColorsConvertFromArgbWithoutPackedColorSpaceErrors() {
        for (choice in NightWaveTheme.entries) {
            val palette = NightWaveDesign.palette(choice)
            val colors = listOf(palette.background, palette.surface, palette.raised,
                palette.foreground, palette.secondary, palette.accent,
                palette.accentSoft, palette.outline, palette.danger,
                palette.contrastOnAccent())
            for (argb in colors) {
                // Color(raw.toULong()) instead of Color(raw.toInt()) used to
                // corrupt Compose's packed color-space ID at app startup.
                assertEquals(argb.toInt(), paletteColor(argb).toArgb())
            }
        }
    }
}
