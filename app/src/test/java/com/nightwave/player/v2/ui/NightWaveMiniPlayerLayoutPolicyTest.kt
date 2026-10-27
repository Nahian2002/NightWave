package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW116 miniature player decisions must be safe and independent of Media3. */
class NightWaveMiniPlayerLayoutPolicyTest {
    private val p = NightWaveMiniPlayerLayoutPolicy

    @Test fun normalPhonesKeepCurrentPremiumMiniPlayer() {
        for (width in listOf(365f, 390f, 412f)) {
            val r = p.resolve(width, 1f, 3)
            assertEquals(52, r.rowHeightDp)
            assertEquals(40, r.artworkDp)
            assertTrue(r.showArtwork)
            assertTrue(r.showNext)
            assertEquals(1, r.titleLines)
            assertEquals(9, r.labelGapDp)
        }
    }

    @Test fun narrowScreenGivesEssentialActionsAndTitleMoreRoom() {
        for (width in listOf(0f, 180f, 239f, 260f, 279.9f)) {
            val r = p.resolve(width, 1f, 5)
            assertFalse(r.showArtwork)
            assertFalse(r.showNext)
            assertEquals(5, r.labelGapDp)
            assertEquals(52, r.rowHeightDp)
        }
        val boundary = p.resolve(280f, 1f, 5)
        assertTrue(boundary.showArtwork)
        assertEquals(36, boundary.artworkDp)
        assertFalse(boundary.showNext)
    }

    @Test fun largerSystemFontsYieldRoomForTwoTitleLines() {
        val x13 = p.resolve(390f, 1.3f, 3)
        assertEquals(64, x13.rowHeightDp)
        assertEquals(44, x13.artworkDp)
        assertEquals(2, x13.titleLines)
        assertTrue(x13.showNext)
        val x18 = p.resolve(390f, 1.8f, 3)
        assertEquals(104, x18.rowHeightDp)
        assertEquals(2, x18.titleLines)
        assertFalse(x18.showNext)
        assertTrue(x18.showArtwork)
        val x22 = p.resolve(390f, 2.2f, 3)
        assertEquals(124, x22.rowHeightDp)
        assertEquals(36, x22.artworkDp)
        assertEquals(2, x22.titleLines)
    }

    @Test fun magnifiedNarrowLayoutsYieldTitleSpaceBeforeDecorativeArt() {
        val large = p.resolve(320f, 2.2f, 6)
        assertFalse(large.showArtwork)
        assertFalse(large.showNext)
        assertEquals(124, large.rowHeightDp)
        assertEquals(5, large.labelGapDp)
        val wide = p.resolve(360f, 2.2f, 6)
        assertTrue(wide.showArtwork)
        assertFalse(wide.showNext)
    }

    @Test fun keepMiniNextThresholdAtNormalFontAndRequireMultipleSongs() {
        assertFalse(p.resolve(364.9f, 1f, 3).showNext)
        assertTrue(p.resolve(365f, 1f, 3).showNext)
        assertFalse(p.resolve(900f, 1f, 1).showNext)
        assertFalse(p.resolve(900f, 1f, 0).showNext)
        assertFalse(p.resolve(900f, 1f, -1).showNext)
        assertTrue(p.resolve(900f, 1f, 2).showNext)
    }

    @Test fun magnificationPreservesNextOnWideScreens() {
        assertFalse(p.resolve(459.9f, 1.8f, 3).showNext)
        assertTrue(p.resolve(460f, 1.8f, 3).showNext)
        assertTrue(p.resolve(740f, 3f, 3).showNext)
    }

    @Test fun invalidDimensionsAndFontsNeverProduceUnsafeSizes() {
        val sizes = listOf(Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, -100f, 0f, 180f, 320f, 390f, 740f)
        val fonts = listOf(Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, -1f, 0f, 1f, 1.3f, 1.8f, 2.2f, 3f)
        var checks = 0
        for (size in sizes) for (font in fonts) {
            val v = p.resolve(size, font, 2)
            assertTrue(v.rowHeightDp >= 52)
            assertTrue(v.artworkDp in 32..44)
            assertTrue(v.titleLines in 1..2)
            assertTrue(v.labelGapDp in 5..9)
            checks++
        }
        assertEquals(90, checks)
        assertFalse(p.resolve(Float.NaN, 1f, 2).showArtwork)
        assertFalse(p.resolve(Float.POSITIVE_INFINITY, 1f, 2).showNext)
        assertEquals(52, p.resolve(390f, Float.NaN, 2).rowHeightDp)
    }

    @Test fun miniSizingNeverChangesPlaybackSelectionOrSongIdentity() {
        val left = p.resolve(390f, 1f, 5)
        val right = p.resolve(320f, 2.2f, 5)
        assertTrue(left.showArtwork)
        assertFalse(right.showArtwork)
        // Visual changes alone never manufacture another selection or queue.
        assertFalse(p.resolve(390f, 1f, 1).showNext)
        assertFalse(p.resolve(320f, 2.2f, 1).showNext)
    }
}
