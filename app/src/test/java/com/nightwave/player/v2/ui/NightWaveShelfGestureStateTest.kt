package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class NightWaveShelfGestureStateTest {
    @Test fun overlappingShelfOwnersReleaseIndependently() {
        val state = NightWaveShelfGestureState(); val a = Any(); val b = Any()
        state.begin(a); state.begin(b); state.end(a); assertTrue(state.active)
        state.end(b); assertFalse(state.active)
    }
    @Test fun repeatedBeginAndCancelledUnknownOwnersCannotLeaveAStaleCount() {
        val state = NightWaveShelfGestureState(); val owner = Any()
        state.begin(owner); state.begin(owner); state.end(Any()); assertTrue(state.active)
        state.end(owner); state.end(owner); assertFalse(state.active)
    }
}
