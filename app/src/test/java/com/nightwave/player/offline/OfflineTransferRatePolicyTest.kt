package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTransferRatePolicyTest {
    @Test fun throughputIsMeasuredFromObservedBytesNotAdvertised() {
        assertEquals(1_048_576L,
            (OfflineTransferRatePolicy.bytesPerSecond(524_288L, 500L) ?: -1L))
        assertNull(OfflineTransferRatePolicy.bytesPerSecond(0L, 800L))
        assertNull(OfflineTransferRatePolicy.bytesPerSecond(10L, 0L))
    }

    @Test fun formatsSafeRatesAndUnknowns() {
        assertEquals("Speed calculating", OfflineTransferRatePolicy.display(null))
        assertEquals("1.0 MB/s", OfflineTransferRatePolicy.display(1_048_576L))
        assertTrue(OfflineTransferRatePolicy.display(32_768L).contains("32 KB/s"))
    }

    @Test fun etaRequiresKnownTotalAndPositiveSpeed() {
        assertNull(OfflineTransferRatePolicy.etaSeconds(1L, -1L, 800L))
        assertNull(OfflineTransferRatePolicy.etaSeconds(1L, 100L, null))
        assertEquals(9L, (OfflineTransferRatePolicy.etaSeconds(10L, 100L, 10L) ?: -1L))
    }

    @Test fun slowLinksAreNotRoundedUpToFakeOneKilobytePerSecond() {
        assertEquals("1 B/s", OfflineTransferRatePolicy.display(1L))
        assertEquals("512 B/s", OfflineTransferRatePolicy.display(512L))
        assertEquals("1.5 KB/s", OfflineTransferRatePolicy.display(1536L))
        assertEquals("9.5 KB/s", OfflineTransferRatePolicy.display(9728L))
        assertEquals("10 KB/s", OfflineTransferRatePolicy.display(10240L))
    }

    @Test fun nonzeroRemainingTimeRoundsUpRatherThanDisappearing() {
        assertEquals(1L, OfflineTransferRatePolicy.etaSeconds(99L, 100L, 10L))
        assertEquals(2L, OfflineTransferRatePolicy.etaSeconds(97L, 100L, 2L))
        assertEquals(9L, OfflineTransferRatePolicy.etaSeconds(10L, 100L, 10L))
        assertNull(OfflineTransferRatePolicy.etaSeconds(101L, 100L, 100L))
        assertNull(OfflineTransferRatePolicy.etaSeconds(0L, -1L, 10L))
    }
}
