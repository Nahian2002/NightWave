package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.*
import org.junit.Assert.*
import org.junit.Test

/** Visual-role contracts from the recorded theme review; no product-score assertions. */
class NightWaveThemeExpressionTest {
    private fun palettes() = NightWaveTheme.entries.map(NightWaveDesign::palette)

    @Test fun sectionCaptionsStayReadableOnBothColoredSurfaces() {
        for (p in palettes()) for (secondary in listOf(false, true)) {
            val surface = NightWaveThemeExpressionPolicy.sectionSurface(p, secondary)
            for (text in listOf(p.foreground, p.secondary))
                assertTrue(NightWaveContrastPolicy.ratio(text, surface) >= 4.5)
            assertTrue(NightWaveContrastPolicy.ratio(
                NightWaveThemeExpressionPolicy.sectionAccent(p, secondary), surface) >= 4.5)
        }
    }
    @Test fun secondaryAccentHasItsOwnReadableContainerAndTextRoles() {
        for (p in palettes()) {
            assertNotEquals(p.accent, p.secondaryAccent)
            assertNotEquals(p.accentSoft, p.secondarySoft)
            for (surface in listOf(p.background, p.surface, p.raised))
                assertTrue(NightWaveContrastPolicy.ratio(p.secondaryAccent, surface) >= 4.5)
        }
    }
    @Test fun keyControlsUseTheActualSelectedThemeAccentWithStrongInkContrast() {
        for (p in palettes()) {
            assertEquals(p.accent, NightWaveMattePolicy.transportFill(p))
            assertEquals(p.contrastOnAccent(), NightWaveMattePolicy.transportInk(p))
            assertTrue(NightWaveContrastPolicy.ratio(NightWaveMattePolicy.transportFill(p),
                NightWaveMattePolicy.transportInk(p)) >= 7.0)
        }
    }
    @Test fun canvasCardAndRaisedLevelsHaveSeparateTonalRoles() {
        for (p in palettes()) {
            assertEquals(3, setOf(p.background, p.surface, p.raised).size)
            if (p.isDark) {
                assertTrue(NightWaveContrastPolicy.luminance(p.surface) > NightWaveContrastPolicy.luminance(p.background))
                assertTrue(NightWaveContrastPolicy.luminance(p.raised) > NightWaveContrastPolicy.luminance(p.surface))
            } else assertTrue(NightWaveContrastPolicy.luminance(p.surface) > NightWaveContrastPolicy.luminance(p.background))
        }
    }
    @Test fun moodColorIsAnExplicitStableSectionRoleRatherThanAClaimAboutMusic() {
        assertTrue(NightWaveThemeExpressionPolicy.secondaryMood(" AFTER HOURS "))
        assertTrue(NightWaveThemeExpressionPolicy.secondaryMood("Unplugged"))
        assertFalse(NightWaveThemeExpressionPolicy.secondaryMood("Deep focus"))
        assertFalse(NightWaveThemeExpressionPolicy.secondaryMood("Unknown collection"))
    }
    @Test fun anUnpairedFullWidthCollectionUsesTheBoundedFeatureTreatment() {
        assertTrue(NightWaveThemeExpressionPolicy.featuredMixCell(1))
        for (count in listOf(-1, 0, 2, 3, Int.MAX_VALUE))
            assertFalse(NightWaveThemeExpressionPolicy.featuredMixCell(count))
    }
    @Test fun existingPaletteConstructorsRetainSafeSecondaryDefaults() {
        val original = NightWaveDesign.palette(NightWaveTheme.MIDNIGHT)
        val legacy = Palette(original.background, original.surface, original.raised, original.foreground,
            original.secondary, original.accent, original.accentSoft, original.outline, original.danger, original.isDark)
        assertEquals(legacy.accent, legacy.secondaryAccent)
        assertEquals(legacy.accentSoft, legacy.secondarySoft)
        assertEquals(setOf("MIDNIGHT", "AURORA", "PEARL", "AMOLED"), NightWaveTheme.entries.map { it.name }.toSet())
    }
    @Test fun allFourThemesHaveDistinctDeliberatelyColoredPrimaryAccents() {
        val palettes = palettes()
        assertEquals(4, palettes.map { it.accent }.toSet().size)
        assertEquals(0xFF000000, NightWaveDesign.palette(NightWaveTheme.AMOLED).background)
        for (p in palettes) {
            val channels = listOf(16, 8, 0).map { ((p.accent ushr it) and 255).toInt() }
            assertTrue("Primary accent must carry visible color", channels.max() - channels.min() >= 64)
        }
    }
}
