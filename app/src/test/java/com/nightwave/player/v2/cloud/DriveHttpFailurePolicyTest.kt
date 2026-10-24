package com.nightwave.player.v2.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DriveHttpFailurePolicyTest {
    @Test fun oauthErrorsNeedAccountAttentionNotBlindRetries() {
        assertTrue(DriveHttpFailurePolicy.needsAccountAttention(401))
        assertTrue(DriveHttpFailurePolicy.needsAccountAttention(403))
        assertFalse(DriveHttpFailurePolicy.mayRetry(401))
        assertFalse(DriveHttpFailurePolicy.mayRetry(403))
        assertTrue(DriveHttpFailurePolicy.message(403).contains("OAuth"))
    }

    @Test fun networkAndServerErrorsAreRetryable() {
        for (code in listOf(408, 429, 500, 502, 503, 599)) {
            assertTrue(DriveHttpFailurePolicy.mayRetry(code))
        }
        for (code in listOf(400, 401, 403, 404, 409, 422)) {
            assertFalse(DriveHttpFailurePolicy.mayRetry(code))
        }
    }

    @Test fun statusErrorsDoNotContainPrivateIdentifiers() {
        val exception = DriveHttpFailure(401)
        assertEquals(401, exception.statusCode)
        assertFalse(exception.message.orEmpty().contains("Bearer"))
        assertTrue(DriveHttpFailurePolicy.message(404).contains("snapshot"))
    }
}
