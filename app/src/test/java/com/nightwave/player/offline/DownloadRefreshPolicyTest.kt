package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Test

/** Presentation timing only. No WorkManager or Android dependency. */
class DownloadRefreshPolicyTest {
    private val policy = DownloadRefreshPolicy

    @Test fun firstDisplayAndClockResetRenderImmediately() {
        assertEquals(0L, policy.delayMs(-1L, 30L))
        assertEquals(0L, policy.delayMs(-1L, 0L))
        assertEquals(0L, policy.delayMs(8_000L, 2_000L))
    }

    @Test fun burstsAreSpacedAndEventuallyFlushed() {
        assertEquals(400L, policy.delayMs(1_000L, 1_000L))
        assertEquals(399L, policy.delayMs(1_000L, 1_001L))
        assertEquals(250L, policy.delayMs(1_000L, 1_150L))
        assertEquals(1L, policy.delayMs(1_000L, 1_399L))
        assertEquals(0L, policy.delayMs(1_000L, 1_400L))
        assertEquals(0L, policy.delayMs(1_000L, 8_000L))
    }

    @Test fun monotonicLongBoundariesDoNotOverflowOrGenerateNegativeWaits() {
        assertEquals(0L, policy.delayMs(Long.MAX_VALUE - 500L, Long.MAX_VALUE))
        assertEquals(1L, policy.delayMs(Long.MAX_VALUE - 399L, Long.MAX_VALUE))
        assertEquals(0L, policy.delayMs(Long.MAX_VALUE, 0L))
        assertEquals(0L, policy.delayMs(0L, Long.MAX_VALUE))
    }
}
