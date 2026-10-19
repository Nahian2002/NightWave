package com.nightwave.player.widget

import android.content.Context

/**
 * Minimal widget display cache. Only shortened visible title/artist and state;
 * no file locations, credentials, audio, stream URLs or queue IDs.
 */
class NowPlayingWidgetStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_widget_state_v1", Context.MODE_PRIVATE)

    fun read(): WidgetPlaybackSnapshot = runCatching {
        NowPlayingWidgetPolicy.normalized(WidgetPlaybackSnapshot(
            hasTrack = prefs.getBoolean("has_track", false),
            isPlaying = prefs.getBoolean("playing", false),
            title = prefs.getString("title", "").orEmpty(),
            artist = prefs.getString("artist", "").orEmpty()
        ))
    }.getOrDefault(WidgetPlaybackSnapshot())

    fun write(snapshot: WidgetPlaybackSnapshot): Boolean {
        val safe = NowPlayingWidgetPolicy.normalized(snapshot)
        return runCatching {
            prefs.edit()
                .putBoolean("has_track", safe.hasTrack)
                .putBoolean("playing", safe.isPlaying)
                .putString("title", safe.title)
                .putString("artist", safe.artist)
                .commit()
        }.getOrDefault(false)
    }
}
