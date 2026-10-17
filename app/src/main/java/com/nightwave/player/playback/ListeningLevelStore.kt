package com.nightwave.player.playback

import android.content.Context

/** One fail-closed integer preference; never stores tracks or device volume. */
class ListeningLevelStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_listening_level_v1", Context.MODE_PRIVATE
    )

    fun read(): Int {
        val value = runCatching {
            prefs.getInt("level_percent", ListeningLevelPolicy.DEFAULT_PERCENT)
        }.getOrDefault(ListeningLevelPolicy.DEFAULT_PERCENT)
        return if (ListeningLevelPolicy.accepts(value)) value
            else ListeningLevelPolicy.DEFAULT_PERCENT
    }

    fun save(percent: Int): Boolean {
        if (!ListeningLevelPolicy.accepts(percent)) return false
        return runCatching {
            prefs.edit().putInt("level_percent", percent).commit()
        }.getOrDefault(false)
    }
}
