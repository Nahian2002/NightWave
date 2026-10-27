package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerReadbackPolicyTest {
    private val policy = EqualizerReadbackPolicy

    @Test fun nw120DevicePresetMustReadBackTheRequestedPresetIndex() {
        assertEquals(EqualizerReadbackPolicy.Result.VERIFIED,
            policy.inspectPreset(2, 1, listOf(-200, 0, 400), -1500, 1500))
        for (actual in listOf(-1, 0, 2, 20)) assertEquals(EqualizerReadbackPolicy.Result.MISMATCH,
            policy.inspectPreset(2, actual, listOf(-200, 0, 400), -1500, 1500))
    }

    @Test fun nw120PresetWithMissingInvalidOrOutOfRangeBandReadbackIsRejected() {
        for (levels in listOf(emptyList(), listOf(1600), List(33) { 0 }))
            assertEquals(EqualizerReadbackPolicy.Result.INVALID,
                policy.inspectPreset(1, 0, levels, -1500, 1500))
        for (preset in listOf(0, 21, Int.MAX_VALUE))
            assertEquals(EqualizerReadbackPolicy.Result.INVALID,
                policy.inspectPreset(preset, 0, listOf(0), -1500, 1500))
        assertEquals(EqualizerReadbackPolicy.Result.INVALID,
            policy.inspectPreset(1, 0, listOf(0), 100, -100))
    }

    @Test fun nw120HardwarePresetBoundsRemainIndependentOfManualGainLimit() {
        assertEquals(EqualizerReadbackPolicy.Result.VERIFIED,
            policy.inspectPreset(20, 19, listOf(-2000, 2000), -2400, 2400))
        assertEquals(EqualizerReadbackPolicy.Result.INVALID,
            policy.inspect(listOf(2000), listOf(2000)))
    }

    @Test fun nw88ExactManualBandReadbackCanBeReportedAsVerified() {
        assertEquals(EqualizerReadbackPolicy.Result.VERIFIED,
            policy.inspect(listOf(-250, 0, 200, 300), listOf(-250, 0, 200, 300)))
        assertTrue(policy.activeLabel(EqualizerReadbackPolicy.Result.VERIFIED)
            .contains("Output route may bypass effects"))
    }

    @Test fun nw88DeviceQuantizationIsNotFabricatedAsAnExactMatch() {
        assertEquals(EqualizerReadbackPolicy.Result.DEVICE_QUANTIZED,
            policy.inspect(listOf(-225, 0, 275), listOf(-200, 0, 250)))
        assertTrue(policy.activeLabel(EqualizerReadbackPolicy.Result.DEVICE_QUANTIZED)
            .contains("device-rounded"))
    }

    @Test fun nw88IgnoredBoostOrLargeMismatchCannotClaimManualEqActive() {
        assertEquals(EqualizerReadbackPolicy.Result.MISMATCH,
            policy.inspect(listOf(300, -200, 0), listOf(0, 0, 0)))
        assertEquals(EqualizerReadbackPolicy.Result.MISMATCH,
            policy.inspect(listOf(100), listOf(-100)))
        assertTrue(policy.activeLabel(EqualizerReadbackPolicy.Result.MISMATCH)
            .contains("effect disabled"))
    }

    @Test fun nw88MalformedOrStaleBandReceiptsFailClosed() {
        val invalid = EqualizerReadbackPolicy.Result.INVALID
        assertEquals(invalid, policy.inspect(emptyList(), emptyList()))
        assertEquals(invalid, policy.inspect(listOf(100, 0), listOf(100)))
        assertEquals(invalid, policy.inspect(listOf(1501), listOf(1000)))
        assertEquals(invalid, policy.inspect(listOf(0), listOf(2000)))
        assertEquals(invalid, policy.inspect(List(33) { 0 }, List(33) { 0 }))
    }

    @Test fun nw88ReadbackNeverClaimsMeasuredAudibleBluetoothDifference() {
        for (result in EqualizerReadbackPolicy.Result.entries) {
            val text = policy.activeLabel(result)
            assertTrue(text.isNotBlank())
            assertTrue(!text.contains("Bluetooth confirmed"))
            assertTrue(!text.contains("guaranteed audible"))
        }
    }
}
