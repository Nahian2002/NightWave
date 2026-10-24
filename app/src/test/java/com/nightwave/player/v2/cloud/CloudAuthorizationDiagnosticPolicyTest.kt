package com.nightwave.player.v2.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudAuthorizationDiagnosticPolicyTest {
    @Test fun googleDeveloperErrorExplainsCertificateRegistration() {
        val message = CloudAuthorizationDiagnosticPolicy.apiStatus(10)
        assertTrue(message.contains("APK SHA-1"))
        assertTrue(message.contains("OAuth"))
        assertFalse(message.contains("Bearer"))
    }

    @Test fun internalErrorEightProvidesActionableNonSensitiveRecovery() {
        val message = CloudAuthorizationDiagnosticPolicy.apiStatus(8)
        assertTrue(message.contains("check-google-oauth.cmd"))
        assertTrue(message.contains("APK signer SHA-1"))
        assertTrue(message.contains("Play services"))
        assertFalse(message.contains("Bearer"))
    }

    @Test fun cancellationAndMissingResultAreNotReportedAsSuccess() {
        assertTrue(CloudAuthorizationDiagnosticPolicy.resolution(false, false, null)
            .contains("without permission"))
        assertTrue(CloudAuthorizationDiagnosticPolicy.resolution(false, true, null)
            .contains("not completed"))
        assertTrue(CloudAuthorizationDiagnosticPolicy.resolution(true, false, null)
            .contains("no permission result"))
    }

    @Test fun apiStatusOverridesUnhelpfulActivityCancellation() {
        assertEquals(CloudAuthorizationDiagnosticPolicy.apiStatus(10),
            CloudAuthorizationDiagnosticPolicy.resolution(false, true, 10))
        assertTrue(CloudAuthorizationDiagnosticPolicy.apiStatus(7).contains("network"))
        assertTrue(CloudAuthorizationDiagnosticPolicy.apiStatus(16).contains("cancelled"))
    }
}
