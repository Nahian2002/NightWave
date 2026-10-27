package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** UI dimensions only. Never initializes Media3, accounts or Android storage. */
class NightWaveAdaptiveChromePolicyTest {
    @Test fun navigationAndHeroGrowForLargeText() {
        assertEquals(60, NightWaveAdaptiveChromePolicy.navHeightDp(1f))
        assertEquals(72, NightWaveAdaptiveChromePolicy.navHeightDp(1.3f))
        assertEquals(84, NightWaveAdaptiveChromePolicy.navHeightDp(2f))
        assertEquals(112, NightWaveAdaptiveChromePolicy.navHeightDp(2.4f))
        assertEquals(112, NightWaveAdaptiveChromePolicy.navHeightDp(3f))
        assertEquals(140, NightWaveAdaptiveChromePolicy.homeHeroHeightDp(1f))
        assertEquals(170, NightWaveAdaptiveChromePolicy.homeHeroHeightDp(1.3f))
        assertEquals(198, NightWaveAdaptiveChromePolicy.homeHeroHeightDp(2f))
        assertEquals(52, NightWaveAdaptiveChromePolicy.miniPlayerHeightDp(1f))
        assertEquals(78, NightWaveAdaptiveChromePolicy.miniPlayerHeightDp(1.8f))
    }

    @Test fun splitScreenAndInvalidSizesNeverProduceNegativeArtwork() {
        val dimensions = floatArrayOf(-100f, 0f, 20f, 320f, 390f, 720f,
            Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        for (w in dimensions) for (h in dimensions) {
            val art = NightWaveAdaptiveChromePolicy.artworkSizeDp(w, h, 1f)
            assertTrue(art.isFinite())
            assertTrue(art >= 0f && art <= 360f)
        }
    }

    @Test fun artworkFitsSafeWidthAndDifferentHeightCaps() {
        assertEquals(0f, NightWaveAdaptiveChromePolicy.artworkSizeDp(28f, 800f, 1f), 0.001f)
        assertTrue(NightWaveAdaptiveChromePolicy.artworkSizeDp(320f, 640f, 1f) <= 320f)
        assertTrue(NightWaveAdaptiveChromePolicy.artworkSizeDp(360f, 300f, 1f) <= 102f)
        assertTrue(NightWaveAdaptiveChromePolicy.artworkSizeDp(600f, 1000f, 2f) <= 260f)
        assertEquals(360f, NightWaveAdaptiveChromePolicy.artworkSizeDp(1440f, 1600f, 1f), 0.001f)
    }

    @Test fun paddingsAndTouchTargetsAreStable() {
        assertEquals(14, NightWaveAdaptiveChromePolicy.horizontalPaddingDp(320f))
        assertEquals(20, NightWaveAdaptiveChromePolicy.horizontalPaddingDp(390f))
        assertEquals(28, NightWaveAdaptiveChromePolicy.horizontalPaddingDp(900f))
        assertEquals(72, NightWaveAdaptiveChromePolicy.nativeRowHeightDp(1f))
        assertEquals(82, NightWaveAdaptiveChromePolicy.nativeRowHeightDp(1.3f))
        assertEquals(96, NightWaveAdaptiveChromePolicy.nativeRowHeightDp(2f))
        assertTrue(NightWaveAdaptiveChromePolicy.nativeRowHeightDp(1f) >= 48)
    }

    @Test fun reducedMotionAndCorruptInputsAreSafe() {
        assertEquals(0, NightWaveAdaptiveChromePolicy.motionDurationMs(true, 900))
        assertEquals(160, NightWaveAdaptiveChromePolicy.motionDurationMs(false, 160))
        assertEquals(600, NightWaveAdaptiveChromePolicy.motionDurationMs(false, 900))
        assertEquals(0, NightWaveAdaptiveChromePolicy.motionDurationMs(false, -2))
        assertEquals(60, NightWaveAdaptiveChromePolicy.navHeightDp(Float.NaN))
        assertEquals(60, NightWaveAdaptiveChromePolicy.navHeightDp(Float.POSITIVE_INFINITY))
        assertFalse(NightWaveAdaptiveChromePolicy.artworkSizeDp(
            Float.POSITIVE_INFINITY, Float.NaN, 2f) < 0f)
    }
}
