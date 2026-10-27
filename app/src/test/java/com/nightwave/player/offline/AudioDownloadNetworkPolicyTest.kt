package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** No live network, Android device or media files required. */
class AudioDownloadNetworkPolicyTest {
    @Test fun temporaryHttpErrorsUseScheduledBackoffInsteadOfAnImmediateRetryLoop() {
        for (code in listOf(408, 425, 429, 500, 502, 503, 504)) {
            assertTrue("HTTP $code must back off", AudioDownloadNetworkPolicy.shouldBackOff(code))
        }
    }

    @Test fun permanentFailuresMustNotBeTreatedAsTransient() {
        for (code in listOf(200, 206, 300, 301, 400, 401, 403, 404, 410, 416, 451)) {
            assertFalse("HTTP $code must not be automatically resubmitted",
                AudioDownloadNetworkPolicy.shouldBackOff(code))
        }
    }

    @Test fun retryableExceptionIsNetworkFailureWithoutUrlOrPrivateResponse() {
        val failure = RetryableAudioSourceResponse(429)
        assertEquals(429, failure.statusCode)
        assertEquals("HTTP 429", failure.message)
        assertTrue(failure is java.io.IOException)
    }

    @Test fun slowTransfersHaveBoundedWindowLongerThanTwoHours() {
        assertTrue(AudioDownloadNetworkPolicy.MAX_TRANSFER_MINUTES >= 150L)
        assertTrue(AudioDownloadNetworkPolicy.MAX_TRANSFER_MINUTES <= 240L)
    }

    @Test fun requestsWithoutTrustedCheckpointsRejectPartialOrContradictoryBodies() {
        assertTrue(AudioDownloadNetworkPolicy.isFullStartResponse(200, null))
        assertTrue(AudioDownloadNetworkPolicy.isFullStartResponse(200, ""))
        assertFalse(AudioDownloadNetworkPolicy.isFullStartResponse(206, "bytes 0-99/200"))
        assertFalse(AudioDownloadNetworkPolicy.isFullStartResponse(206, null))
        assertFalse(AudioDownloadNetworkPolicy.isFullStartResponse(200, "bytes 0-99/200"))
        for (status in listOf(201, 204, 301, 302, 400, 403, 404, 416, 429, 503)) {
            assertFalse("HTTP $status is not a complete audio transfer",
                AudioDownloadNetworkPolicy.isFullStartResponse(status, null))
        }
        // Advertising ranges is not enough to safely resume a transfer:
        // persisted bytes, validator and Content-Range all need binding.
        assertFalse(AudioDownloadNetworkPolicy.isFullStartResponse(
            200, "bytes 100-199/200"))
    }
}
