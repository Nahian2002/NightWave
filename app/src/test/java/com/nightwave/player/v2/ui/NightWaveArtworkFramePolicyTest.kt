package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class NightWaveArtworkFramePolicyTest {
    private val policy = NightWaveArtworkFramePolicy
    @Test fun landscapeArtUsesItsProportionsInsteadOfASquarePanel() {
        assertEquals(180f, policy.heightDp(320f, 1600, 900, false), .01f)
        assertEquals(240f, policy.heightDp(320f, 640, 480, false), .01f)
    }
    @Test fun squareAndPortraitArtKeepSafeContainment() {
        assertEquals(320f, policy.heightDp(320f, 600, 600, false), .01f)
        assertEquals(320f, policy.heightDp(320f, 400, 800, false), .01f)
    }
    @Test fun missingDimensionsHaveStableSourceAwarePlaceholders() {
        assertEquals(240f, policy.heightDp(320f, 0, 0, false), .01f)
        assertEquals(320f, policy.heightDp(320f, 0, 0, true), .01f)
    }
    @Test fun extremePanoramasNeverCollapseTheArtworkArea() {
        assertTrue(policy.heightDp(320f, Int.MAX_VALUE, 1, false) >= 320f / 1.9f - .01f)
    }
    @Test fun malformedMeasurementInputsCannotProduceInvalidLayout() {
        for (width in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f))
            assertEquals(0f, policy.heightDp(width, -1, 0, false), 0f)
    }
}
