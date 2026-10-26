package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW83 contract: compact, split-screen and accessibility sizing with no Android UI. */
class NightWavePremiumDesignTokensTest {
    private val d = NightWavePremiumDesignTokens

    @Test fun responsivePageGuttersHaveOneSourceOfTruth() {
        assertEquals(14, d.pageGutterDp(320f))
        assertEquals(20, d.pageGutterDp(390f))
        assertEquals(28, d.pageGutterDp(900f))
        assertEquals(14, d.pageGutterDp(Float.NaN))
        assertEquals(14, d.pageGutterDp(Float.POSITIVE_INFINITY))
        assertEquals(14, d.pageGutterDp(-10f))
    }

    @Test fun rowsGrowWithoutClippingLargeAccessibilityText() {
        assertEquals(72, d.rowMinHeightDp(1f))
        assertEquals(88, d.rowMinHeightDp(1.3f))
        assertEquals(108, d.rowMinHeightDp(1.8f))
        assertEquals(108, d.rowMinHeightDp(3f))
        assertEquals(72, d.rowMinHeightDp(Float.NaN))
        assertTrue(d.rowMinHeightDp(1.8f) > d.rowMinHeightDp(1f))
    }

    @Test fun largerOrWiderTextAreasPermitTwoLineTrackNames() {
        assertEquals(1, d.titleLines(1f, 320f))
        assertEquals(1, d.titleLines(1f, 429f))
        assertEquals(2, d.titleLines(1f, 430f))
        assertEquals(2, d.titleLines(1.3f, 320f))
        assertEquals(2, d.titleLines(3f, Float.NaN))
        assertEquals(1, d.titleLines(Float.NaN, Float.NaN))
    }

    @Test fun everyControlAndCardHasAStableAccessibleGeometry() {
        assertEquals(48, d.MIN_TOUCH_TARGET_DP)
        assertEquals(48, d.controlTouchTargetDp(1f))
        assertEquals(56, d.controlTouchTargetDp(2f))
        assertTrue(d.MIN_TOUCH_TARGET_DP >= 48)
        assertTrue(d.ROW_ARTWORK_DP + 2 * d.ROW_VERTICAL_INSET_DP <= d.rowMinHeightDp(1f))
        assertTrue(d.CARD_CORNER_DP in 12..28)
        assertTrue(d.SEARCH_CARD_CORNER_DP in 12..28)
        assertTrue(d.HERO_CORNER_DP in 12..32)
        assertEquals(740, d.MAX_CONTENT_WIDTH_DP)
    }

    @Test fun selectionRemainsRestrainedAndDistinct() {
        assertTrue(d.rowBorderAlpha(true) > d.rowBorderAlpha(false))
        assertTrue(d.selectedFillAlpha(true) > d.selectedFillAlpha(false))
        assertTrue(d.rowBorderAlpha(true) in 0f..1f)
        assertTrue(d.rowBorderAlpha(false) in 0f..1f)
        assertTrue(d.selectedFillAlpha(true) in 0f..1f)
        assertTrue(d.selectedFillAlpha(false) in 0f..1f)
    }
}
