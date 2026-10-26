package com.nightwave.player.v2.cloud

import android.content.Context

/** Local preferences only; no access or refresh token is ever persisted. */
data class CloudBackupStatus(
    val account: String = "",
    val enabled: Boolean = false,
    val includeHistory: Boolean = false,
    val includeLyrics: Boolean = false,
    val includePreferences: Boolean = false,
    val wifiOnly: Boolean = true,
    val lastSuccessMs: Long = 0L,
    val lastResult: String = "Cloud backup is off",
    val revision: Long = 0L,
    /** Last Drive read-back actually verified for this exact consent revision. */
    val lastVerifiedRevision: Long = -1L
)

/** A revision prevents disconnect/reconnect of the same account from matching old work. */
fun CloudBackupStatus.consentSnapshot(): CloudBackupConsent = CloudBackupConsent(
    account, enabled, includeHistory, includeLyrics, includePreferences, wifiOnly, revision
)

class CloudBackupState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_cloud_backup_v1", Context.MODE_PRIVATE
    )

    companion object {
        // Every CloudBackupState instance in this process uses the same guard.
        // A read/consent check and its status write must not race a disconnect.
        private val guard = Any()
        private const val REVISION_KEY = "consent_revision"
        private const val VERIFIED_REVISION_KEY = "last_verified_consent_revision"
    }

    private fun readLocked() = CloudBackupStatus(
        account = prefs.getString("account", "").orEmpty(),
        enabled = prefs.getBoolean("enabled", false),
        includeHistory = prefs.getBoolean("history", false),
        includeLyrics = prefs.getBoolean("lyrics", false),
        includePreferences = prefs.getBoolean("preferences", false),
        wifiOnly = prefs.getBoolean("wifi_only", true),
        lastSuccessMs = prefs.getLong("last_success_ms", 0L),
        lastResult = prefs.getString("last_result", "Cloud backup is off").orEmpty(),
        revision = prefs.getLong(REVISION_KEY, 0L),
        lastVerifiedRevision = prefs.getLong(VERIFIED_REVISION_KEY, -1L)
    )

    fun read(): CloudBackupStatus = synchronized(guard) { readLocked() }

    private fun nextRevision(): Long {
        val current = prefs.getLong(REVISION_KEY, 0L)
        return if (current == Long.MAX_VALUE) 1L else current + 1L
    }

    /** Called only after interactive authorization confirms a specific account. */
    fun connect(account: String): Boolean = synchronized(guard) {
        if (account.isBlank() || !account.contains('@')) return@synchronized false
        val previous = readLocked()
        // Switching accounts MUST require a new first successful upload.
        val switched = !previous.account.equals(account, ignoreCase = true)
        prefs.edit()
            .putString("account", account)
            .putBoolean("enabled", false)
            .putLong("last_success_ms", if (switched) 0 else previous.lastSuccessMs)
            .putLong(VERIFIED_REVISION_KEY,
                if (switched) -1L else previous.lastVerifiedRevision)
            .putLong(REVISION_KEY, nextRevision())
            .putString("last_result", "Account authorized; initial backup pending")
            .commit()
    }

    private fun setChoice(key: String, value: Boolean): Boolean = synchronized(guard) {
        // A duplicate toggle isn't a new consent decision; preserve the
        // receipt and don't falsely label a verified snapshot outdated.
        val previous = prefs.getBoolean(key, key == "wifi_only")
        if (!CloudBackupFreshnessPolicy.changed(previous, value)) return@synchronized true
        prefs.edit().putBoolean(key, value)
            .putLong(REVISION_KEY, nextRevision())
            .putString("last_result",
                "Backup settings changed; back up now to verify the current choices.")
            .commit()
    }

    fun setHistory(value: Boolean) = setChoice("history", value)
    fun setLyrics(value: Boolean) = setChoice("lyrics", value)
    fun setPreferences(value: Boolean) = setChoice("preferences", value)
    fun setWifiOnly(value: Boolean) = setChoice("wifi_only", value)

    /** Interactive first backup: existing call site and contract are preserved. */
    fun success(account: String, whenMs: Long): Boolean = synchronized(guard) {
        if (!readLocked().account.equals(account, ignoreCase = true)) {
            return@synchronized false
        }
        prefs.edit().putBoolean("enabled", true)
            .putLong("last_success_ms", whenMs)
            .putLong(VERIFIED_REVISION_KEY, readLocked().revision)
            .putString("last_result", "Backup verified")
            .commit()
    }

    /** Atomically accept a worker's result only while its exact consent is current. */
    fun successIfCurrent(
        consent: CloudBackupConsent, whenMs: Long, created: Boolean
    ): Boolean = synchronized(guard) {
        if (!CloudBackupConsentPolicy.same(consent, readLocked().consentSnapshot())) {
            return@synchronized false
        }
        prefs.edit().putBoolean("enabled", true)
            .putLong("last_success_ms", whenMs)
            .putLong(VERIFIED_REVISION_KEY, consent.revision)
            .putString("last_result",
                if (created) "Backup verified" else "Already backed up; library unchanged")
            .commit()
    }

    /** Old or cancelled workers cannot replace the current account's status. */
    fun statusIfCurrent(consent: CloudBackupConsent, message: String): Boolean =
        synchronized(guard) {
            if (!CloudBackupConsentPolicy.same(consent, readLocked().consentSnapshot())) {
                return@synchronized false
            }
            prefs.edit().putString("last_result", message.take(130)).commit()
        }

    fun status(message: String): Boolean = synchronized(guard) {
        prefs.edit().putString("last_result", message.take(130)).commit()
    }

    fun disconnect(): Boolean = synchronized(guard) {
        prefs.edit()
            .remove("account").putBoolean("enabled", false)
            .putLong(VERIFIED_REVISION_KEY, -1L)
            .putLong(REVISION_KEY, nextRevision())
            .putString("last_result", "Disconnected on this device; Drive snapshots remain")
            .commit()
    }
}
