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
    val lastResult: String = "Cloud backup is off"
)

class CloudBackupState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_cloud_backup_v1", Context.MODE_PRIVATE
    )

    fun read() = CloudBackupStatus(
        account = prefs.getString("account", "").orEmpty(),
        enabled = prefs.getBoolean("enabled", false),
        includeHistory = prefs.getBoolean("history", false),
        includeLyrics = prefs.getBoolean("lyrics", false),
        includePreferences = prefs.getBoolean("preferences", false),
        wifiOnly = prefs.getBoolean("wifi_only", true),
        lastSuccessMs = prefs.getLong("last_success_ms", 0L),
        lastResult = prefs.getString("last_result", "Cloud backup is off").orEmpty()
    )

    /** Called only after interactive authorization confirms a specific account. */
    fun connect(account: String): Boolean {
        if (account.isBlank() || !account.contains('@')) return false
        val previous = read()
        // Switching accounts MUST require a new first successful upload.
        val switched = !previous.account.equals(account, ignoreCase = true)
        return prefs.edit()
            .putString("account", account)
            .putBoolean("enabled", false)
            .putLong("last_success_ms", if (switched) 0 else previous.lastSuccessMs)
            .putString("last_result", "Account authorized; initial backup pending")
            .commit()
    }

    fun setHistory(value: Boolean) = prefs.edit().putBoolean("history", value).commit()
    fun setLyrics(value: Boolean) = prefs.edit().putBoolean("lyrics", value).commit()
    fun setPreferences(value: Boolean) = prefs.edit().putBoolean("preferences", value).commit()
    fun setWifiOnly(value: Boolean) = prefs.edit().putBoolean("wifi_only", value).commit()

    fun success(account: String, whenMs: Long): Boolean {
        if (!read().account.equals(account, ignoreCase = true)) return false
        return prefs.edit().putBoolean("enabled", true)
            .putLong("last_success_ms", whenMs)
            .putString("last_result", "Backup verified")
            .commit()
    }

    fun status(message: String): Boolean =
        prefs.edit().putString("last_result", message.take(130)).commit()

    fun disconnect(): Boolean = prefs.edit()
        .remove("account").putBoolean("enabled", false)
        .putString("last_result", "Disconnected on this device; Drive snapshots remain")
        .commit()
}
