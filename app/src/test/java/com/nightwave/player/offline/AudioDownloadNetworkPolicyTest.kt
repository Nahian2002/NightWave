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
}
