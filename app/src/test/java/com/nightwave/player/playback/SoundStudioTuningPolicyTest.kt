package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure NW49 EQ: no Android effect instance, song, route or device access. */
class SoundStudioTuningPolicyTest {
    private val policy = SoundStudioTuningPolicy
    private val device = listOf(60, 230, 910, 3_600, 14_000)

    @Test fun generatedCurvesRespectActualDeviceBandCenters() {
        assertEquals(listOf(250, 180, -50, 0, 0),
            policy.draft(policy.Shape.WARM, device, -1500, 1500))
        assertEquals(listOf(-100, 0, 100, 120, 0),
            policy.draft(policy.Shape.VOCALS, device, -1500, 1500))
        assertEquals(listOf(-50, 0, 0, 50, 200),
            policy.draft(policy.Shape.AIRY, device, -1500, 1500))
        assertEquals(List(device.size) { 0 },
            policy.draft(policy.Shape.FLAT, device, -1500, 1500))
    }

    @Test fun neverRequestsOutOfRangeOrOverlyAggressiveBoosts() {
        for (shape in policy.Shape.entries) {
            for (hz in listOf(20, 50, 120, 160, 250, 500, 1000, 2500, 6000, 20000)) {
                assertTrue(policy.targetMb(shape, hz) in -100..250)
            }
            assertTrue(policy.draft(shape, device, -75, 125)!!
                .all { it in -75..125 })
        }
    }

    @Test fun rejectsStaleOrUnsafeCapabilities() {
        assertNull(policy.draft(policy.Shape.WARM, emptyList(), -1500, 1500))
        assertNull(policy.draft(policy.Shape.WARM, listOf(0, 900), -1500, 1500))
        assertNull(policy.draft(policy.Shape.WARM, List(33) { 1000 }, -1500, 1500))
        assertNull(policy.draft(policy.Shape.WARM, device, 1800, 2000))
        assertNull(policy.draft(policy.Shape.WARM, device, 100, -100))
        assertEquals(List(device.size) { 0 },
            policy.draft(policy.Shape.FLAT, device, -1000, 1000))
    }

    @Test fun draftMustBeAppliedAndIsNotAClaimOfAudibleOutput() {
        assertFalse(policy.hasChanged(emptyList(), emptyList()))
        assertFalse(policy.hasChanged(listOf(0, 0), listOf(0, 0)))
        assertTrue(policy.hasChanged(listOf(100, 0), listOf(0, 0)))
        assertTrue(policy.hasChanged(listOf(0, 0), listOf(0)))
        assertEquals("Not applied", policy.statusLabel(true, 1, true))
        assertEquals("Manual EQ active", policy.statusLabel(true, 21, false))
        assertEquals("Device preset active", policy.statusLabel(true, 2, false))
        assertEquals("Effect off", policy.statusLabel(false, 21, false))
    }
}
