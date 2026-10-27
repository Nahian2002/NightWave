package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadePolicyTest {
    @Test fun nw120OverlappingEqDecksRequireTheSamePositiveAudioSession() {
        for (preset in listOf(1, 20, 21)) {
            assertTrue(CrossfadePolicy.compatibleEffectSession(preset, 42, 42))
            assertFalse(CrossfadePolicy.compatibleEffectSession(preset, 42, 43))
            assertFalse(CrossfadePolicy.compatibleEffectSession(preset, 0, 0))
            assertFalse(CrossfadePolicy.compatibleEffectSession(preset, -1, -1))
        }
    }

    @Test fun nw120EqOffKeepsIndependentCrossfadeDeckSessionsAvailable() {
        assertTrue(CrossfadePolicy.compatibleEffectSession(0, 42, 43))
        assertTrue(CrossfadePolicy.compatibleEffectSession(0, 0, -1))
    }
    @Test fun defaultIsOffAndDurationBounded() {
        assertEquals(0, CrossfadePolicy.DEFAULT_SECONDS)
        assertFalse(CrossfadePolicy.accepts(-1))
        assertTrue(CrossfadePolicy.accepts(0))
        assertTrue(CrossfadePolicy.accepts(1))
        assertTrue(CrossfadePolicy.accepts(10))
        assertFalse(CrossfadePolicy.accepts(11))
    }

    @Test fun onlyEligibleOwnedOfflineTimelineCanCrossfade() {
        assertTrue(CrossfadePolicy.canPrepare(4, true, true, true, 0, 3, 60_000L, 55_000L))
        assertFalse(CrossfadePolicy.canPrepare(4, false, true, true, 0, 3, 60_000L, 55_000L))
        assertFalse(CrossfadePolicy.canPrepare(4, true, false, true, 0, 3, 60_000L, 55_000L))
        assertFalse(CrossfadePolicy.canPrepare(4, true, true, false, 0, 3, 60_000L, 55_000L))
        assertFalse(CrossfadePolicy.canPrepare(4, true, true, true, 2, 3, 60_000L, 55_000L))
        assertFalse(CrossfadePolicy.canPrepare(4, true, true, true, 0, 3, 4_500L, 0L))
    }

    @Test fun neverAmplifiesOrBreaksEndpoints() {
        assertEquals(1f to 0f, CrossfadePolicy.gains(0f, 1f))
        assertEquals(0.5f to 0.5f, CrossfadePolicy.gains(.5f, 1f))
        assertEquals(0f to 1f, CrossfadePolicy.gains(1f, 1f))
        assertEquals(.25f to .25f, CrossfadePolicy.gains(.5f, .5f))
        assertEquals(1f to 0f, CrossfadePolicy.gains(Float.NaN, 1f))
    }

    @Test fun boundedRampClampsLateTicks() {
        assertEquals(0f, CrossfadePolicy.progress(-10L, 4))
        assertEquals(.5f, CrossfadePolicy.progress(2_000L, 4))
        assertEquals(1f, CrossfadePolicy.progress(99_999L, 4))
        assertEquals(0f, CrossfadePolicy.progress(1_000L, 0))
    }
}
