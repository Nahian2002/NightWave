package com.nightwave.player.playback

import android.content.Context

/** Private validated preference; no audio paths, library, tokens or history. */
class OfflinePlaybackSpeedStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_offline_speed_v1", Context.MODE_PRIVATE
    )

    fun read(): Int {
        val saved = prefs.getInt("speed_percent", OfflinePlaybackSpeedPolicy.NORMAL_PERCENT)
        return if (OfflinePlaybackSpeedPolicy.accepts(saved)) saved
            else OfflinePlaybackSpeedPolicy.NORMAL_PERCENT
    }

    fun save(percent: Int): Boolean {
        if (!OfflinePlaybackSpeedPolicy.accepts(percent)) return false
        return prefs.edit().putInt("speed_percent", percent).commit()
    }
}
