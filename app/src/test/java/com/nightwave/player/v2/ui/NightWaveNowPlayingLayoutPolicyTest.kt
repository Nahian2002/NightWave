package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW46 checks presentation mathematics without service/network/album-art loading. */
class NightWaveNowPlayingLayoutPolicyTest {
    @Test fun normalPhoneGetsImmersiveHeroWithoutHorizontalClipping() {
        val layout = NightWaveNowPlayingLayoutPolicy.resolve(390f, 844f, 1f)
        assertFalse(layout.compact)
        assertEquals(20, layout.horizontalPaddingDp)
        assertTrue(layout.artworkDp >= 320f)
        assertTrue(layout.artworkDp <= 390f - 2 * layout.horizontalPaddingDp - 12f)
        assertEquals(16, layout.artworkCornerDp)
    }

    @Test fun smallOrSplitScreenRemainsCompactAndReachable() {
        val small = NightWaveNowPlayingLayoutPolicy.resolve(320f, 620f, 1f)
        val split = NightWaveNowPlayingLayoutPolicy.resolve(380f, 300f, 1f)
        assertTrue(small.compact)
        assertTrue(split.compact)
        assertTrue(small.artworkDp <= 280f)
        assertTrue(split.artworkDp <= 109f) // Float rounding at 0.36 × 300dp
        assertTrue(small.headerBottomSpaceDp < 20)
    }

    @Test fun largeTextYieldsMoreRoomForLabelsAndControls() {
        val normal = NightWaveNowPlayingLayoutPolicy.resolve(390f, 800f, 1f)
        val large = NightWaveNowPlayingLayoutPolicy.resolve(390f, 800f, 1.8f)
        assertTrue(large.compact)
        assertTrue(large.artworkDp < normal.artworkDp)
        assertEquals(16, large.artworkCornerDp)
    }

    @Test fun tabletAndLandscapeStayBounded() {
        val tablet = NightWaveNowPlayingLayoutPolicy.resolve(1200f, 1800f, 1f)
        val landscape = NightWaveNowPlayingLayoutPolicy.resolve(900f, 420f, 1f)
        assertEquals(420f, tablet.artworkDp, 0.001f)
        assertEquals(28, tablet.horizontalPaddingDp)
        assertTrue(landscape.compact)
        assertTrue(landscape.artworkDp < 200f)
    }

    @Test fun invalidMeasurementsNeverCrashOrProduceNegativeDp() {
        val inputs = floatArrayOf(-10f, 0f, 24f, 320f, 820f,
            Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        for (width in inputs) for (height in inputs) for (font in inputs) {
            val layout = NightWaveNowPlayingLayoutPolicy.resolve(width, height, font)
            assertTrue(layout.artworkDp.isFinite())
            assertTrue(layout.artworkDp in 0f..420f)
            assertTrue(layout.horizontalPaddingDp >= 0)
            assertTrue(layout.headerBottomSpaceDp >= 0)
        }
    }

    @Test fun normalWidthRetainsFiveControlsAndSideBySideActions() {
        for (width in listOf(360f, 390f, 412f, 700f)) {
            val layout = NightWaveNowPlayingLayoutPolicy.resolve(width, 844f, 1f)
            assertFalse("Standard phone/tablet should keep unified bar", layout.splitTransportControls)
            assertFalse("Standard phone/tablet should keep paired actions", layout.stackQuickActions)
        }
    }

    @Test fun narrowSplitScreenKeepsAllTransportButtonsReachable() {
        for (width in listOf(240f, 260f, 280f, 300f)) {
            val layout = NightWaveNowPlayingLayoutPolicy.resolve(width, 540f, 1f)
            assertTrue("Narrow viewport requires separated mode controls",
                layout.splitTransportControls)
            assertTrue(layout.stackQuickActions)
            assertTrue(layout.artworkDp <= width)
        }
    }

    @Test fun intermediateWidthStacksCardsButStillFitsFiveControlRow() {
        val layout = NightWaveNowPlayingLayoutPolicy.resolve(320f, 700f, 1f)
        assertFalse(layout.splitTransportControls)
        assertTrue(layout.stackQuickActions)
        assertTrue(layout.artworkDp > 0f)
        assertEquals(14, layout.horizontalPaddingDp)
    }

    @Test fun largeFontPrioritizesPrimaryTransportAndReadableCaptions() {
        val normal = NightWaveNowPlayingLayoutPolicy.resolve(390f, 800f, 1f)
        val enlarged = NightWaveNowPlayingLayoutPolicy.resolve(390f, 800f, 1.6f)
        val huge = NightWaveNowPlayingLayoutPolicy.resolve(390f, 800f, 2f)
        assertFalse(normal.splitTransportControls)
        assertFalse(normal.stackQuickActions)
        assertTrue(enlarged.stackQuickActions)
        assertFalse(enlarged.splitTransportControls)
        assertTrue(huge.stackQuickActions)
        assertTrue(huge.splitTransportControls)
        assertTrue(huge.artworkDp < normal.artworkDp)
    }

    @Test fun layoutThresholdsAreDeterministicWithoutFlicker() {
        // Width below 360dp uses 14dp horizontal padding. 311.9 - 28
        // leaves 283.9dp, below the 284dp five-control breakpoint.
        assertTrue(NightWaveNowPlayingLayoutPolicy.resolve(
            311.9f, 720f, 1f).splitTransportControls)
        assertFalse(NightWaveNowPlayingLayoutPolicy.resolve(
            312f, 720f, 1f).splitTransportControls)
        // Under 360dp the existing padding is 14dp, not 20dp.
        // That puts the 320dp content threshold at 348dp.
        assertTrue(NightWaveNowPlayingLayoutPolicy.resolve(
            347.9f, 720f, 1f).stackQuickActions)
        assertFalse(NightWaveNowPlayingLayoutPolicy.resolve(
            348f, 720f, 1f).stackQuickActions)
        assertFalse(NightWaveNowPlayingLayoutPolicy.resolve(
            360f, 720f, 1f).stackQuickActions)
    }

    @Test fun invalidAndExtremeMeasurementsStillMakeSafeTransportDecisions() {
        for (width in listOf(-100f, 0f, 240f, Float.NaN,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            for (scale in listOf(1f, 2f, Float.NaN, Float.POSITIVE_INFINITY)) {
                val layout = NightWaveNowPlayingLayoutPolicy.resolve(width, 650f, scale)
                assertTrue(layout.artworkDp.isFinite())
                assertTrue(layout.artworkDp >= 0f)
                assertTrue(layout.horizontalPaddingDp >= 0)
                if (!width.isFinite() || width <= 0f) {
                    assertTrue(layout.splitTransportControls)
                    assertTrue(layout.stackQuickActions)
                }
            }
        }
    }

    @Test fun ultraNarrowViewportStacksPrimaryControlsOnlyWhenThreeCannotFit() {
        val tooNarrow = NightWaveNowPlayingLayoutPolicy.resolve(203.9f, 700f, 1f)
        val exactFit = NightWaveNowPlayingLayoutPolicy.resolve(204f, 700f, 1f)
        assertTrue(tooNarrow.stackPrimaryTransportControls)
        assertFalse(exactFit.stackPrimaryTransportControls)
        assertTrue(tooNarrow.splitTransportControls)
        assertTrue(exactFit.splitTransportControls)
        assertTrue(tooNarrow.stackQuickActions)
    }

    @Test fun secondaryModeButtonsAlsoStackWhenTheirOwnTargetsCannotFit() {
        val tiny = NightWaveNowPlayingLayoutPolicy.resolve(131.9f, 700f, 1f)
        val justEnough = NightWaveNowPlayingLayoutPolicy.resolve(132f, 700f, 1f)
        assertTrue(tiny.stackSecondaryTransportControls)
        assertFalse(justEnough.stackSecondaryTransportControls)
        assertTrue(tiny.stackPrimaryTransportControls)
        assertTrue(justEnough.stackPrimaryTransportControls)
    }

    @Test fun normalPhonesAndTabletsKeepOriginalTransportArrangement() {
        for (width in listOf(240f, 320f, 360f, 390f, 412f, 700f, 1200f)) {
            val layout = NightWaveNowPlayingLayoutPolicy.resolve(width, 850f, 1f)
            assertFalse("Primary transport must remain horizontal at $width",
                layout.stackPrimaryTransportControls)
            assertFalse(layout.stackSecondaryTransportControls)
        }
        val normal = NightWaveNowPlayingLayoutPolicy.resolve(390f, 850f, 1f)
        assertFalse(normal.splitTransportControls)
        assertFalse(normal.stackQuickActions)
    }

    @Test fun largeTextSplitsModesButDoesNotNeedlesslyStackPrimaryActions() {
        for (scale in listOf(1.3f, 1.6f, 1.8f, 2f, 3f)) {
            val layout = NightWaveNowPlayingLayoutPolicy.resolve(390f, 850f, scale)
            assertFalse(layout.stackPrimaryTransportControls)
            assertFalse(layout.stackSecondaryTransportControls)
        }
        assertTrue(NightWaveNowPlayingLayoutPolicy.resolve(
            390f, 850f, 2f).splitTransportControls)
        assertTrue(NightWaveNowPlayingLayoutPolicy.resolve(
            390f, 850f, 2f).stackQuickActions)
    }

    @Test fun partialTinyWindowStillHasBoundedFiniteArtwork() {
        val sizes = listOf(0f, 1f, 48f, 60f, 80f, 100f, 120f, 130f, 160f, 200f, 204f)
        for (width in sizes) {
            val layout = NightWaveNowPlayingLayoutPolicy.resolve(width, 500f, 2f)
            assertTrue(layout.artworkDp.isFinite())
            assertTrue(layout.artworkDp >= 0f)
            assertTrue(layout.artworkDp <= width)
            if (width < 204f) assertTrue(layout.stackPrimaryTransportControls)
            if (width < 132f) assertTrue(layout.stackSecondaryTransportControls)
        }
    }

    @Test fun corruptDimensionsFailSafeWithoutSpuriousRowWidths() {
        for (width in listOf(Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, -500f, 0f)) {
            val layout = NightWaveNowPlayingLayoutPolicy.resolve(width, Float.NaN, Float.NaN)
            assertTrue(layout.stackPrimaryTransportControls)
            assertTrue(layout.stackSecondaryTransportControls)
            assertTrue(layout.splitTransportControls)
            assertTrue(layout.stackQuickActions)
            assertTrue(layout.artworkDp.isFinite())
        }
    }
}
