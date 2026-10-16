package com.nightwave.player.playback

import android.content.Context
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
