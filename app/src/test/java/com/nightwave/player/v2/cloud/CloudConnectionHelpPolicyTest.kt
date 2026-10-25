package com.nightwave.player.v2.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudConnectionHelpPolicyTest {
    private val policy = CloudConnectionHelpPolicy
    private val picker = CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER
    private val drive = CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK

    @Test fun errorEightIsRetryableButDoesNotImplyConnected() {
        val info = policy.guidance(picker, 8)
        assertTrue(info.offerSingleRetry)
        assertTrue(info.needsSetupReport)
        assertTrue(info.detail.contains("picker appeared"))
        assertTrue(info.detail.contains("does not mean Drive connected"))
        assertTrue(info.nextStep.contains("SHA-1"))
    }

    @Test fun configurationTenDoesNotOfferBlindRetry() {
        val info = policy.guidance(picker, 10)
        assertFalse(info.offerSingleRetry)
        assertTrue(info.needsSetupReport)
        assertTrue(info.nextStep.contains("Drive API"))
    }

    @Test fun cancellationKeepsLocalLibraryAndDoesNotAutoRetry() {
        for (code in listOf(16, 12501)) {
            val info = policy.guidance(picker, code)
            assertFalse(info.offerSingleRetry)
            assertTrue(info.detail.contains("offline music"))
        }
    }

    @Test fun driveHttpCodesAreNotConfusedWithGoogleSignInStatuses() {
        assertTrue(policy.guidance(drive, 403).detail.contains("Drive API"))
        assertFalse(policy.guidance(drive, 403).offerSingleRetry)
        assertTrue(policy.guidance(drive, 429).offerSingleRetry)
        assertTrue(policy.guidance(drive, 503).offerSingleRetry)
        assertTrue(policy.guidance(drive, 401).title.contains("expired"))
    }

    @Test fun copiedFailureStatusDoesNotShowTokensOrEmails() {
        for (code in listOf(null, 7, 8, 10, 13, 16, 401, 403, 503)) {
            val state = policy.guidance(picker, code)
            val text = state.title + state.detail + state.nextStep
            assertFalse(text.contains("Bearer "))
            assertFalse(text.contains("accessToken"))
            assertFalse(text.contains("Authorization:"))
            assertFalse(text.contains("@gmail.com"))
        }
        assertTrue(policy.guidance(
            CloudAuthorizationFailurePolicy.Stage.REQUEST, null).needsSetupReport)
    }
}
