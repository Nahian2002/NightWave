package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.NightWaveContrastPolicy
import com.nightwave.player.v2.model.NightWaveDesign
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.*
import org.junit.Test

/** Layout policies only; rendered acceptance belongs to the Android matrix. */
class NightWaveCompactHierarchyTest {
    private val tokens = NightWavePremiumDesignTokens

    @Test fun narrowAndLargeTypeGetTheirOwnActionLine() {
        for (width in listOf(320f, 360f, 412f)) {
            for (scale in listOf(2f, 3f)) assertTrue(tokens.stackActions(width, scale))
        }
        assertTrue(tokens.stackActions(320f, 1f))
        assertFalse(tokens.stackActions(360f, 1f))
        assertFalse(tokens.stackActions(412f, 1f))
    }

    @Test fun invalidMeasurementsChooseSafeReflowAndBoundedShelves() {
        for (width in listOf(Float.NaN, Float.NEGATIVE_INFINITY, -1f, 0f)) {
            assertTrue(tokens.stackActions(width, 1f))
            assertTrue(tokens.shelfWidthDp(width, 3f) in 156..260)
        }
        assertEquals(tokens.shelfWidthDp(320f, 1f), tokens.shelfWidthDp(320f, Float.NaN))
    }

    @Test fun shelvesGrowForTextWhileKeepingAnotherItemDiscoverable() {
        for (width in listOf(320f, 360f, 412f)) {
            val ordinary = tokens.shelfWidthDp(width, 1f)
            val enlarged = tokens.shelfWidthDp(width, 3f)
            assertTrue(enlarged > ordinary)
            assertTrue(enlarged <= width - 48f)
            assertTrue(ordinary >= tokens.MIN_TOUCH_TARGET_DP)
        }
    }

    @Test fun continueMinimumGrowsMonotonicallyAndLeavesMoreMusicSpace() {
        val minimums = listOf(1f, 1.3f, 2f, 3f).map {
            NightWaveAdaptiveChromePolicy.homeHeroHeightDp(it)
        }
        assertEquals(minimums.sorted(), minimums)
        assertTrue(minimums.first() < 140) // protected NW126 default minimum
        assertTrue(minimums.all { it >= tokens.MIN_TOUCH_TARGET_DP })
    }

    @Test fun opaqueCaptionsKeepRequiredTextContrastInEveryTheme() {
        for (theme in NightWaveTheme.entries) {
            val p = NightWaveDesign.palette(theme)
            // No image alpha is involved: exact foreground/surface pairing.
            for (foreground in listOf(p.foreground, p.secondary, p.accent)) {
                assertTrue("$theme caption", NightWaveContrastPolicy.ratio(foreground, p.surface) >= 4.5)
            }
            assertTrue("$theme selection", NightWaveContrastPolicy.ratio(p.accent, p.surface) >= 3.0)
        }
    }

    @Test fun compactInsetsDoNotShrinkTextOrTouchTargets() {
        assertTrue(tokens.PAGE_TOP_DP < 16)
        assertTrue(tokens.PAGE_BOTTOM_DP < 34)
        assertTrue(tokens.HEADER_MIN_HEIGHT_DP >= tokens.MIN_TOUCH_TARGET_DP)
        for (scale in listOf(1f, 2f, 3f)) assertTrue(tokens.controlTouchTargetDp(scale) >= 48)
        assertTrue(tokens.SECTION_TITLE_SP >= 20)
        assertTrue(tokens.SECTION_LINE_SP >= tokens.SECTION_TITLE_SP)
    }
}
