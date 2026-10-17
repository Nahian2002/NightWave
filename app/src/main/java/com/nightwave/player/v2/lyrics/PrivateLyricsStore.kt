package com.nightwave.player.v2.lyrics

import android.content.Context
import com.nightwave.player.offline.OfflineAudioLibrary

/**
 * Lyrics live in separate, app-private preferences; 5U backup and
 * cloud library merge do not include them. A user imports every LRC explicitly.
 */
class PrivateLyricsStore(context: Context) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences(
        "nightwave_private_lrc_v1", Context.MODE_PRIVATE
    )
    private val offline = OfflineAudioLibrary(app)
    private val offlineId = Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)")

    private fun validatedId(id: String): String {
        require(offlineId.matches(id)) { "Lyrics require a local audio track" }
        require(offline.find(id) != null) { "This local audio file is not in your library" }
        return id
    }

    fun read(id: String): TimedLyrics? {
        if (!offlineId.matches(id) || offline.find(id) == null) return null
        val saved = preferences.getString("lrc:$id", null) ?: return null
        return runCatching { TimedLyricsCodec.parse(saved.toByteArray(Charsets.UTF_8)) }
            .getOrNull()
    }

    /** Parse and validate before mutating any saved lyrics. */
    fun save(id: String, bytes: ByteArray) {
        val key = "lrc:" + validatedId(id)
        val parsed = TimedLyricsCodec.parse(bytes)
        // Normalize user input to bounded, unambiguous synchronized timestamps.
        val normalized = parsed.cues.joinToString("\n") { cue ->
            val min = cue.timeMs / 60000L
            val sec = (cue.timeMs / 1000L) % 60
            val ms = cue.timeMs % 1000L
            "[%02d:%02d.%03d] %s".format(java.util.Locale.ROOT,
                min, sec, ms, cue.text)
        }
        val data = normalized.toByteArray(Charsets.UTF_8)
        require(data.size in 1..TimedLyricsCodec.MAX_BYTES) {
            "Lyrics text exceeds private storage limit"
        }
        require(preferences.edit().putString(key, normalized).commit()) {
            "Could not save the imported lyrics"
        }
    }

    fun delete(id: String): Boolean {
        val key = "lrc:" + validatedId(id)
        return preferences.edit().remove(key).commit()
    }
}
