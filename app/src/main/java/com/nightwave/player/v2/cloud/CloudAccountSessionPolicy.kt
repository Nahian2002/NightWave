package com.nightwave.player.v2.cloud

/**
 * NW72 — a Drive preview/restore belongs to one exact authorized local
 * account session. Email comparison ALONE is insufficient: disconnecting and
 * reconnecting the same account invalidates all earlier preview receipts.
 *
 * A consent revision also changes after privacy-option edits. Rejecting
 * those old previews is deliberately conservative. Never store OAuth tokens,
 * Drive IDs, account emails or raw error payloads in this policy.
 */
object CloudAccountSessionPolicy {
    fun same(expected: CloudBackupConsent, current: CloudBackupConsent): Boolean =
        expected.account.isNotBlank() &&
            expected.account.equals(current.account, ignoreCase = true) &&
            expected.revision == current.revision

    /** Only explicit, authorized restore/merge actions can use a preview. */
    fun mayApply(expected: CloudBackupConsent, current: CloudBackupConsent,
                 busy: Boolean): Boolean = !busy && same(expected, current)
}
