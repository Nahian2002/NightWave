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
        assertEquals(30, layout.artworkCornerDp)
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
        assertEquals(24, large.artworkCornerDp)
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
}
