package com.nightwave.player.playback

import android.content.Context

/**
 * Private, opt-in normalization and file-identity cache.
 * Never stores audio samples, paths, lyrics or cloud credentials.
 */
class OfflineNormalizationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_offline_normalization_v1", Context.MODE_PRIVATE
    )

    fun enabled(): Boolean = runCatching { prefs.getBoolean("enabled", false) }
        .getOrDefault(false)

    fun setEnabled(enabled: Boolean): Boolean = runCatching {
        prefs.edit().putBoolean("enabled", enabled).commit()
    }.getOrDefault(false)

    fun savedGain(id: String, bytes: Long): Float? {
        if (!validId(id) || bytes <= 0L) return null
        return runCatching {
            if (prefs.getLong("bytes_$id", -1L) != bytes) return null
            prefs.getFloat("gain_$id", Float.NaN).takeIf { it.isFinite() && it in .25f..1f }
        }.getOrNull()
    }

    fun saveGain(id: String, bytes: Long, gain: Float): Boolean {
        if (!validId(id) || bytes <= 0L || !gain.isFinite() ||
            gain !in .25f..1f) return false
        return runCatching {
            prefs.edit().putLong("bytes_$id", bytes)
                .putFloat("gain_$id", gain).commit()
        }.getOrDefault(false)
    }

    private fun validId(id: String): Boolean =
        id.length in 5..64 && id.matches(Regex("[0-9a-zA-Z._-]+")) &&
            '/' !in id && '\\' !in id
}
