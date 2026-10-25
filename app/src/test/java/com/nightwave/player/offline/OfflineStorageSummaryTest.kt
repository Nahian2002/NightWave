package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineStorageSummaryTest {
    @Test fun formatsSmallAndLargeBytes() {
        assertEquals("0 B", OfflineStorageSummary.size(0L))
        assertEquals("512 B", OfflineStorageSummary.size(512L))
        assertEquals("1.0 KB", OfflineStorageSummary.size(1024L))
        assertEquals("1.5 MB", OfflineStorageSummary.size(1572864L))
        assertEquals("2.0 GB", OfflineStorageSummary.size(2147483648L))
    }

    @Test fun neverClaimsUnavailableEntriesArePlayable() {
        val shown = OfflineStorageSummary.label(3, 3L * 1024 * 1024, 4096, 2)
        assertTrue(shown.contains("3 playable"))
        assertTrue(shown.contains("3.0 MB imported audio"))
        assertTrue(shown.contains("2 entries need attention"))
    }

    @Test fun unknownFreeSpaceDoesNotFakeAReading() {
        val shown = OfflineStorageSummary.label(0, 0, -1, 0)
        assertEquals("0 playable · 0 B imported audio", shown)
    }
}
