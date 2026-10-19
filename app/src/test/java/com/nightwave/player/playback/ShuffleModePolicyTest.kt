package com.nightwave.player.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleModePolicyTest {
    @Test fun explicitOnIsIdempotentEvenIfServiceWasAlreadyOn() {
        assertTrue(ShuffleModePolicy.resolve(false, true, true))
        assertTrue(ShuffleModePolicy.resolve(true, true, true))
    }

    @Test fun explicitOffDoesNotRetoggleAndUndoTheRequest() {
        assertFalse(ShuffleModePolicy.resolve(false, true, false))
        assertFalse(ShuffleModePolicy.resolve(true, true, false))
    }

    @Test fun rapidOnOffOnRequestsAlwaysEndEnabled() {
        var state = false
        for (enabled in listOf(true, false, true)) {
            state = ShuffleModePolicy.resolve(state, true, enabled)
        }
        assertTrue(state)
    }

    @Test fun legacyCommandsStillToggleExactlyOnce() {
        assertTrue(ShuffleModePolicy.resolve(false, false, false))
        assertFalse(ShuffleModePolicy.resolve(true, false, true))
    }
}
