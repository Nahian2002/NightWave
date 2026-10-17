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
    val tracks: List<RecordedTrack>,
    /** Rolling, opt-in UTC daily buckets; never exported or sent to Drive. */
    val daily: List<DailyListeningBucket> = emptyList(),
    /** Local recorded candidates only; UI still intersects real saved catalog. */
    val recommendationTracks: List<RecordedTrack> = emptyList()
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
        const val MAX_DAILY_JSON_BYTES = 8_000
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

    private fun readDaily(today: Long): List<DailyListeningBucket> {
        val encoded = prefs.getString("dailyUtc", "[]") ?: "[]"
        if (encoded.toByteArray(Charsets.UTF_8).size > MAX_DAILY_JSON_BYTES) return emptyList()
        return try {
            val arr = JSONArray(encoded)
            if (arr.length() > ListeningTrendEngine.MAX_DAYS) return emptyList()
            val lower = today - ListeningTrendEngine.MAX_DAYS + 1
            val days = mutableListOf<DailyListeningBucket>()
            for (i in 0 until arr.length()) {
                val entry = arr.getJSONObject(i)
                val day = entry.getLong("day")
                if (day !in lower..today) continue
                days += DailyListeningBucket(
                    day,
                    entry.optLong("duration").coerceIn(0L, MAX_TRACK_MS),
                    entry.optInt("plays").coerceIn(0, 1_000_000)
                )
            }
            days.distinctBy { it.epochDayUtc }.sortedBy { it.epochDayUtc }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeDaily(days: List<DailyListeningBucket>): String =
        JSONArray().apply {
            days.sortedBy { it.epochDayUtc }.forEach { item ->
                put(JSONObject().put("day", item.epochDayUtc)
                    .put("duration", item.observedMs)
                    .put("plays", item.qualifiedSessions))
            }
        }.toString()

    fun snapshot(): ListeningMemorySummary = synchronized(lock) {
        val tracks = decode().sortedWith(
            compareByDescending<RecordedTrack> { it.listenedMs }
                .thenByDescending { it.lastAtMs }
        )
        val enabled = prefs.getBoolean("consent", false)
        // Expired UTC buckets are removed even if playback tracking is OFF.
        val today = ListeningTrendEngine.utcEpochDay(System.currentTimeMillis())
        val days = readDaily(today)
        val encoded = encodeDaily(days)
        if (prefs.getString("dailyUtc", "[]") != encoded) {
            prefs.edit().putString("dailyUtc", encoded).commit()
        }
        ListeningMemorySummary(
            enabled = enabled,
            listenedMs = tracks.sumOf { it.listenedMs },
            qualifiedPlays = tracks.sumOf { it.qualifiedPlays.toLong() }
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            trackedSongs = tracks.size,
            tracks = tracks.take(5),
            daily = days,
            recommendationTracks = if (enabled) tracks.filter {
                it.id.startsWith("https://www.youtube.com/watch?v=")
            }.take(100) else emptyList()
        )
    }

    /** Refuse writes if consent was revoked, even if service has an old pulse. */
    fun record(pulse: ListeningPulse, expectedRevision: Long): Boolean = synchronized(lock) {
        // Queued disk work is valid only for the exact opt-in generation.
        // Rapid OFF -> ON cannot resurrect a previous listening segment.
        if (!prefs.getBoolean("consent", false) ||
            prefs.getLong("revision", 0L) != expectedRevision) return@synchronized false
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

        // Attribute an interval to the UTC day it was recorded, not inferred
        // seek time. Short cross-midnight intervals belong to their flush day.
        val today = ListeningTrendEngine.utcEpochDay(System.currentTimeMillis())
        val day = ListeningTrendEngine.utcEpochDay(pulse.recordedAtMs)
        if (day !in (today - ListeningTrendEngine.MAX_DAYS + 1)..today) {
            return@synchronized false
        }
        val previous = readDaily(today)
        val oldBucket = previous.firstOrNull { it.epochDayUtc == day }
        val nextBucket = DailyListeningBucket(
            day,
            ((oldBucket?.observedMs ?: 0L) + pulse.listenedMs)
                .coerceAtMost(MAX_TRACK_MS),
            ((oldBucket?.qualifiedSessions?.toLong() ?: 0L) +
                (if (pulse.qualifyingPlay) 1L else 0L))
                .coerceAtMost(1_000_000L).toInt()
        )
        val dailyData = encodeDaily(
            (previous.filterNot { it.epochDayUtc == day } + nextBucket)
                .sortedBy { it.epochDayUtc }.takeLast(ListeningTrendEngine.MAX_DAYS)
        )
        if (dailyData.toByteArray(Charsets.UTF_8).size > MAX_DAILY_JSON_BYTES) {
            return@synchronized false
        }
        // A single commit prevents an aggregate row without its day bucket.
        prefs.edit().putString("aggregates", data)
            .putString("dailyUtc", dailyData).commit()
    }
}
