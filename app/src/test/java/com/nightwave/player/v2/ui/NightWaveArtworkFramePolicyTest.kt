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
    @Test fun matchingSolidSidebarsTrimWithoutCroppingEdgeDetail() {
        val edge = 0xff114158.toInt()
        val art = 0xfffcd092.toInt()
        val trim = policy.sideTrim(1600, 900) { x, _ ->
            if (x < 350 || x >= 1250) edge else art
        }
        assertEquals(NightWaveArtworkFramePolicy.SideTrim(350, 350), trim)
        assertNull(policy.sideTrim(1600, 900) { x, y -> if (y < 450 && x < 350) edge else art })
        assertNull(policy.sideTrim(1600, 900) { _, _ -> edge })
        assertNull(policy.sideTrim(1600, 900) { x, _ -> if (x < 350 || x >= 1340) edge else art })
        assertNull(policy.sideTrim(900, 900) { _, _ -> edge })
    }
    @Test fun malformedMeasurementInputsCannotProduceInvalidLayout() {
        for (width in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f))
            assertEquals(0f, policy.heightDp(width, -1, 0, false), 0f)
    }
}
