package com.nightwave.player.playback

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Only test-suffixed private preferences are touched.
 * No installed music, bookmarks, online accounts or media service are modified.
 */
@RunWith(AndroidJUnit4::class)
class QueuePlayNextPersistenceInstrumentedTest {
    private fun song(i: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
        "Track $i", "Artist $i", ""
    )

    private class PrivateTestContext(base: Context, val suffix: String) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences(name + suffix, mode)
    }

    @Test fun explicitPlayNextSurvivesServiceRecreationWithShuffleEnabled() {
        val base: Context = ApplicationProvider.getApplicationContext()
        val isolated = PrivateTestContext(base, "_nw43_" + UUID.randomUUID())
        val prefs = isolated.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
        try {
            val store = QueueStateStore(isolated)
            val before = PlaybackQueue(pick = { 0 })
            before.replace((1..4).map(::song), 0)
            before.shuffle = true
            assertTrue(before.insertNext(song(4)))
            assertTrue(before.insertNext(song(3)))
            store.save(before)

            val serialized = prefs.getString("snapshot", null)
            assertNotNull(serialized)
            assertEquals(2, JSONObject(serialized!!).getJSONArray("manual_next").length())

            val restored = PlaybackQueue(pick = { 0 })
            store.restore(restored)
            assertTrue(restored.shuffle)
            assertEquals(song(1), restored.current)
            assertEquals(song(3), restored.next())
            assertEquals(song(4), restored.next())
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun corruptPriorityCannotInjectUnknownSongOrChangeSelectedSong() {
        val base: Context = ApplicationProvider.getApplicationContext()
        val isolated = PrivateTestContext(base, "_nw43_" + UUID.randomUUID())
        val prefs = isolated.getSharedPreferences("nightwave_queue_v1", Context.MODE_PRIVATE)
        try {
            val store = QueueStateStore(isolated)
            val original = PlaybackQueue()
            original.replace((1..3).map(::song), 0)
            store.save(original)

            val stored = JSONObject(prefs.getString("snapshot", "{}")!!)
            stored.put("manual_next", org.json.JSONArray()
                .put("file:///private/unsafe.mp3")
                .put(song(3).url)
                .put(song(3).url))
            assertTrue(prefs.edit().putString("snapshot", stored.toString()).commit())

            val restored = PlaybackQueue()
            store.restore(restored)
            assertEquals(song(1), restored.current)
            assertEquals(listOf(song(3).url), restored.priorityNextUrls())
            assertEquals(song(3), restored.next())
            assertEquals(3, restored.size)
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
