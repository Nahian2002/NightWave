package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerReadbackPolicyTest {
    private val policy = EqualizerReadbackPolicy

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
