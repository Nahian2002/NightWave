package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class EqualizerSelectionPolicyTest {
    private val curve = EqualizerSelection(
        21, listOf(60, 230, 910, 3600, 14000), listOf(250, 0, -100, 120, 200))

    @Test fun validCurveRestoresOnExactFrequencyLayout() {
        assertTrue(EqualizerSelectionPolicy.valid(curve))
        assertTrue(EqualizerSelectionPolicy.matchesSession(curve, curve.centersHz))
    }

    @Test fun equalBandCountButDifferentFrequenciesMustNotEnableManualEq() {
        assertFalse(EqualizerSelectionPolicy.matchesSession(curve,
            listOf(60, 230, 910, 3500, 14000)))
    }

    @Test fun reorderedBandLayoutCannotReplayThePreviousGainCurve() {
        assertFalse(EqualizerSelectionPolicy.matchesSession(curve, curve.centersHz.reversed()))
    }

    @Test fun incompleteOrUnsafeGainCurveFailsClosed() {
        assertFalse(EqualizerSelectionPolicy.valid(curve.copy(levelsMb = listOf(0))))
        assertFalse(EqualizerSelectionPolicy.valid(curve.copy(
            levelsMb = listOf(0, 0, 0, 0, 1601))))
    }

    @Test fun corruptedOrDuplicateFrequenciesAreRejected() {
        assertFalse(EqualizerSelectionPolicy.valid(curve.copy(
            centersHz = listOf(60, 60, 910, 3600, 14000))))
        assertFalse(EqualizerSelectionPolicy.valid(curve.copy(
            centersHz = listOf(60, 0, 910, 3600, 14000))))
    }

    @Test fun offAndHardwarePresetsMustNotCarryStaleManualValues() {
        assertTrue(EqualizerSelectionPolicy.valid(EqualizerSelection(0)))
        assertTrue(EqualizerSelectionPolicy.valid(EqualizerSelection(2)))
        assertFalse(EqualizerSelectionPolicy.valid(
            EqualizerSelection(2, curve.centersHz, curve.levelsMb)))
    }
}
