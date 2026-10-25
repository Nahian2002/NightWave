package com.nightwave.player.v2.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudAuthorizationFailurePolicyTest {
    @Test fun errorEightIsTemporaryNotProvenMisconfiguration() {
        val request = CloudAuthorizationFailurePolicy.shortMessage(
            8, CloudAuthorizationFailurePolicy.Stage.REQUEST)
        val chooser = CloudAuthorizationFailurePolicy.shortMessage(
            8, CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER)
        assertTrue(request.contains("internal error (8)"))
        assertTrue(request.contains("before opening account selection"))
        assertTrue(chooser.contains("after account selection"))
        assertTrue(chooser.contains("Retry once"))
        assertFalse(chooser.contains("invalid certificate"))
        assertTrue(CloudAuthorizationFailurePolicy.canSuggestRetry(8))
    }

    @Test fun configurationAndCancellationAreSeparateFailures() {
        val config = CloudAuthorizationFailurePolicy.shortMessage(
            10, CloudAuthorizationFailurePolicy.Stage.REQUEST)
        assertTrue(config.contains("configuration error (10)"))
        assertTrue(config.contains("SHA-1"))
        assertFalse(CloudAuthorizationFailurePolicy.canSuggestRetry(10))
        assertFalse(CloudAuthorizationFailurePolicy.canSuggestRetry(16))
        assertTrue(CloudAuthorizationFailurePolicy.shortMessage(
            16, CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER)
            .contains("cancelled"))
        assertTrue(CloudAuthorizationFailurePolicy.canSuggestRetry(7))
        assertTrue(CloudAuthorizationFailurePolicy.canSuggestRetry(13))
    }

    @Test fun unexpectedStatusNeverLeaksCredentialsOrGoogleResponseBody() {
        val output = CloudAuthorizationFailurePolicy.shortMessage(
            99, CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK)
        // DRIVE_CHECK returns an HTTP status, unlike Google Sign-In statuses.
        // A safe, unrecognized Drive response is labeled "HTTP 99", not "(99)".
        assertTrue(output.contains("HTTP 99"))
        assertFalse(output.contains("authorization error"))
        assertFalse(output.contains("Bearer"))
        assertFalse(output.contains("Authorization:"))
        assertFalse(output.contains("accessToken"))
        assertTrue(output.length < 155)
    }

    @Test fun driveHttpErrorsAreNotMisreportedAsGoogleSignInErrors() {
        val forbidden = CloudAuthorizationFailurePolicy.shortMessage(
            403, CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK)
        assertTrue(forbidden.contains("HTTP 403"))
        assertTrue(forbidden.contains("Drive verification"))
        assertFalse(forbidden.contains("authorization error"))
        val expired = CloudAuthorizationFailurePolicy.shortMessage(
            401, CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK)
        assertTrue(expired.contains("Reconnect"))
        assertTrue(CloudAuthorizationFailurePolicy.shortMessage(
            null, CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK)
            .contains("permission returned"))
    }

    @Test fun onlyDeclaredStagesCanBeReadBackFromPrefs() {
        for (stage in CloudAuthorizationFailurePolicy.Stage.values()) {
            assertEquals(stage, CloudAuthorizationFailurePolicy.supportedStage(stage.name))
            assertTrue(CloudAuthorizationFailurePolicy.stageLabel(stage).isNotBlank())
        }
        assertNull(CloudAuthorizationFailurePolicy.supportedStage("ACCESS_TOKEN"))
        assertNull(CloudAuthorizationFailurePolicy.supportedStage(null))
        assertNull(CloudAuthorizationFailurePolicy.supportedStage("ACCOUNT_NAME"))
    }
}
