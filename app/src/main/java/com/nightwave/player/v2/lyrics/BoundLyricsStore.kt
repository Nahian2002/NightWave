package com.nightwave.player.v2.lyrics

import android.content.Context
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.offline.OfflineLibraryIndexLock

/** Routes local IDs to their original stores. Public-reference LRC is explicitly
 * imported, device-private, bounded and excluded from the existing cloud format.
 * This class never resolves a stream, fetches words or guesses a source match.
 */
internal class BoundLyricsStore(context: Context, private val local: PrivateLyricsStore,
    private val offsets: PrivateLyricsOffsetStore) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private fun publicId(id: String): String {
        require(LyricsTrackBindingPolicy.isPublic(id)) { "Lyrics require an exact supported source" }
        return id
    }
    private fun raw(id: String): String? {
        val value = prefs.all["lrc:" + publicId(id)]
        check(value == null || value is String) { "Private lyrics need review; existing data kept" }
        return value as? String
    }
    fun title(id: String, publicTitle: String): String = if (LyricsTrackBindingPolicy.isLocal(id))
        requireNotNull(OfflineAudioLibrary(app).find(id)) { "This local song is no longer indexed" }.title
        else { publicId(id); publicTitle.filterNot { it.isISOControl() }.trim().take(320).ifBlank { "Selected public video" } }

    fun read(id: String): TimedLyrics? = if (LyricsTrackBindingPolicy.isLocal(id)) local.read(id) else
        runCatching { raw(id)?.let { TimedLyricsCodec.parse(it.toByteArray(Charsets.UTF_8)) } }.getOrNull()

    fun fingerprint(id: String): String? = synchronized(writeLock) {
        if (LyricsTrackBindingPolicy.isLocal(id)) local.fingerprint(id)
        else raw(id)?.let(OwnedLyricsContentPolicy::fingerprint)
    }

    fun saveReviewed(id: String, bytes: ByteArray, expectedFingerprint: String?,
        active: () -> Boolean = { true }): Boolean {
        val normalized = OwnedLyricsContentPolicy.normalized(bytes)
        return synchronized(writeLock) {
            if (!active()) return false
            if (LyricsTrackBindingPolicy.isLocal(id)) synchronized(OfflineLibraryIndexLock.monitor) {
                if (!active()) false else local.saveReviewed(id, normalized.toByteArray(Charsets.UTF_8), expectedFingerprint)
            } else {
                publicId(id)
                if (fingerprint(id) != expectedFingerprint) return false
                val all = prefs.all.filterKeys { it.startsWith("lrc:") }.map { (key, value) ->
                    check(value is String) { "Private lyrics need review; existing data kept" }
                    key.removePrefix("lrc:") to value
                }.toMap()
                require(OwnedLyricsContentPolicy.withinQuota(all, id, normalized)) {
                    "Device-private public lyrics limit reached; remove an older imported LRC and retry"
                }
                if (!active()) false else prefs.edit().putString("lrc:$id", normalized).commit()
            }
        }
    }

    fun delete(id: String, active: () -> Boolean = { true }): Boolean = synchronized(writeLock) {
        if (!active()) return false
        if (LyricsTrackBindingPolicy.isLocal(id)) synchronized(OfflineLibraryIndexLock.monitor) {
            if (!active()) false else local.delete(id)
        } else {
            publicId(id)
            prefs.edit().remove("lrc:$id").remove("offset:$id").commit()
        }
    }

    fun readOffset(id: String): Int = if (LyricsTrackBindingPolicy.isLocal(id)) offsets.read(id) else
        runCatching { publicId(id); prefs.getInt("offset:$id", 0) }.getOrDefault(0)
            .takeIf(LyricsSyncPolicy::validOffset) ?: 0

    fun saveOffset(id: String, offsetMs: Int, active: () -> Boolean = { true }): Boolean = synchronized(writeLock) {
        require(LyricsSyncPolicy.validOffset(offsetMs)) { "Invalid lyric offset" }
        if (!active()) return false
        if (LyricsTrackBindingPolicy.isLocal(id)) synchronized(OfflineLibraryIndexLock.monitor) {
            if (!active()) false else offsets.save(id, offsetMs)
        } else {
            require(raw(id) != null) { "Import owned lyrics for this exact source before setting timing" }
            val editor = prefs.edit()
            if (offsetMs == 0) editor.remove("offset:$id") else editor.putInt("offset:$id", offsetMs)
            if (!active()) false else editor.commit()
        }
    }
    companion object {
        const val PREFS_NAME = "nightwave_public_lrc_v1"
        private val writeLock = Any()
    }
}
