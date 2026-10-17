package com.nightwave.player.v2.insights

import android.content.Context
import com.nightwave.player.core.VideoLink
import org.json.JSONArray
import org.json.JSONObject

/** A locally aggregated, opt-in history; never part of library/cloud exports. */
data class RecordedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val listenedMs: Long,
    val qualifiedPlays: Int,
    val lastAtMs: Long
)
data class ListeningMemorySummary(
    val enabled: Boolean,
    val listenedMs: Long,
    val qualifiedPlays: Int,
    val trackedSongs: Int,
    val tracks: List<RecordedTrack>
)

/**
 * The service writes small, bounded aggregates; the UI only reads summaries
 * or explicitly changes consent. All operations share a single in-process lock,
 * so Clear & disable cannot race a service-side write and resurrect history.
 */
class ListeningMemoryStore(context: Context) {
    companion object {
        private val lock = Any()
        const val MAX_TRACKS = 250
        const val MAX_JSON_BYTES = 120_000
        private const val MAX_TRACK_MS = 10L * 365 * 24 * 60 * 60 * 1000
        private val offlineId = Regex(
            "offline:[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)"
        )
    }

    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_listening_memory_v1", Context.MODE_PRIVATE
    )

    fun isEnabled(): Boolean = synchronized(lock) { prefs.getBoolean("consent", false) }

    /** Incremented on every consent change; service resets in-flight session. */
    fun consentRevision(): Long = synchronized(lock) { prefs.getLong("revision", 0L) }

    fun setEnabled(enabled: Boolean): Boolean = synchronized(lock) {
        val next = prefs.getLong("revision", 0L).plus(1L)
        prefs.edit().putBoolean("consent", enabled).putLong("revision", next).commit()
    }

    /** Clearing disables and advances revision atomically: no delayed session survives. */
    fun eraseAndDisable(): Boolean = synchronized(lock) {
        val next = prefs.getLong("revision", 0L).plus(1L)
        prefs.edit().clear().putLong("revision", next).commit()
    }

    private fun approvedId(value: String): Boolean =
        offlineId.matches(value) ||
            (value.length <= 120 && value.startsWith("https://www.youtube.com/watch?v=") &&
                VideoLink.normalize(value) == value)

    private fun decode(): MutableList<RecordedTrack> {
        val data = prefs.getString("aggregates", "[]") ?: "[]"
        if (data.toByteArray(Charsets.UTF_8).size > MAX_JSON_BYTES) return mutableListOf()
        return try {
            val array = JSONArray(data)
            if (array.length() > MAX_TRACKS) return mutableListOf()
            val tracks = mutableListOf<RecordedTrack>()
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                val id = entry.getString("id")
                if (!approvedId(id)) continue
                tracks += RecordedTrack(
                    id, entry.optString("title").take(180),
                    entry.optString("artist").take(120),
                    entry.optLong("duration").coerceIn(0L, MAX_TRACK_MS),
                    entry.optInt("plays").coerceIn(0, 1_000_000),
                    entry.optLong("last").coerceAtLeast(0L)
                )
            }
            tracks.distinctBy { it.id }.toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun snapshot(): ListeningMemorySummary = synchronized(lock) {
        val tracks = decode().sortedWith(
            compareByDescending<RecordedTrack> { it.listenedMs }
                .thenByDescending { it.lastAtMs }
        )
        ListeningMemorySummary(
            enabled = prefs.getBoolean("consent", false),
            listenedMs = tracks.sumOf { it.listenedMs },
            qualifiedPlays = tracks.sumOf { it.qualifiedPlays.coerceAtMost(1_000_000) }
                .coerceAtMost(Int.MAX_VALUE),
            trackedSongs = tracks.size,
            tracks = tracks.take(5)
        )
    }

    /** Refuse writes if consent was revoked, even if service has an old pulse. */
    fun record(pulse: ListeningPulse): Boolean = synchronized(lock) {
        if (!prefs.getBoolean("consent", false)) return@synchronized false
        if (!approvedId(pulse.trackId) ||
            pulse.listenedMs !in 0L..ListeningSessionMeter.MAX_OBSERVED_GAP_MS * 8L ||
            pulse.recordedAtMs < 0L
        ) return@synchronized false
        val tracks = decode()
        val at = tracks.indexOfFirst { it.id == pulse.trackId }
        val old = tracks.getOrNull(at)
        val updated = RecordedTrack(
            id = pulse.trackId,
            title = pulse.title.take(180).ifBlank { old?.title ?: "Unknown track" },
            artist = pulse.artist.take(120).ifBlank { old?.artist.orEmpty() },
            listenedMs = (old?.listenedMs ?: 0L).plus(pulse.listenedMs)
                .coerceAtMost(MAX_TRACK_MS),
            qualifiedPlays = ((old?.qualifiedPlays?.toLong() ?: 0L) +
                (if (pulse.qualifyingPlay) 1L else 0L))
                .coerceAtMost(1_000_000L).toInt(),
            lastAtMs = pulse.recordedAtMs
        )
        if (at >= 0) tracks[at] = updated else tracks.add(updated)
        val bounded = tracks.sortedWith(compareByDescending<RecordedTrack> { it.lastAtMs })
            .take(MAX_TRACKS)
        val data = JSONArray().apply {
            bounded.forEach { entry ->
                put(JSONObject().put("id", entry.id).put("title", entry.title)
                    .put("artist", entry.artist).put("duration", entry.listenedMs)
                    .put("plays", entry.qualifiedPlays).put("last", entry.lastAtMs))
            }
        }.toString()
        if (data.toByteArray(Charsets.UTF_8).size > MAX_JSON_BYTES) return@synchronized false
        prefs.edit().putString("aggregates", data).commit()
    }
}
