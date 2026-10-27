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
    val lastVerifiedRevision: Long = -1L,
    val accessState: CloudAccessState = CloudAccessState.UNKNOWN,
    val lastAccessCheckMs: Long = 0L,
    val accessRevision: Long = 0L
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
        lastVerifiedRevision = prefs.getLong(VERIFIED_REVISION_KEY, -1L),
        accessState = CloudAccessState.entries.firstOrNull {
            it.name == prefs.getString("access_state", "")
        } ?: CloudAccessState.UNKNOWN,
        lastAccessCheckMs = prefs.getLong("access_checked_ms", 0L).coerceAtLeast(0L),
        accessRevision = prefs.getLong("access_revision", 0L)
    )

    fun read(): CloudBackupStatus = synchronized(guard) { readLocked() }

    /** A foreground screen can observe worker results without polling/network access. */
    fun observe(onChange: (CloudBackupStatus) -> Unit): AutoCloseable {
        val active = java.util.concurrent.atomic.AtomicBoolean(true)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            if (active.get()) onChange(read())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onChange(read())
        return AutoCloseable {
            if (active.getAndSet(false)) prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    private fun nextRevision(): Long {
        val current = prefs.getLong(REVISION_KEY, 0L)
        return if (current == Long.MAX_VALUE) 1L else current + 1L
    }

    private fun nextAccessRevision(): Long {
        val current = prefs.getLong("access_revision", 0L)
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
            .putString("access_state", CloudAccessState.CHECKED.name)
            .putLong("access_checked_ms", System.currentTimeMillis())
            .putLong("access_revision", nextAccessRevision())
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
        val current = readLocked()
        val newRevision = nextRevision()
        // Switching Wi-Fi/mobile data changes scheduling, not what the
        // previously uploaded snapshot contains. Keep verified status ONLY
        // if the old snapshot was already verified for its consent revision.
        val stillVerified = CloudBackupFreshnessPolicy.preservesVerifiedSnapshot(key) &&
            current.enabled && current.lastSuccessMs > 0L &&
            current.lastVerifiedRevision == current.revision
        val editor = prefs.edit().putBoolean(key, value)
            .putLong(REVISION_KEY, newRevision)
            .putString("last_result", if (stillVerified)
                "Backup network choice updated; previously verified data is unchanged."
                else "Backup settings changed; back up now to verify the current choices.")
        if (stillVerified) editor.putLong(VERIFIED_REVISION_KEY, newRevision)
        editor.commit()
    }

    fun setHistory(value: Boolean) = setChoice("history", value)
    fun setLyrics(value: Boolean) = setChoice("lyrics", value)
    fun setPreferences(value: Boolean) = setChoice("preferences", value)
    fun setWifiOnly(value: Boolean) = setChoice("wifi_only", value)

    /** No account, token or permission is inferred from a cached backup timestamp. */
    fun accessIfCurrent(consent: CloudBackupConsent, state: CloudAccessState,
        whenMs: Long = System.currentTimeMillis()): Boolean =
        recordAccessIfCurrent(consent, state, whenMs) != null

    /** Return the exact new receipt atomically; a later failure can compare-and-set it. */
    fun recordAccessIfCurrent(consent: CloudBackupConsent, state: CloudAccessState,
        whenMs: Long = System.currentTimeMillis(), expectedAccessRevision: Long? = null): Long? = synchronized(guard) {
        val current = readLocked()
        if (!CloudBackupConsentPolicy.same(consent, current.consentSnapshot()) ||
            (expectedAccessRevision != null && expectedAccessRevision != current.accessRevision) ||
            (state == CloudAccessState.CHECKED && whenMs <= 0L)) return@synchronized null
        val revision = nextAccessRevision()
        val editor = prefs.edit().putString("access_state", state.name).putLong("access_revision", revision)
        if (state == CloudAccessState.CHECKED) editor.putLong("access_checked_ms", whenMs)
        if (editor.commit()) revision else null
    }

    /** Interactive first backup: existing call site and contract are preserved. */
    fun success(account: String, whenMs: Long): Boolean = synchronized(guard) {
        if (whenMs <= 0L || !readLocked().account.equals(account, ignoreCase = true)) {
            return@synchronized false
        }
        prefs.edit().putBoolean("enabled", true)
            .putString("access_state", CloudAccessState.CHECKED.name)
            .putLong("access_checked_ms", whenMs)
            .putLong("access_revision", nextAccessRevision())
            .putLong("last_success_ms", whenMs)
            .putLong(VERIFIED_REVISION_KEY, readLocked().revision)
            .putString("last_result", "Backup verified")
            .commit()
    }

    /** Atomically accept a worker's result only while its exact consent is current. */
    fun successIfCurrent(
        consent: CloudBackupConsent, whenMs: Long, created: Boolean
    ): Boolean = synchronized(guard) {
        if (whenMs <= 0L || !CloudBackupConsentPolicy.same(consent, readLocked().consentSnapshot())) {
            return@synchronized false
        }
        prefs.edit().putBoolean("enabled", true)
            .putString("access_state", CloudAccessState.CHECKED.name)
            .putLong("access_checked_ms", whenMs)
            .putLong("access_revision", nextAccessRevision())
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
            .remove("access_state").remove("access_checked_ms")
            .putLong("access_revision", nextAccessRevision())
            .putLong(VERIFIED_REVISION_KEY, -1L)
            .putLong(REVISION_KEY, nextRevision())
            .putString("last_result", "Disconnected on this device; Drive snapshots remain")
            .commit()
    }
}
