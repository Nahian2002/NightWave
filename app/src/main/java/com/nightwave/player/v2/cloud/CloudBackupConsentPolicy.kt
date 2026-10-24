package com.nightwave.player.v2.cloud

/**
 * Exact user consent captured when exporting optional Drive backup data.
 * The access token is deliberately excluded. A backup prepared with outdated
 * history/lyrics/preferences/network choices must not be marked current.
 */
data class CloudBackupConsent(
    val account: String,
    val enabled: Boolean,
    val history: Boolean,
    val lyrics: Boolean,
    val preferences: Boolean,
    val wifiOnly: Boolean
)

object CloudBackupConsentPolicy {
    fun same(expected: CloudBackupConsent, current: CloudBackupConsent): Boolean =
        expected.account.isNotBlank() &&
            expected.account.equals(current.account, ignoreCase = true) &&
            expected.enabled == current.enabled &&
            expected.history == current.history &&
            expected.lyrics == current.lyrics &&
            expected.preferences == current.preferences &&
            expected.wifiOnly == current.wifiOnly
}
