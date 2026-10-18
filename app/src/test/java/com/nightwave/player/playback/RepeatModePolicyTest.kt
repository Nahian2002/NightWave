package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Repeat selection must remain correct when taps outrun service/UI refresh. */
class RepeatModePolicyTest {
    @Test fun cyclesOffAllOneOff() {
        val off = PlaybackQueue.Repeat.OFF
        val all = RepeatModePolicy.next(off)
        val one = RepeatModePolicy.next(all)
        assertEquals(PlaybackQueue.Repeat.ALL, all)
        assertEquals(PlaybackQueue.Repeat.ONE, one)
        assertEquals(off, RepeatModePolicy.next(one))
    }

    @Test fun rapidThreeTapsCanReturnOffWithoutStateRefresh() {
        var mode = PlaybackQueue.Repeat.OFF
        repeat(3) { mode = RepeatModePolicy.next(mode) }
        assertEquals(PlaybackQueue.Repeat.OFF, mode)
    }

    @Test fun explicitRequestsAreAbsoluteAndIdempotent() {
        val off = PlaybackQueue.Repeat.OFF
        val all = PlaybackQueue.Repeat.ALL
        assertEquals(all, RepeatModePolicy.resolve(off, true, "ALL"))
        assertEquals(all, RepeatModePolicy.resolve(all, true, "ALL"))
        assertEquals(off, RepeatModePolicy.resolve(all, true, "OFF"))
    }

    @Test fun invalidOrMissingExplicitModeNeverCycles() {
        assertNull(RepeatModePolicy.resolve(PlaybackQueue.Repeat.ALL, true, null))
        assertNull(RepeatModePolicy.resolve(PlaybackQueue.Repeat.ALL, true, "REPEAT_ALL"))
        assertNull(RepeatModePolicy.resolve(PlaybackQueue.Repeat.ALL, true, ""))
    }

    @Test fun legacyToggleStillCyclesWhenNoModeExtraPresent() {
        assertEquals(PlaybackQueue.Repeat.ONE,
            RepeatModePolicy.resolve(PlaybackQueue.Repeat.ALL, false, null))
    }
}
