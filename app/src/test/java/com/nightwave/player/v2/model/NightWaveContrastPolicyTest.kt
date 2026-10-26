package com.nightwave.player.v2.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWaveContrastPolicyTest {
    @Test fun allTextColorsAreReadableOnTheirConfiguredSurfaces() {
        for (theme in NightWaveTheme.entries) {
            val p = NightWaveDesign.palette(theme)
            for (surface in listOf(p.surface, p.raised)) {
                assertTrue("$theme foreground on surface", NightWaveContrastPolicy.normalTextPasses(
                    p.foreground, surface))
                assertTrue("$theme secondary on surface", NightWaveContrastPolicy.normalTextPasses(
                    p.secondary, surface))
                assertTrue("$theme accent on surface", NightWaveContrastPolicy.normalTextPasses(
                    p.accent, surface))
            }
            assertTrue("$theme action foreground", NightWaveContrastPolicy.normalTextPasses(
                p.contrastOnAccent(), p.accent))
        }
    }

    @Test fun blackOnWhiteIsMaximumAndSameColorFails() {
        val black = 0xFF000000
        val white = 0xFFFFFFFF
        assertTrue(NightWaveContrastPolicy.ratio(black, white) >= 21.0 - 0.00001)
        assertFalse(NightWaveContrastPolicy.normalTextPasses(black, black))
    }

    @Test fun pearlHasDarkAccentAndReadableActionLabels() {
        val p = NightWaveDesign.palette(NightWaveTheme.PEARL)
        assertTrue(NightWaveContrastPolicy.ratio(p.accent, p.raised) >= 4.5)
        assertTrue(NightWaveContrastPolicy.ratio(p.secondary, p.raised) >= 4.5)
        assertTrue(NightWaveContrastPolicy.ratio(p.contrastOnAccent(), p.accent) >= 4.5)
    }

    @Test fun nw83MaterialContainerRolesMeetNormalTextContrastInAllPalettes() {
        for (theme in NightWaveTheme.entries) {
            val p = NightWaveDesign.palette(theme)
            val ratio = NightWaveContrastPolicy::ratio
            assertTrue("$theme onPrimary/tertiaryContainer",
                ratio(p.foreground, p.accentSoft) >= 4.5)
            assertTrue("$theme onSecondaryContainer",
                ratio(p.foreground, p.raised) >= 4.5)
            assertTrue("$theme onSecondary",
                ratio(p.background, p.secondary) >= 4.5)
            assertTrue("$theme onError",
                ratio(p.background, p.danger) >= 4.5)
            // Error container uses surface, not raised: Pearl would
            // otherwise fall below the 4.5:1 small-text threshold.
            assertTrue("$theme onErrorContainer",
                ratio(p.danger, p.surface) >= 4.5)
        }
    }
}
