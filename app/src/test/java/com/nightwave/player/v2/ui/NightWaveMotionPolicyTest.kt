package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWaveMotionPolicyTest {
    @Test fun motionReducesToZeroForUserPreference() {
        assertEquals(0, NightWaveMotionPolicy.duration(true, 280))
        assertEquals(0, NightWaveMotionPolicy.duration(true, 999))
        assertFalse(NightWaveMotionPolicy.transitionEnabled(true))
        assertEquals(1f, NightWaveMotionPolicy.tabScale(false, true))
        assertEquals(1f, NightWaveMotionPolicy.artworkScale(false, true))
    }

    @Test fun nearbyNavigationAnimatesButLongDistanceSkipsHeavyIntermediatePages() {
        assertTrue(NightWaveMotionPolicy.animateTabChange(0, 1, 4, false))
        assertTrue(NightWaveMotionPolicy.animateTabChange(3, 2, 4, false))
        assertFalse(NightWaveMotionPolicy.animateTabChange(0, 3, 4, false))
        assertFalse(NightWaveMotionPolicy.animateTabChange(1, 1, 4, false))
        assertFalse(NightWaveMotionPolicy.animateTabChange(0, 1, 4, true))
    }

    @Test fun invalidPagesAndExtremeIndicesDoNotAnimate() {
        assertFalse(NightWaveMotionPolicy.animateTabChange(Int.MIN_VALUE,
            Int.MAX_VALUE, 4, false))
        assertFalse(NightWaveMotionPolicy.animateTabChange(0, 1, 0, false))
        assertFalse(NightWaveMotionPolicy.animateTabChange(0, -1, 4, false))
        assertFalse(NightWaveMotionPolicy.animateTabChange(0, 4, 4, false))
        assertFalse(NightWaveMotionPolicy.tabHaptic(0, 5, 4, false))
    }

    @Test fun hapticFeedbackIsOptionalAndUserNavigationOnly() {
        assertTrue(NightWaveMotionPolicy.tabHaptic(0, 3, 4, false))
        assertFalse(NightWaveMotionPolicy.tabHaptic(0, 0, 4, false))
        assertFalse(NightWaveMotionPolicy.tabHaptic(0, 2, 4, true))
        assertFalse(NightWaveMotionPolicy.tabHaptic(-1, 2, 4, false))
    }

    @Test fun playerEntryExitAreSmallAndSafeEvenForInvalidMeasurements() {
        assertEquals(0, NightWaveMotionPolicy.playerEntryOffsetPx(-200))
        assertEquals(0, NightWaveMotionPolicy.playerExitOffsetPx(-200))
        assertEquals(120, NightWaveMotionPolicy.playerEntryOffsetPx(720))
        assertEquals(102, NightWaveMotionPolicy.playerExitOffsetPx(720))
        assertEquals(240, NightWaveMotionPolicy.playerEntryOffsetPx(Int.MAX_VALUE))
        assertEquals(200, NightWaveMotionPolicy.playerExitOffsetPx(Int.MAX_VALUE))
    }

    @Test fun allDeclaredMotionIsBoundedAndReducibleToZero() {
        for (ms in listOf(NightWaveMotionPolicy.NAV_MS,
                NightWaveMotionPolicy.SURFACE_MS,
                NightWaveMotionPolicy.CONTENT_MS,
                NightWaveMotionPolicy.FEEDBACK_MS,
                NightWaveMotionPolicy.COVER_MS,
                NightWaveMotionPolicy.PLAYER_ENTER_MS,
                NightWaveMotionPolicy.PLAYER_EXIT_MS)) {
            assertTrue(ms in 0..NightWaveMotionPolicy.MAX_MS)
            assertEquals(0, NightWaveMotionPolicy.duration(true, ms))
        }
    }

    @Test fun regularMotionIsBoundedAndStateBased() {
        assertEquals(240, NightWaveMotionPolicy.duration(false, 240))
        assertEquals(NightWaveMotionPolicy.MAX_MS,
            NightWaveMotionPolicy.duration(false, 9999))
        assertEquals(0, NightWaveMotionPolicy.duration(false, -50))
        assertTrue(NightWaveMotionPolicy.transitionEnabled(false))
        assertEquals(.93f, NightWaveMotionPolicy.tabScale(false, false))
        assertEquals(1f, NightWaveMotionPolicy.tabScale(true, false))
        assertEquals(.985f, NightWaveMotionPolicy.artworkScale(false, false))
        assertEquals(1f, NightWaveMotionPolicy.artworkScale(true, false))
    }
}
