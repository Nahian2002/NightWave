package com.nightwave.player.v2.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightwave.player.net.SearchMode
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.model.NightWaveTheme
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Actual screen -> observed primary preferences -> parser/index -> result tap.
 * Unique isolated preference files only; no real library, audio or service use.
 */
@RunWith(AndroidJUnit4::class)
class NightWaveFullLocalSearchInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val suffix = "nw129-fixture-" + UUID.randomUUID()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val isolated = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            base.getSharedPreferences("$suffix-$name", mode)
    }
    private val public = isolated.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
    private val offline = isolated.getSharedPreferences("nightwave_offline_v1", Context.MODE_PRIVATE)
    private var played: VideoPreview? = null
    private var offlineOpened: String? = null

    @After fun cleanup() {
        // Delete only this test's unique files; never clear the actual stores.
        base.deleteSharedPreferences("$suffix-nightwave_library_v1")
        base.deleteSharedPreferences("$suffix-nightwave_offline_v1")
    }

    private fun row(i: Int, title: String = "Music $i") = JSONObject().put("title", title)
        .put("creator", "Fixture creator").put("url", "https://www.youtube.com/watch?v=${i.toString().padStart(11, '0')}")
        .put("artwork", "").put("duration", 210)

    private fun seed() {
        val editor = public.edit()
        val names = JSONArray()
        repeat(34) { list ->
            val name = "Collection ${list + 1}"; names.put(name)
            val rows = JSONArray()
            for (i in list * 300 until minOf((list + 1) * 300, 10_000)) rows.put(row(i))
            editor.putString("list_$name", rows.toString())
        }
        editor.putString("playlist_names", names.toString())
        editor.putString("favorites", JSONArray().apply { repeat(500) { put(row(it)) } }.toString())
        editor.putString("recent", "[]").commit()
        offline.edit().putString("items", JSONArray().apply {
            repeat(5_000) { put(JSONObject().put("id", it.toString(16).padStart(36, '0') + ".mp3")
                .put("title", "Offline seeker $it").put("mime", "audio/mpeg").put("size", 100)) }
        }.toString()).commit()
    }

    private fun screen() {
        compose.setContent {
            val (corpus, retry) = rememberNightWaveLocalCorpus(isolated, active = true, refresh = 1)
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                StudioSearch(modifier = androidx.compose.ui.Modifier, state = StudioSearchSnapshot(),
                    onPrimarySearch = { _, _ -> }, onNativeSearch = { _, _ -> }, onWebSearch = {},
                    onPlay = { song, queue -> played = song; assertEquals(listOf(song), queue) },
                    onSongOptions = {}, playingUrl = "", corpus = corpus, onRetryCorpus = retry,
                    onOfflineSearch = { offlineOpened = it }, searchHistory = emptyList(),
                    canUndoSearchRemoval = false, onRecordSearch = {}, onRemoveSearch = {},
                    onUndoSearchRemoval = {}, onClearSearch = {})
            }
        }
    }

    private fun type(query: String) {
        compose.onNodeWithTag("nw129-search-query").performScrollTo().performTextReplacement(query)
    }
    private fun ready() {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("nw129-local-loading").fetchSemanticsNodes().isEmpty() }
    }

    @Test fun actualScreenFindsFavoriteAfter350Playlist26AndLastTenThousandthSong() {
        seed(); screen(); ready()
        for (i in listOf(450, 7_799, 9_999)) {
            type("Music $i"); ready()
            val row = hasTestTag("nightwave-track-search") and hasText("Music $i")
            compose.onNodeWithTag("nw129-search-list").performScrollToNode(row)
            compose.onNode(row).performClick()
            compose.runOnIdle { assertEquals(i.toString().padStart(11, '0'), played!!.url.substringAfter("v=")) }
        }
    }

    @Test fun actualScreenFindsOfflineItemAfter400WithoutClaimingFileReadiness() {
        seed(); screen(); ready(); type("Offline seeker 4999"); ready()
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(hasText("♫  Offline seeker 4999"))
        compose.onNodeWithText("♫  Offline seeker 4999").performClick()
        compose.runOnIdle { assertEquals("Offline seeker 4999", offlineOpened) }
    }

    @Test fun observedEditAndRapidQueryChangeCannotReuseOldQueryOrLibraryRows() {
        seed(); screen(); ready(); type("Music 9999"); ready()
        // The target is only in the final playlist, so its removal cannot be
        // masked by a like/history duplicate elsewhere in the fixture.
        val changed = JSONArray(public.getString("list_Collection 34", "[]"))
        changed.getJSONObject(changed.length() - 1).put("title", "Renamed final entry")
        assertTrue(public.edit().putString("list_Collection 34", changed.toString()).commit())
        type("Music 450"); type("Renamed final entry"); ready()
        val row = hasTestTag("nightwave-track-search") and hasText("Renamed final entry")
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(row)
        compose.onNode(row).performClick()
        compose.runOnIdle { assertEquals("Renamed final entry", played!!.title) }
        compose.onNode(hasTestTag("nightwave-track-search") and hasText("Music 9999")).assertDoesNotExist()
    }
}
