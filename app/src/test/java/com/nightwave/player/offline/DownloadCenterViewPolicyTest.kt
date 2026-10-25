package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure UI-only rules. No network, WorkManager instance or personal library. */
class DownloadCenterViewPolicyTest {
    @Test fun statusDependsOnActualFileNotWorkManagerSuccessAlone() {
        val p = DownloadCenterViewPolicy
        assertTrue(p.matches(p.Filter.READY, "SUCCEEDED", true))
        assertFalse(p.matches(p.Filter.READY, "SUCCEEDED", false))
        assertTrue(p.matches(p.Filter.ATTENTION, "SUCCEEDED", false))
        assertTrue(p.matches(p.Filter.ATTENTION, "FAILED", false))
        assertTrue(p.matches(p.Filter.ATTENTION, "CANCELLED", false))
        assertFalse(p.matches(p.Filter.ATTENTION, "RUNNING", false))
        for (state in listOf("ENQUEUED", "RUNNING", "BLOCKED")) {
            assertTrue(p.matches(p.Filter.ACTIVE, state, false))
            assertFalse(p.matches(p.Filter.READY, state, true))
        }
        assertTrue(p.matches(p.Filter.ALL, "UNKNOWN", false))
    }

    @Test fun percentCannotOverflowOrMisreportInvalidTotals() {
        val p = DownloadCenterViewPolicy
        assertEquals(0, p.percent(0L, 100L))
        assertEquals(50, p.percent(50L, 100L))
        assertEquals(100, p.percent(400L, 100L))
        assertEquals(100, p.percent(Long.MAX_VALUE, Long.MAX_VALUE))
        assertNull(p.percent(-1L, 100L))
        assertNull(p.percent(10L, 0L))
        assertNull(p.percent(10L, -1L))
    }

    @Test fun frozenThroughputIsNotMisreportedAsCurrentSpeed() {
        val p = DownloadCenterViewPolicy
        assertEquals(200_000L, p.recentRate(200_000L, 1000L, 9000L))
        assertNull(p.recentRate(200_000L, 1000L, 9001L))
        assertNull(p.recentRate(200_000L, 2000L, 1000L))
        assertNull(p.recentRate(null, 1000L, 2000L))
        assertNull(p.recentRate(-1L, 1000L, 2000L))
    }

    @Test fun emptyScreensTellTheTruth() {
        val p = DownloadCenterViewPolicy
        assertTrue(p.emptyMessage(p.Filter.ACTIVE).contains("No active"))
        assertTrue(p.emptyMessage(p.Filter.READY).contains("No verified"))
        assertTrue(p.emptyMessage(p.Filter.ATTENTION).contains("No failed"))
    }
}
