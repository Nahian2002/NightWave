package com.nightwave.player.offline

import android.content.Context
import com.nightwave.player.playback.PlaybackQueue

/**
 * Offline controls persist separately from public-video QueueStateStore.
 * No media files, positions or queue identifiers are modified.
 */
class OfflinePlaybackPreferencesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_offline_controls_v1", Context.MODE_PRIVATE
    )

    fun shuffle(): Boolean = runCatching { prefs.getBoolean("shuffle", false) }
        .getOrDefault(false)

    fun repeat(): PlaybackQueue.Repeat = runCatching {
        PlaybackQueue.Repeat.valueOf(prefs.getString("repeat", "OFF") ?: "OFF")
    }.getOrDefault(PlaybackQueue.Repeat.OFF)

    fun setShuffle(enabled: Boolean): Boolean = runCatching {
        prefs.edit().putBoolean("shuffle", enabled).commit()
    }.getOrDefault(false)

    fun setRepeat(mode: PlaybackQueue.Repeat): Boolean = runCatching {
        prefs.edit().putString("repeat", mode.name).commit()
    }.getOrDefault(false)
}
