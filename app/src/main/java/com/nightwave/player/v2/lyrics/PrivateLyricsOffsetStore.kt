package com.nightwave.player.v2.lyrics

import android.content.Context
import com.nightwave.player.offline.OfflineAudioLibrary

/**
 * Local per-offline-track lyric offsets; stored separately from imported LRC
 * and not included in existing cloud lyric exports or backups.
 * No migration or rewrite of `nightwave_private_lrc_v1` occurs here.
 */
class PrivateLyricsOffsetStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(
        "nightwave_private_lrc_offset_v1", Context.MODE_PRIVATE)
    private val offline = OfflineAudioLibrary(app)
    private val validId = Regex(
        "[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)"
    )

    fun read(id: String): Int {
        if (!validId.matches(id)) return 0
        return runCatching { prefs.getInt("offset:$id", 0) }
            .getOrDefault(0).takeIf(LyricsSyncPolicy::validOffset) ?: 0
    }

    /** Only currently indexed, imported local audio can acquire a new offset. */
    fun save(id: String, offsetMs: Int): Boolean {
        require(validId.matches(id) && offline.find(id) != null) {
            "Lyrics offset requires a local imported audio track"
        }
        require(LyricsSyncPolicy.validOffset(offsetMs)) { "Invalid lyric offset" }
        val edit = prefs.edit()
        if (offsetMs == 0) edit.remove("offset:$id")
        else edit.putInt("offset:$id", offsetMs)
        return edit.commit()
    }
}
