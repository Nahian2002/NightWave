package com.nightwave.player.playback

import android.content.Context

/** Local-only crossfade duration, OFF by default, no audio or cloud data. */
class CrossfadeSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_crossfade_v1", Context.MODE_PRIVATE
    )

    fun read(): Int = runCatching {
        prefs.getInt("seconds", CrossfadePolicy.DEFAULT_SECONDS)
    }.getOrDefault(CrossfadePolicy.DEFAULT_SECONDS).let { value ->
        if (CrossfadePolicy.accepts(value)) value else CrossfadePolicy.DEFAULT_SECONDS
    }

    fun save(seconds: Int): Boolean =
        CrossfadePolicy.accepts(seconds) && runCatching {
            prefs.edit().putInt("seconds", seconds).commit()
        }.getOrDefault(false)
}
