package com.nightwave.player.v2.cloud

import org.junit.Assert.*
import org.junit.Test

class CloudAccessPolicyTest {
    private val request = CloudAuthorizationFailurePolicy.Stage.REQUEST
    private val drive = CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK

    @Test fun cancellationPreservesThePreviouslyCheckedAccount() {
        for (state in CloudAccessState.entries) for (code in listOf(16, 12501))
            assertEquals(state, CloudAccessPolicy.afterFailure(state, request, code))
    }

    @Test fun permissionAndConfigurationFailuresNeedReviewWithoutClaimingSignOut() {
        for (code in listOf(4, 10, 17)) assertEquals(CloudAccessState.NEEDS_REVIEW,
            CloudAccessPolicy.afterFailure(CloudAccessState.CHECKED, request, code))
        for (code in listOf(401, 403)) assertEquals(CloudAccessState.NEEDS_REVIEW,
            CloudAccessPolicy.afterFailure(CloudAccessState.CHECKED, drive, code))
    }

    @Test fun transientFailuresMakeCurrentAccessUnknownRatherThanRevoked() {
        for (code in listOf(null, 7, 8, 13)) assertEquals(CloudAccessState.UNKNOWN,
            CloudAccessPolicy.afterFailure(CloudAccessState.CHECKED, request, code))
        for (code in listOf(404, 408, 429, 500, 503)) assertEquals(CloudAccessState.UNKNOWN,
            CloudAccessPolicy.afterFailure(CloudAccessState.CHECKED, drive, code))
    }

    @Test fun httpAndGoogleStatusDomainsAreNeverInterchanged() {
        assertEquals(CloudAccessState.UNKNOWN,
            CloudAccessPolicy.afterFailure(CloudAccessState.CHECKED, drive, 16))
        assertEquals(CloudAccessState.UNKNOWN,
            CloudAccessPolicy.afterFailure(CloudAccessState.CHECKED, request, 401))
    }

    @Test fun aSavedAccountOrBackupReceiptCannotInventCheckedAccess() {
        assertTrue(CloudAccessPolicy.label(true, CloudAccessState.UNKNOWN, 1000).contains("not checked"))
        assertTrue(CloudAccessPolicy.label(true, CloudAccessState.CHECKED, 0).contains("not checked"))
        assertEquals("Account checked", CloudAccessPolicy.label(true, CloudAccessState.CHECKED, 1000))
        assertEquals("On this device", CloudAccessPolicy.label(false, CloudAccessState.CHECKED, 1000))
    }

    @Test fun helpRemainsAvailableForFailuresOnAnAlreadySavedAccount() {
        assertTrue(CloudAccessPolicy.needsHelp(true, CloudAccessState.NEEDS_REVIEW, false))
        assertTrue(CloudAccessPolicy.needsHelp(true, CloudAccessState.UNKNOWN, true))
        assertTrue(CloudAccessPolicy.needsHelp(false, CloudAccessState.UNKNOWN, false))
        assertFalse(CloudAccessPolicy.needsHelp(true, CloudAccessState.CHECKED, false))
    }

    @Test fun compactHeaderNeverLabelsUnknownOrFailedAccessAsConnectedOrBackedUp() {
        assertEquals("SAVED", CloudAccessPolicy.compactBadge(true, CloudAccessState.UNKNOWN, "BACKED UP"))
        assertEquals("CHECK ACCESS", CloudAccessPolicy.compactBadge(true, CloudAccessState.NEEDS_REVIEW, "BACKED UP"))
        assertEquals("BACKED UP", CloudAccessPolicy.compactBadge(true, CloudAccessState.CHECKED, "BACKED UP"))
        assertEquals("LOCAL", CloudAccessPolicy.compactBadge(false, CloudAccessState.NEEDS_REVIEW, "BACKED UP"))
    }
}
