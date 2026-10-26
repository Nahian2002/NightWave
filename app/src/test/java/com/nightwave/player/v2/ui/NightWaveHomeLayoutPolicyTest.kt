package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWaveHomeLayoutPolicyTest {
    private val policy = NightWaveHomeLayoutPolicy

    @Test fun standardAndWidePhonesGetBalancedMusicCards() {
        assertEquals(1, policy.mixColumns(320f, 1f))
        assertEquals(1, policy.mixColumns(389.9f, 1f))
        assertEquals(2, policy.mixColumns(390f, 1f))
        assertEquals(2, policy.mixColumns(430f, 1f))
        assertEquals(2, policy.mixColumns(1000f, 1f))
    }

    @Test fun largerTextGetsReadableFullWidthMixCards() {
        assertEquals(1, policy.mixColumns(430f, 1.3f))
        assertEquals(1, policy.mixColumns(1000f, 2f))
        assertEquals(2, policy.mixColumns(430f, 1.29f))
        assertTrue(policy.mixCardCompact(1))
        assertFalse(policy.mixCardCompact(2))
    }

    @Test fun invalidDisplayMeasurementsDoNotBreakHome() {
        assertEquals(1, policy.mixColumns(Float.NaN, 1f))
        assertEquals(1, policy.mixColumns(Float.POSITIVE_INFINITY, 1f))
        assertEquals(1, policy.mixColumns(-1f, 1f))
        assertEquals(2, policy.mixColumns(400f, Float.NaN))
    }
}
