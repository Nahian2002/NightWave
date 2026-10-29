package com.nightwave.player.v2.cloud

/** Memory-only recovery authority; a timestamp is never an identity or permission. */
internal data class CloudRecoverySession(val consent: CloudBackupConsent, val accessRevision: Long)

internal object CloudRecoverySessionPolicy {
    fun matches(expected: CloudRecoverySession, current: CloudBackupStatus): Boolean =
        expected.accessRevision > 0L && expected.accessRevision == current.accessRevision &&
            current.accessState == CloudAccessState.CHECKED && current.lastAccessCheckMs > 0L &&
            CloudBackupConsentPolicy.same(expected.consent, current.consentSnapshot()) &&
            expected.consent.account.isNotBlank()

    fun mayApply(expected: CloudRecoverySession, current: CloudBackupStatus, busy: Boolean): Boolean =
        !busy && matches(expected, current)
}

/** Foreground progress only; not persisted or inferred from old backup messages. */
internal enum class CloudOperationPhase {
    IDLE, AUTHORIZING, AWAITING_PERMISSION, CHECKING_ACCOUNT, PREPARING_BACKUP,
    TRANSFERRING_BACKUP, LISTING_BACKUPS, OPENING_BACKUP, MERGING, RESTORING, APPLYING_OPTIONS
}

internal object CloudRecoveryUiPolicy {
    fun busyLabel(phase: CloudOperationPhase): String = when (phase) {
        CloudOperationPhase.IDLE -> "Recovery operation in progress"
        CloudOperationPhase.AUTHORIZING -> "Checking Google permission"
        CloudOperationPhase.AWAITING_PERMISSION -> "Waiting for your Google permission choice"
        CloudOperationPhase.CHECKING_ACCOUNT -> "Verifying the selected Drive account"
        CloudOperationPhase.PREPARING_BACKUP -> "Preparing your selected backup data"
        CloudOperationPhase.TRANSFERRING_BACKUP -> "Checking or uploading your recovery copy"
        CloudOperationPhase.LISTING_BACKUPS -> "Finding recovery copies in Drive"
        CloudOperationPhase.OPENING_BACKUP -> "Reading and checking the selected backup"
        CloudOperationPhase.MERGING -> "Adding reviewed library items"
        CloudOperationPhase.RESTORING -> "Applying the reviewed library restore"
        CloudOperationPhase.APPLYING_OPTIONS -> "Applying selected optional data"
    }
}
