package com.nightwave.player.v2.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWavePlayerOverlayPolicyTest {
    @Test fun overlayIsOpaqueBeforeFirstEnterFrame() {
        assertTrue(NightWavePlayerOverlayPolicy.retainBackdrop(true, false, false))
    }

    @Test fun backdropStaysDuringEnterAndExit() {
        assertTrue(NightWavePlayerOverlayPolicy.retainBackdrop(true, false, true))
        assertTrue(NightWavePlayerOverlayPolicy.retainBackdrop(true, true, true))
        assertTrue(NightWavePlayerOverlayPolicy.retainBackdrop(false, true, false))
        assertTrue(NightWavePlayerOverlayPolicy.retainBackdrop(false, true, true))
    }

    @Test fun backdropIsRemovedAfterExitCompletes() {
        assertFalse(NightWavePlayerOverlayPolicy.retainBackdrop(false, false, false))
    }
}
