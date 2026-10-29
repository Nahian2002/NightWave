package com.nightwave.player.v2.cloud

import org.junit.Assert.*
import org.junit.Test

class CloudRecoverySessionPolicyTest {
    private val state = CloudBackupStatus(account = "fixture@example.test", enabled = true,
        revision = 7L, accessRevision = 12L, accessState = CloudAccessState.CHECKED,
        lastAccessCheckMs = 1234L, lastSuccessMs = 1234L, lastVerifiedRevision = 7L)
    private val receipt = CloudRecoverySession(state.consentSnapshot(), state.accessRevision)

    @Test fun sameReceiptAllowsOnlyExplicitIdleRecovery() {
        assertTrue(CloudRecoverySessionPolicy.matches(receipt, state.copy()))
        assertTrue(CloudRecoverySessionPolicy.mayApply(receipt, state.copy(account = "FIXTURE@EXAMPLE.TEST"), false))
        assertFalse(CloudRecoverySessionPolicy.mayApply(receipt, state, true))
    }

    @Test fun sameTimestampNewerAccessProofInvalidatesEarlierPreview() {
        val newer = state.copy(accessRevision = 13L)
        assertEquals(state.lastAccessCheckMs, newer.lastAccessCheckMs)
        assertFalse(CloudRecoverySessionPolicy.matches(receipt, newer))
    }

    @Test fun accountReconnectAndEveryPrivacyChoiceInvalidatesReceipt() {
        for (changed in listOf(state.copy(account = "other@example.test"), state.copy(revision = 9L),
            state.copy(includeHistory = true), state.copy(includeLyrics = true),
            state.copy(includePreferences = true), state.copy(wifiOnly = false), state.copy(enabled = false))) {
            assertFalse(CloudRecoverySessionPolicy.matches(receipt, changed))
        }
    }

    @Test fun historicalBackupCannotProveCurrentPermission() {
        for (changed in listOf(state.copy(accessState = CloudAccessState.UNKNOWN),
            state.copy(accessState = CloudAccessState.NEEDS_REVIEW), state.copy(lastAccessCheckMs = 0),
            state.copy(account = ""), state.copy(accessRevision = 0))) {
            assertFalse(CloudRecoverySessionPolicy.matches(receipt, changed))
        }
        assertFalse(CloudRecoverySessionPolicy.matches(receipt.copy(accessRevision = 0), state))
    }

    @Test fun backupResultTextAndTimestampAloneCannotChangeRecoveryAuthority() {
        assertTrue(CloudRecoverySessionPolicy.matches(receipt,
            state.copy(lastResult = "Synthetic failure", lastSuccessMs = 0L, lastVerifiedRevision = -1L)))
        assertFalse(CloudRecoverySessionPolicy.matches(receipt,
            state.copy(accessRevision = 14, lastResult = "Backup verified")))
    }

    @Test fun revokedAndTransientAccessAreDistinctWithoutDestroyingOldBackup() {
        assertEquals(CloudAccessState.NEEDS_REVIEW, CloudAccessPolicy.afterFailure(
            CloudAccessState.CHECKED, CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK, 403))
        assertEquals(CloudAccessState.UNKNOWN, CloudAccessPolicy.afterFailure(
            CloudAccessState.CHECKED, CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK, 503))
        assertEquals(CloudAccessState.CHECKED, CloudAccessPolicy.afterFailure(
            CloudAccessState.CHECKED, CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER, 12501))
        assertEquals(1234L, state.lastSuccessMs)
    }

    @Test fun everyForegroundPhaseNamesRealWorkWithoutInventingPercentOrSuccess() {
        val labels = CloudOperationPhase.entries.map(CloudRecoveryUiPolicy::busyLabel)
        assertEquals(labels.size, labels.distinct().size)
        assertTrue(labels.all { it.isNotBlank() && '%' !in it && !it.contains("complete", ignoreCase = true) })
        assertEquals("Checking or uploading your recovery copy",
            CloudRecoveryUiPolicy.busyLabel(CloudOperationPhase.TRANSFERRING_BACKUP))
        assertEquals("Waiting for your Google permission choice",
            CloudRecoveryUiPolicy.busyLabel(CloudOperationPhase.AWAITING_PERMISSION))
    }
}
