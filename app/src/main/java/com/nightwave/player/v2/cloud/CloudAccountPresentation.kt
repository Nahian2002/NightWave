package com.nightwave.player.v2.cloud

/**
 * UI-only descriptions of local guest identity and optional Drive backup.
 * No email address is used as a NightWave login credential.
 */
object CloudAccountPresentation {
    enum class BackupStage {
        LOCAL_ONLY, CONNECTED_NOT_BACKED_UP, PREVIOUS_BACKUP_NEEDS_REFRESH, VERIFIED_BACKUP
    }

    fun stage(connected: Boolean, enabled: Boolean, lastSuccessMs: Long): BackupStage =
        when {
            !connected -> BackupStage.LOCAL_ONLY
            enabled && lastSuccessMs > 0L -> BackupStage.VERIFIED_BACKUP
            else -> BackupStage.CONNECTED_NOT_BACKED_UP
        }

    fun stage(status: CloudBackupStatus): BackupStage = when (
        CloudBackupFreshnessPolicy.classify(
            status.account.isNotBlank(), status.enabled, status.lastSuccessMs,
            status.revision, status.lastVerifiedRevision)
    ) {
        CloudBackupFreshnessPolicy.Freshness.LOCAL_ONLY -> BackupStage.LOCAL_ONLY
        CloudBackupFreshnessPolicy.Freshness.CONNECTED_PENDING -> BackupStage.CONNECTED_NOT_BACKED_UP
        CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED ->
            BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH
        CloudBackupFreshnessPolicy.Freshness.CURRENT_VERIFIED -> BackupStage.VERIFIED_BACKUP
    }

    /**
     * Compact You-header badge. Keeping the exhaustive stage mapping here
     * prevents UI screens from showing BACKED UP for outdated snapshots.
     * Kotlin compilation fails if a future BackupStage is not handled.
     */
    fun badge(stage: BackupStage): String = when (stage) {
        BackupStage.LOCAL_ONLY -> "LOCAL"
        BackupStage.CONNECTED_NOT_BACKED_UP -> "CONNECTED"
        BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH -> "REFRESH NEEDED"
        BackupStage.VERIFIED_BACKUP -> "BACKED UP"
    }

    fun title(stage: BackupStage): String = when (stage) {
        BackupStage.LOCAL_ONLY -> "Local guest library"
        BackupStage.CONNECTED_NOT_BACKED_UP -> "Drive connected · backup pending"
        BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH -> "Backup needs refreshing"
        BackupStage.VERIFIED_BACKUP -> "Drive backup verified"
    }

    fun details(stage: BackupStage): String = when (stage) {
        BackupStage.LOCAL_ONLY ->
            "Your music and playlists stay on this device until you choose a backup."
        BackupStage.CONNECTED_NOT_BACKED_UP ->
            "Google Drive access is connected. Create and verify a backup before relying on recovery."
        BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH ->
            "A previous backup was verified, but it isn't confirmed for the current backup choices. Back up now to refresh it."
        BackupStage.VERIFIED_BACKUP ->
            "A library backup was verified. This is not continuous cross-device synchronization."
    }

    /** Display-only masking; never mutate the account saved for authorization. */
    fun maskedAccount(email: String): String {
        val input = email.trim()
        val at = input.indexOf('@')
        if (at <= 0 || at >= input.lastIndex) return "Google account"
        val user = input.substring(0, at)
        val domain = input.substring(at + 1)
        if (domain.isBlank()) return "Google account"
        return user.take(1) + "•••@" + domain.take(80)
    }

    fun canBackUp(status: CloudBackupStatus, busy: Boolean): Boolean =
        status.account.isNotBlank() && !busy
}
