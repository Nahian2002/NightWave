package com.nightwave.player.v2.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.insights.ListeningMemorySummary
import com.nightwave.player.v2.model.ProfileSettings
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Home composable with synthetic callbacks; never starts a player or changes saved data. */
@RunWith(AndroidJUnit4::class)
class NightWavePurposefulHomeInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private var opened = ""
    private var played: List<VideoPreview> = emptyList()
    private var resumed: List<VideoPreview> = emptyList()
    private fun song(n: Int) = VideoPreview("Fixture song $n", "Fixture uploader $n",
        "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'), "", 60L)
    private fun screen(recent: List<VideoPreview> = emptyList(), liked: List<VideoPreview> = emptyList(),
        playlists: List<VideoPreview> = emptyList()) {
        compose.setContent {
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                StudioHome(Modifier, ProfileSettings("fixture"), recent, liked, playlists,
                    ListeningMemorySummary(false, 0L, 0, 0, emptyList()), "",
                    onSearch = { opened = it }, onResumeSpotlight = { first, queue ->
                        assertSame(queue.first(), first); resumed = queue
                    }, onPlay = { first, queue -> assertTrue(first in queue); played = queue },
                    onSongOptions = {}, onRemoveRecent = {}, onClearRecent = {},
                    openRecentHistory = { opened = "history" }, openLibrary = { opened = "library" },
                    openLikedSongs = { opened = "liked" }, openOffline = { opened = "offline" },
                    openSearch = { opened = "search" })
            }
        }
    }
    @Test fun newListenerHasSingleFindMusicActionAndNoEmptyHistoryDashboard() {
        screen()
        compose.onNodeWithTag("nightwave-home-hero").performClick()
        compose.runOnIdle { assertEquals("search", opened); assertTrue(played.isEmpty()) }
        compose.onNodeWithText("Recently played").assertDoesNotExist()
        compose.onNodeWithTag("nw117-recent-empty-state").assertDoesNotExist()
        compose.onNodeWithTag("nw47-home-search").assertDoesNotExist()
    }
    @Test fun likedOnlyPrimaryOpensRealCollection() {
        screen(liked = listOf(song(1)))
        compose.onNodeWithTag("nightwave-home-hero").performClick()
        compose.runOnIdle { assertEquals("liked", opened); assertTrue(played.isEmpty()) }
        compose.onNodeWithTag("nw47-home-liked").assertDoesNotExist()
    }
    @Test fun playlistOnlyPrimaryOpensLibrary() {
        screen(playlists = listOf(song(1)))
        compose.onNodeWithTag("nightwave-home-hero").performClick()
        compose.runOnIdle { assertEquals("library", opened) }
    }
    @Test fun historyOnlyContinueUsesOriginalBoundedQueueWithoutMutatingHistory() {
        val history = (1..5000).map { song((it - 1) % 150 + 1) }
        val before = history.toList()
        screen(recent = history)
        compose.onNodeWithTag("nightwave-home-hero").performClick()
        compose.runOnIdle {
            assertEquals(100, resumed.size)
            assertSame(history.first(), resumed.first())
            assertEquals(before, history)
            assertTrue(played.isEmpty())
        }
    }
    @Test fun mixOpensPreviewBeforeExplicitPlayAndKeepsOriginalReferences() {
        val songs = (1..36).map(::song)
        screen(liked = songs)
        compose.onNodeWithTag("nw131-home-list").performScrollToNode(hasTestTag("nw131-mix-daily_mix_1"))
        compose.onNodeWithTag("nw131-mix-daily_mix_1").performClick()
        compose.runOnIdle { assertTrue(played.isEmpty()) }
        compose.onNodeWithTag("nw131-home-list").performScrollToNode(hasTestTag("nw131-play-mix"))
        compose.onNodeWithTag("nw131-play-mix").performClick()
        compose.runOnIdle {
            assertTrue(played.isNotEmpty())
            assertTrue(played.size <= 12)
            assertTrue(played.all { candidate -> songs.any { it === candidate } })
        }
    }
    @Test fun moodShortcutOnlyOpensSearch() {
        screen()
        compose.onNodeWithTag("nw131-home-list").performScrollToNode(hasText("After hours"))
        compose.onNodeWithText("After hours").performClick()
        compose.runOnIdle { assertTrue(opened.isNotBlank()); assertTrue(played.isEmpty()); assertTrue(resumed.isEmpty()) }
    }
}
