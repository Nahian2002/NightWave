package com.nightwave.player.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
}
