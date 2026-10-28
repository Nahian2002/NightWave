package com.nightwave.player.v2.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.*
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Search screen, synthetic callbacks only; no playback, account or network service. */
@RunWith(AndroidJUnit4::class)
class NightWavePublicSearchInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private var state by mutableStateOf(StudioSearchSnapshot())
    private val played = mutableListOf<VideoPreview>()
    private var cancellations = 0
    private var retries = 0
    private var web = ""
    private val first = VideoPreview("Play Date (Official Audio)", "Fixture uploader", "https://www.youtube.com/watch?v=00000000001", "", 215)
    private val second = first.copy(url = "https://www.youtube.com/watch?v=00000000002")

    private fun screen() {
        compose.setContent {
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                StudioSearch(Modifier, state, onPrimarySearch = { _, _ -> },
                    onNativeSearch = { _, _ -> retries++ }, onWebSearch = { web = it },
                    onPlay = { song, queue -> assertEquals(listOf(song), queue); played.add(song) },
                    onSongOptions = {}, playingUrl = "", corpus = LocalMusicCorpusState(0, 0, failed = true),
                    onRetryCorpus = {}, onOfflineSearch = {}, searchHistory = emptyList(),
                    canUndoSearchRemoval = false, onRecordSearch = {}, onRemoveSearch = {},
                    onUndoSearchRemoval = {}, onClearSearch = {}, onCancelSearch = {
                        cancellations++
                        state = state.copy(loading = false, error = "Search cancelled. Retry when you are ready.")
                    })
            }
        }
        compose.waitForIdle()
    }

    @Test fun identicalPlayDateCaptionsExposeSourceEvidenceAndTapExactlyEachChosenUrl() {
        state = StudioSearchSnapshot(query = "play date", videos = listOf(first, second))
        screen()
        for (song in listOf(first, second)) {
            val row = hasTestTag("nightwave-track-search") and hasText("Video ID: " + song.url.substringAfter("v="))
            compose.onNodeWithTag("nw129-search-list").performScrollToNode(row)
            compose.onNode(row).assert(hasText("YouTube video · 3:35"))
                .assert(hasText("Uploader: Fixture uploader")).assert(hasText("Title labels: Official audio")).performClick()
        }
        compose.runOnIdle { assertEquals(listOf(first.url, second.url), played.map { it.url }) }
    }

    @Test fun currentPhaseCancelRetryAndExplicitWebFallbackAreReachable() {
        state = StudioSearchSnapshot(query = "play date", loading = true, phase = PublicSearchPhase.COMMUNITY)
        screen()
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(hasTestTag("nw130-cancel-search"))
        compose.onNodeWithText(PublicSearchPhase.COMMUNITY.label).assertIsDisplayed()
        compose.onNodeWithTag("nw130-cancel-search").performClick()
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(hasText("Retry search"))
        compose.onNodeWithText("Retry search").performClick()
        compose.onNodeWithText("YouTube", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(1, cancellations); assertEquals(1, retries); assertEquals("play date", web) }
    }

    @Test fun typingDuringSlowRequestCancelsItAndOldRowsCannotBecomeCurrent() {
        state = StudioSearchSnapshot(query = "play date", loading = true, videos = listOf(first, second))
        screen()
        compose.onNodeWithTag("nw129-search-query").performScrollTo().performTextReplacement("new music")
        compose.runOnIdle { assertEquals(1, cancellations) }
        compose.onNode(hasTestTag("nightwave-track-search") and hasText("Play Date")).assertDoesNotExist()
    }
}
