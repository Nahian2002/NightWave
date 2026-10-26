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
        val repeat: PlaybackQueue.Repeat,
        /** Explicit user Play Next items, public watch URLs only. */
        val manualNextUrls: List<String> = emptyList(),
        /** Optional NW93 shuffle visit/retrace history; legacy queues omit it. */
        val shuffleRecovery: ShuffleRecoverySnapshot? = null
    )

    data class Bookmark(val url: String, val positionMs: Long, val durationMs: Long)

    /** Signed audio URLs are never persisted. This records only public IDs and position. */
    fun saveBookmark(
        url: String, positionMs: Long, durationMs: Long, flush: Boolean = false
    ): Boolean {
        if (!QueueRestorePolicy.isCanonicalPublicWatchUrl(url)) return false
        return runCatching {
            val editor = prefs.edit()
                .putString("bookmark_url", url)
                .putLong("bookmark_position_ms", positionMs.coerceIn(0L, 86_400_000L))
                .putLong("bookmark_duration_ms", durationMs.coerceIn(0L, 86_400_000L))
            // The 2.5-second ticker remains asynchronous. Pause/onDestroy
            // must instead synchronously flush this same atomic preference edit.
            if (flush) editor.commit() else { editor.apply(); true }
        }.getOrDefault(false)
    }

    fun bookmark(expectedUrl: String?): Bookmark? {
        if (expectedUrl == null || !QueueRestorePolicy.isCanonicalPublicWatchUrl(expectedUrl)) return null
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
        if (!QueueRestorePolicy.isCanonicalPublicWatchUrl(publicUrl)) return
        prefs.edit().putString("resolving_url", publicUrl)
            .putLong("resolving_at_elapsed", SystemClock.elapsedRealtime()).apply()
    }

    fun clearLookup() {
        prefs.edit().remove("resolving_url").remove("resolving_at_elapsed").apply()
    }

    fun lookupPending(expectedUrl: String?): Boolean {
        if (expectedUrl == null || !QueueRestorePolicy.isCanonicalPublicWatchUrl(expectedUrl)) return false
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
        if (!QueueRestorePolicy.isCanonicalPublicWatchUrl(publicUrl)) return
        prefs.edit().putString("last_failure_url", publicUrl)
            .putString("last_failure_label", label.take(160)).apply()
    }

    fun failure(expectedUrl: String?): String? {
        if (expectedUrl == null || !QueueRestorePolicy.isCanonicalPublicWatchUrl(expectedUrl)) return null
        return try {
            if (prefs.getString("last_failure_url", null) != expectedUrl) null
            else prefs.getString("last_failure_label", null)
        } catch (_: ClassCastException) { null }
    }

    fun clearFailure() {
        prefs.edit().remove("last_failure_url").remove("last_failure_label").apply()
    }

    fun save(queue: PlaybackQueue): Boolean {
        val selectedUrl = queue.current?.url
        if (selectedUrl != null && !QueueRestorePolicy.isCanonicalPublicWatchUrl(selectedUrl)) return false
        // Only public, canonical IDs may be persisted, including upcoming rows.
        // A service command must never save signed audio URLs or arbitrary URIs.
        val entries = queue.snapshot().filter {
            QueueRestorePolicy.isCanonicalPublicWatchUrl(it.url)
        }.distinctBy { it.url }.take(100)
        val selectedIndex = if (selectedUrl == null) -1
            else entries.indexOfFirst { it.url == selectedUrl }
        if (selectedUrl != null && selectedIndex < 0) return false
        val tracks = JSONArray()
        entries.forEach {
            tracks.put(JSONObject().put("url", it.url)
                .put("title", it.title.take(250))
                .put("creator", it.creator.take(150))
                .put("artwork", it.artwork.take(2_000)))
        }
        val priority = JSONArray()
        queue.priorityNextUrls().filter { url ->
            QueueRestorePolicy.isCanonicalPublicWatchUrl(url) &&
                entries.indexOfFirst { it.url == url } > selectedIndex
        }.take(100).forEach { priority.put(it) }
        val state = JSONObject().put("tracks", tracks).put("index", selectedIndex)
            .put("selected_url", selectedUrl.orEmpty())
            .put("shuffle", queue.shuffle).put("repeat", queue.repeat.name)
            .put("manual_next", priority)
        if (queue.shuffle && selectedUrl != null) {
            val recovered = queue.shuffleRecoveryState()
            state.put("shuffle_history", JSONObject()
                .put("visited", JSONArray(recovered.visited))
                .put("forward", JSONArray(recovered.forward))
                .put("seen", JSONArray(recovered.seen)))
        }
        // Queue changes happen on explicit selections / transitions, not the
        // 2.5-second playhead ticker. Flush them so a process kill cannot
        // silently revert the selected track to a stale persisted index.
        return prefs.edit().putString("snapshot", state.toString()).commit()
    }

    /** Only string IDs from bounded arrays; malformed history is ignored alone. */
    private fun recoveryArray(raw: JSONObject, name: String): List<String>? {
        val array = raw.optJSONArray(name) ?: return null
        if (array.length() > ShuffleRecoveryPolicy.MAX_HISTORY) return null
        return (0 until array.length()).map { i ->
            (array.opt(i) as? String) ?: return null
        }
    }

    private fun readShuffleRecovery(raw: JSONObject?): ShuffleRecoverySnapshot? {
        raw ?: return null
        val visited = recoveryArray(raw, "visited") ?: return null
        val forward = recoveryArray(raw, "forward") ?: return null
        val seen = recoveryArray(raw, "seen") ?: return null
        return ShuffleRecoverySnapshot(visited, forward, seen)
    }

    fun read(): Snapshot = try {
        val raw = prefs.getString("snapshot", "{}") ?: "{}"
        require(raw.length <= 300_000) { "Oversized queue snapshot" }
        val state = JSONObject(raw)
        val data = state.optJSONArray("tracks") ?: JSONArray()
        require(data.length() <= 100) { "Oversized queue" }
        // Keep original slot identities until after validation and duplicate
        // removal, otherwise a numeric index can shift onto the wrong song.
        val originalUrls = (0 until data.length()).map { i ->
            data.optJSONObject(i)?.optString("url")
        }
        val tracks = (0 until data.length()).mapNotNull { i ->
            val obj = data.optJSONObject(i) ?: return@mapNotNull null
            val url = obj.optString("url")
            if (!QueueRestorePolicy.isCanonicalPublicWatchUrl(url)) return@mapNotNull null
            QueueEntry(url, obj.optString("title").take(250),
                obj.optString("creator").take(150), obj.optString("artwork").take(2_000))
        }.distinctBy { it.url }
        val expectedSelection = if (state.has("selected_url"))
            state.optString("selected_url") else null
        val restoredIndex = QueueRestorePolicy.selectedIndex(originalUrls,
            state.optInt("index", -1), expectedSelection, tracks)
        if (restoredIndex == null) {
            Snapshot(emptyList(), -1, false, PlaybackQueue.Repeat.OFF)
        } else {
            val repeat = runCatching {
                PlaybackQueue.Repeat.valueOf(state.optString("repeat", "OFF"))
            }.getOrDefault(PlaybackQueue.Repeat.OFF)
            val upcomingUrls = tracks.drop(restoredIndex + 1).map { it.url }.toSet()
            val storedPriority = state.optJSONArray("manual_next") ?: JSONArray()
            val priorityUrls = (0 until minOf(storedPriority.length(), 100))
                .mapNotNull { i ->
                    storedPriority.optString(i).takeIf { it in upcomingUrls }
                }.distinct()
            val shuffled = state.optBoolean("shuffle", false)
            val recovery = if (shuffled) {
                ShuffleRecoveryPolicy.validated(
                    readShuffleRecovery(state.optJSONObject("shuffle_history")),
                    tracks.map { it.url }.toSet(), tracks[restoredIndex].url
                )
            } else null
            Snapshot(tracks, restoredIndex, shuffled,
                repeat, priorityUrls, recovery)
        }
    } catch (_: Exception) {
        // Preserve raw preferences for explicit user-controlled recovery.
        Snapshot(emptyList(), -1, false, PlaybackQueue.Repeat.OFF)
    }

    fun restore(queue: PlaybackQueue) {
        val snapshot = read()
        queue.replace(snapshot.tracks, snapshot.index)
        queue.shuffle = snapshot.shuffle
        queue.repeat = snapshot.repeat
        // Shuffle setter resets transient state. Rehydrate only the validated
        // public-ID history for this exact selected queue before priorities.
        queue.restoreShuffleRecovery(snapshot.shuffleRecovery)
        queue.restoreNextPriorities(snapshot.manualNextUrls)
    }
}
