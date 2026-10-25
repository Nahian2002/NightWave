package com.nightwave.player.v2.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Isolated private preference file so instrumentation never clears the user's
 * actual saved searches, playlists, offline files, or cloud settings.
 */
@RunWith(AndroidJUnit4::class)
class SearchHistoryStoreInstrumentedTest {
    private class TestContext(base: Context, private val suffix: String) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences(name + suffix, mode)
    }

    @Test fun malformedHistoryNeverGetsReplacedByAnOrdinarySearch() {
        val base: Context = ApplicationProvider.getApplicationContext()
        val suffix = "_nw40_test_" + UUID.randomUUID()
        val isolated = TestContext(base, suffix)
        val preferences = isolated.getSharedPreferences(
            "nightwave_search_history_v1", Context.MODE_PRIVATE)
        try {
            val malformed = "{ this is not a JSON array"
            assertTrue(preferences.edit().putString("submitted_queries", malformed).commit())
            val store = SearchHistoryStore(isolated)
            assertTrue(store.read().isEmpty()) // Search remains open despite corruption.
            store.record("Important Music")
            store.remove("Old Search")
            store.clear()
            store.restoreIfUnchanged(emptyList(), listOf("Old Search"))
            assertEquals(malformed, preferences.getString("submitted_queries", null))
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test fun legitimateHistoryCanStillRecordRemoveAndUndo() {
        val base: Context = ApplicationProvider.getApplicationContext()
        val suffix = "_nw40_test_" + UUID.randomUUID()
        val isolated = TestContext(base, suffix)
        val preferences = isolated.getSharedPreferences(
            "nightwave_search_history_v1", Context.MODE_PRIVATE)
        try {
            val store = SearchHistoryStore(isolated)
            assertEquals(listOf("Blue Moon"), store.record(" Blue   Moon "))
            assertEquals(listOf("Night Drive", "Blue Moon"), store.record("Night Drive"))
            val old = store.read()
            val removed = store.remove("BLUE MOON")
            assertEquals(listOf("Night Drive"), removed)
            assertEquals(old, store.restoreIfUnchanged(removed, old))
            store.record("Newer search")
            assertEquals(store.read(), store.restoreIfUnchanged(removed, old))
            assertEquals(emptyList<String>(), store.clear())
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
