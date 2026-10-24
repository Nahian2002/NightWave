package com.nightwave.player.v2.lyrics

import android.content.Context
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.v2.cloud.CloudLyricEntry
import com.nightwave.player.v2.cloud.CloudLyricsPolicy

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

    /** Fast UI hint only. Does not parse or export lyrics or infer song identity. */
    fun hasImportedLrc(id: String): Boolean =
        offlineId.matches(id) && preferences.contains("lrc:" + id)

    fun read(id: String): TimedLyrics? {
        if (!offlineId.matches(id) || offline.find(id) == null) return null
        val saved = preferences.getString("lrc:$id", null) ?: return null
        return runCatching { TimedLyricsCodec.parse(saved.toByteArray(Charsets.UTF_8)) }
            .getOrNull()
    }

    /**
     * Stable fingerprint of the existing imported lyric text for explicit
     * preview/replace decisions. Null means this song has no saved LRC.
     * Raw LRC is never added to diagnostics, intent extras or logs.
     */
    @Synchronized
    fun fingerprint(id: String): String? {
        val key = "lrc:" + validatedId(id)
        val saved = preferences.getString(key, null) ?: return null
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(saved.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(java.util.Locale.ROOT, it.toInt() and 0xff) }
    }

    /**
     * Commit only the file the user explicitly reviewed, and reject a
     * replacement if the previous LRC changed after preview was shown.
     */
    @Synchronized
    fun saveReviewed(id: String, bytes: ByteArray, expectedFingerprint: String?): Boolean {
        if (fingerprint(id) != expectedFingerprint) return false
        save(id, bytes) // Uses the same parser and byte/line bounds as old imports.
        return true
    }

    /** Parse and validate before mutating any saved lyrics. */
    @Synchronized
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

    /**
     * Export only explicit LRC attached to currently present user-imported
     * audio. No audio bytes, source URI or physical file path ever leaves.
     */
    fun exportCloud(): List<CloudLyricEntry> {
        val values = preferences.all
        val records = offline.songs().mapNotNull { song ->
            val text = values["lrc:" + song.id] as? String ?: return@mapNotNull null
            CloudLyricEntry(song.id, text)
        }
        return CloudLyricsPolicy.validated(records)
    }

    /**
     * Reattach only on the SAME local audio ID and only if no LRC already
     * exists. Cross-device offline IDs normally differ; never invent a match.
     */
    @Synchronized
    fun restoreCloudMissing(entries: List<CloudLyricEntry>): Int {
        val clean = CloudLyricsPolicy.validated(entries)
        val validIds = offline.songs().map { it.id }.toSet()
        val existing = preferences.all
        val additions = clean.filter { it.localSongId in validIds &&
            !existing.containsKey("lrc:" + it.localSongId) }
        if (additions.isEmpty()) return 0
        val editor = preferences.edit()
        additions.forEach { editor.putString("lrc:" + it.localSongId, it.lrc) }
        check(editor.commit()) { "Could not restore private LRC" }
        return additions.size
    }

    fun delete(id: String): Boolean {
        val key = "lrc:" + validatedId(id)
        return preferences.edit().remove(key).commit()
    }
}
