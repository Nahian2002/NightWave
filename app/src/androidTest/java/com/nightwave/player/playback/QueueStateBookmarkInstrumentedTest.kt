package com.nightwave.player.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class QueueStateBookmarkInstrumentedTest {
    private lateinit var app: Context
    private lateinit var store: QueueStateStore
    private val first = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val next = "https://www.youtube.com/watch?v=J---aiyznGQ"

    @Before fun start() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = QueueStateStore(app)
    }

    @After fun cleanup() {
        app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun malformedBookmarkTypesFailClosedWithoutDeletingPrefs() {
        val prefs = app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
        prefs.edit().putString("bookmark_url", first)
            .putString("bookmark_position_ms", "corrupted-not-long")
            .putLong("bookmark_duration_ms", 120_000L).commit()
        assertNull(QueueStateStore(app).bookmark(first))
        assertEquals("corrupted-not-long", prefs.getString("bookmark_position_ms", null))
    }

    @Test fun wrongTypedBookmarkUrlNeverCrashesScreenStartup() {
        val prefs = app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
        prefs.edit().putInt("bookmark_url", 18).commit()
        assertNull(QueueStateStore(app).bookmark(first))
        assertEquals(18, prefs.getInt("bookmark_url", 0))
    }

    @Test fun playbackFailureIsScopedToOnlyCurrentPublicVideo() {
        store.recordFailure(first, "Unavailable public stream")
        assertEquals("Unavailable public stream", QueueStateStore(app).failure(first))
        assertNull(store.failure(next))
        store.clearFailure()
        assertNull(store.failure(first))
    }

    @Test fun streamFailureNeverStoresPrivateSignedAudioUrl() {
        val privateAudio = "https://rr.example.googlevideo.com/audio?sig=private"
        store.recordFailure(privateAudio, "Do not store")
        assertNull(store.failure(privateAudio))
        assertFalse(
            app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE).all.values
                .any { it.toString().contains("sig=private") }
        )
    }

    @Test fun savedQueueAndPlaybackBookmarkSurviveNewStoreInstance() {
        val queue = PlaybackQueue()
        queue.replace(
            listOf(
                QueueEntry(first, "First", "Artist", "https://example.org/image1"),
                QueueEntry(next, "Second", "Artist", "https://example.org/image2")
            ), 1
        )
        store.save(queue)
        store.saveBookmark(next, 82_000L, 204_000L)
        val restored = QueueStateStore(app)
        assertEquals(1, restored.read().index)
        assertEquals(next, restored.read().tracks[1].url)
        assertEquals(82_000L, restored.bookmark(next)?.positionMs)
        assertEquals(204_000L, restored.bookmark(next)?.durationMs)
    }

    @Test fun bookmarkNeverLeaksToDifferentQueueSong() {
        store.saveBookmark(first, 30_000L, 120_000L)
        assertNull(store.bookmark(next))
        assertEquals(30_000L, store.bookmark(first)?.positionMs)
    }

    @Test fun finishedTrackRestoresFromStart() {
        store.saveBookmark(first, 119_100L, 120_000L)
        assertEquals(0L, store.bookmark(first)?.positionMs)
    }

    @Test fun bookmarkNeverStoresStreamingSignedLinks() {
        store.saveBookmark("https://rr1---sn.googlevideo.com/audio?sig=secret",
            34_000L, 200_000L)
        assertNull(store.bookmark(first))
        assertFalse(
            app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
                .all.values.any { it.toString().contains("sig=secret") }
        )
    }

    @Test fun maliciousNegativeAndHugePositionsAreClamped() {
        store.saveBookmark(first, -10L, Long.MAX_VALUE)
        assertEquals(0L, store.bookmark(first)?.positionMs)
        store.saveBookmark(first, Long.MAX_VALUE, 0L)
        assertEquals(86_400_000L, store.bookmark(first)?.positionMs)
    }
    private fun storedQueue(urls: List<String>, index: Int, selectedUrl: String? = null): String {
        val tracks = JSONArray()
        urls.forEach { url ->
            tracks.put(JSONObject().put("url", url).put("title", "Saved song")
                .put("creator", "Saved artist").put("artwork", ""))
        }
        val state = JSONObject().put("tracks", tracks).put("index", index)
            .put("shuffle", false).put("repeat", "OFF")
        if (selectedUrl != null) state.put("selected_url", selectedUrl)
        return state.toString()
    }

    @Test fun legacySnapshotKeepsSelectedSongAfterInvalidRowIsRemoved() {
        val prefs = app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
        val original = storedQueue(listOf(first,
            "https://www.youtube.com/watch?v=invalid", next), 2)
        assertTrue(prefs.edit().putString("snapshot", original).commit())

        val recovered = store.read()
        assertEquals(2, recovered.tracks.size)
        assertEquals(1, recovered.index)
        assertEquals(next, recovered.tracks[recovered.index].url)
        val queue = PlaybackQueue()
        store.restore(queue)
        assertEquals(next, queue.current?.url)
        // Repair on read is intentionally non-destructive.
        assertEquals(original, prefs.getString("snapshot", null))
    }

    @Test fun mismatchedSavedSelectionFailsClosedWithoutDeletingSnapshot() {
        val prefs = app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
        val original = storedQueue(listOf(first, next), 0, next)
        assertTrue(prefs.edit().putString("snapshot", original).commit())

        assertTrue(store.read().tracks.isEmpty())
        val queue = PlaybackQueue()
        store.restore(queue)
        assertNull(queue.current)
        assertEquals(original, prefs.getString("snapshot", null))
    }

    @Test fun modernSnapshotSavesExplicitSongIdentity() {
        val prefs = app.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
        val queue = PlaybackQueue()
        queue.replace(listOf(
            QueueEntry(first, "First", "Artist", ""),
            QueueEntry(next, "Second", "Artist", "")), 1)
        store.save(queue)
        val saved = JSONObject(prefs.getString("snapshot", "{}")!!)
        assertEquals(next, saved.getString("selected_url"))
        assertEquals(next, QueueStateStore(app).read().tracks[1].url)
    }

}
