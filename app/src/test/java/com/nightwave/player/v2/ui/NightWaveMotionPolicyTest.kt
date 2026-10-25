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
