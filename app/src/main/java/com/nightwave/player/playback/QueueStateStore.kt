package com.nightwave.player.playback

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject

/** Persists the music queue as public video IDs/metadata, without expiring stream signatures. */
class QueueStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)

    data class Snapshot(
        val tracks: List<QueueEntry>,
        val index: Int,
        val shuffle: Boolean,
        val repeat: PlaybackQueue.Repeat
    )

    data class Bookmark(val url: String, val positionMs: Long, val durationMs: Long)

    /** Signed audio URLs are never persisted. This records only public IDs and position. */
    fun saveBookmark(url: String, positionMs: Long, durationMs: Long) {
        if (!url.startsWith("https://www.youtube.com/watch?v=")) return
        prefs.edit()
            .putString("bookmark_url", url)
            .putLong("bookmark_position_ms", positionMs.coerceIn(0L, 86_400_000L))
            .putLong("bookmark_duration_ms", durationMs.coerceIn(0L, 86_400_000L))
            .apply()
    }

    fun bookmark(expectedUrl: String?): Bookmark? {
        if (expectedUrl.isNullOrBlank()) return null
        // Old/corrupt preference types must not kill either the player service
        // or an Activity showing the restored previous song.
        return try {
            val url = prefs.getString("bookmark_url", null) ?: return null
            if (url != expectedUrl) return null
            val duration = prefs.getLong("bookmark_duration_ms", 0L).coerceIn(0L, 86_400_000L)
            val raw = prefs.getLong("bookmark_position_ms", 0L).coerceIn(0L, 86_400_000L)
            val position = if (duration > 0L &&
                raw >= (duration - 4_000L).coerceAtLeast(0L)) 0L else raw
            Bookmark(url, position, duration)
        } catch (_: ClassCastException) {
            null // Preserve the original bytes for an explicit backup/repair.
        }
    }

    /**
     * Transient, ID-only public stream lookup state for the UI. No stream
     * signature or private media path is stored. Monotonic TTL makes a service
     * crash or reboot fail closed instead of showing a permanent spinner.
     */
    fun markLookup(publicUrl: String) {
        if (!publicUrl.startsWith("https://www.youtube.com/watch?v=")) return
        prefs.edit().putString("resolving_url", publicUrl)
            .putLong("resolving_at_elapsed", SystemClock.elapsedRealtime()).apply()
    }

    fun clearLookup() {
        prefs.edit().remove("resolving_url").remove("resolving_at_elapsed").apply()
    }

    fun lookupPending(expectedUrl: String?): Boolean {
        if (expectedUrl.isNullOrBlank()) return false
        return try {
            val saved = prefs.getString("resolving_url", null)
            val then = prefs.getLong("resolving_at_elapsed", -1L)
            val now = SystemClock.elapsedRealtime()
            saved == expectedUrl && then >= 0L && now >= then &&
                now - then <= PlaybackResolutionPolicy.RESOLUTION_TIMEOUT_MS
        } catch (_: ClassCastException) {
            false
        }
    }

    /** Safe user-facing failure state, keyed ONLY by public watch URL. */
    fun recordFailure(publicUrl: String, label: String) {
        if (!publicUrl.startsWith("https://www.youtube.com/watch?v=")) return
        prefs.edit().putString("last_failure_url", publicUrl)
            .putString("last_failure_label", label.take(160)).apply()
    }

    fun failure(expectedUrl: String?): String? {
        if (expectedUrl.isNullOrBlank()) return null
        return try {
            if (prefs.getString("last_failure_url", null) != expectedUrl) null
            else prefs.getString("last_failure_label", null)
        } catch (_: ClassCastException) { null }
    }

    fun clearFailure() {
        prefs.edit().remove("last_failure_url").remove("last_failure_label").apply()
    }

    fun save(queue: PlaybackQueue) {
        val tracks = JSONArray()
        queue.snapshot().forEach {
            tracks.put(JSONObject().put("url", it.url).put("title", it.title)
                .put("creator", it.creator).put("artwork", it.artwork))
        }
        val state = JSONObject().put("tracks", tracks).put("index", queue.index)
            .put("shuffle", queue.shuffle).put("repeat", queue.repeat.name)
        prefs.edit().putString("snapshot", state.toString()).apply()
    }

    fun read(): Snapshot = try {
        val state = JSONObject(prefs.getString("snapshot", "{}") ?: "{}")
        val data = state.optJSONArray("tracks") ?: JSONArray()
        val tracks = (0 until data.length().coerceAtMost(100)).mapNotNull { i ->
            val obj = data.optJSONObject(i) ?: return@mapNotNull null
            val url = obj.optString("url")
            if (!url.startsWith("https://www.youtube.com/watch?v=")) return@mapNotNull null
            QueueEntry(url, obj.optString("title"), obj.optString("creator"), obj.optString("artwork"))
        }.distinctBy { it.url }
        val repeat = runCatching {
            PlaybackQueue.Repeat.valueOf(state.optString("repeat", "OFF"))
        }.getOrDefault(PlaybackQueue.Repeat.OFF)
        Snapshot(tracks, state.optInt("index", 0), state.optBoolean("shuffle", false), repeat)
    } catch (_: Exception) {
        Snapshot(emptyList(), 0, false, PlaybackQueue.Repeat.OFF)
    }

    fun restore(queue: PlaybackQueue) {
        val snapshot = read()
        queue.replace(snapshot.tracks, snapshot.index)
        queue.shuffle = snapshot.shuffle
        queue.repeat = snapshot.repeat
    }
}
