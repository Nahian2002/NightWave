package com.nightwave.player.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningDispatchPolicyTest {
    @Test fun boundedQueueNeverAllowsAnUnlimitedTelemetryBacklog() {
        assertTrue(ListeningDispatchPolicy.MAX_PENDING_WRITES in 1..64)
    }

    @Test fun warningsAreRateLimitedWhenStorageIsStalled() {
        assertTrue(ListeningDispatchPolicy.shouldWarn(1L, 0L))
        assertTrue(ListeningDispatchPolicy.shouldWarn(60_001L, 1L))
        assertFalse(ListeningDispatchPolicy.shouldWarn(12_000L, 1L))
        assertFalse(ListeningDispatchPolicy.shouldWarn(60_000L, 1L))
        assertFalse(ListeningDispatchPolicy.shouldWarn(-1L, 1L))
        assertFalse(ListeningDispatchPolicy.shouldWarn(1L, 2L))
    }
}
