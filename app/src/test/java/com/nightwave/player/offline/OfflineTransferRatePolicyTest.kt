package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTransferRatePolicyTest {
    @Test fun throughputIsMeasuredFromObservedBytesNotAdvertised() {
        assertEquals(1_048_576L,
            OfflineTransferRatePolicy.bytesPerSecond(524_288L, 500L))
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
        assertEquals(9L, OfflineTransferRatePolicy.etaSeconds(10L, 100L, 10L))
    }
}
