package com.nightwave.player.v2.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudAuthorizationRecoveryPolicyTest {
    @Test fun onlyKnownPermissionActionsCanSurviveRecreation() {
        for (action in listOf("connect", "backup", "preview", "merge")) {
            assertEquals(action, CloudAuthorizationRecoveryPolicy.safeAction(action))
            assertTrue(CloudAuthorizationRecoveryPolicy.shouldRestore(action, true))
        }
        assertNull(CloudAuthorizationRecoveryPolicy.safeAction("delete"))
        assertFalse(CloudAuthorizationRecoveryPolicy.shouldRestore("delete", true))
        assertFalse(CloudAuthorizationRecoveryPolicy.shouldRestore("connect", false))
    }

    @Test fun staleOrFinishedRequestsDoNotMutateCurrentUi() {
        assertTrue(CloudAuthorizationRecoveryPolicy.requestIsCurrent(3, 3, true, false))
        assertFalse(CloudAuthorizationRecoveryPolicy.requestIsCurrent(2, 3, true, false))
        assertFalse(CloudAuthorizationRecoveryPolicy.requestIsCurrent(3, 3, false, false))
        assertFalse(CloudAuthorizationRecoveryPolicy.requestIsCurrent(3, 3, true, true))
    }

    @Test fun timeoutIsBoundedWithoutUserConsentTimeout() {
        assertTrue(CloudAuthorizationRecoveryPolicy.timeoutMs() in 5_000L..60_000L)
        assertFalse(CloudAuthorizationRecoveryPolicy.shouldRestore(null, true))
    }
}
