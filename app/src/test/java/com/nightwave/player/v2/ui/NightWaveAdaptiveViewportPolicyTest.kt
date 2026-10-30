package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class NightWaveAdaptiveViewportPolicyTest {
    @Test fun dialogBudgetsLeaveBodySpaceAcrossRealisticShortAndTallWindows() {
        for (height in listOf(240f, 320f, 412f, 600f, 840f, 1280f)) {
            val header = NightWaveAdaptiveViewportPolicy.headerMaxHeightDp(height)
            val footer = NightWaveAdaptiveViewportPolicy.footerMaxHeightDp(height)
            assertTrue(header >= 48f && footer >= 48f)
            assertTrue(height - header - footer >= height * .39f)
            assertTrue(header <= 160f && footer <= 220f)
        }
    }
    @Test fun malformedMeasurementsNeverCreateInfiniteOrNegativeCaps() {
        for (height in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f, 0f, Float.MAX_VALUE)) {
            val header = NightWaveAdaptiveViewportPolicy.headerMaxHeightDp(height)
            val footer = NightWaveAdaptiveViewportPolicy.footerMaxHeightDp(height)
            assertTrue(header.isFinite() && header in 0f..160f)
            assertTrue(footer.isFinite() && footer in 0f..220f)
        }
    }
    @Test fun detailsUseAStackOnNarrowOrMagnifiedWindows() {
        for (width in listOf(240f, 320f, 360f)) assertTrue(NightWaveAdaptiveViewportPolicy.stackDetail(width, 1f))
        assertFalse(NightWaveAdaptiveViewportPolicy.stackDetail(412f, 1f))
        for (scale in listOf(1.3f, 2f, 3f)) assertTrue(NightWaveAdaptiveViewportPolicy.stackDetail(840f, scale))
    }
    @Test fun invalidWidthChoosesSafeReflowWithoutShrinkingSystemText() {
        assertTrue(NightWaveAdaptiveViewportPolicy.stackDetail(Float.NaN, 1f))
        assertTrue(NightWaveAdaptiveViewportPolicy.stackDetail(Float.POSITIVE_INFINITY, 1f))
        assertTrue(NightWaveAdaptiveViewportPolicy.stackDetail(-1f, 3f))
        assertFalse(NightWaveAdaptiveViewportPolicy.stackDetail(600f, Float.NaN))
    }
}
