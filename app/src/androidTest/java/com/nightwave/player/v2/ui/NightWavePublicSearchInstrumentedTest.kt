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
    private var requested = ""
    private var web = ""
    private val first = VideoPreview("Play Date (Official Audio)", "Fixture uploader", "https://www.youtube.com/watch?v=00000000001", "", 215)
    private val second = first.copy(url = "https://www.youtube.com/watch?v=00000000002")

    private fun screen(history: List<String> = emptyList()) {
        if (state.query.isNotBlank()) state = state.copy(prefillToken = "nw150-fixture")
        compose.setContent {
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                StudioSearch(Modifier, state, onPrimarySearch = { _, _ -> },
                    onNativeSearch = { query, _ -> requested = query; retries++ }, onWebSearch = { web = it },
                    onPlay = { song, queue -> assertEquals(listOf(song), queue); played.add(song) },
                    onSongOptions = {}, playingUrl = "", corpus = LocalMusicCorpusState(0, 0, failed = true),
                    onRetryCorpus = {}, onOfflineSearch = { _, _ -> }, searchHistory = history,
                    canUndoSearchRemoval = false, onRecordSearch = {}, onRemoveSearch = {},
                    onUndoSearchRemoval = {}, onClearSearch = {}, onCancelSearch = {
                        cancellations++
                        state = state.stop(SearchStopReason.CANCELLED)
                    })
            }
        }
        compose.waitForIdle()
    }

    @Test fun submitClearsInputFocusWithoutDiscardingTheDraft() {
        screen()
        compose.onNodeWithTag("nw129-search-query").performScrollTo().performTextReplacement("focus fixture")
        compose.onNodeWithText("Find music").performScrollTo().performClick()
        compose.onNodeWithTag("nw129-search-query").assertIsNotFocused().assertTextContains("focus fixture")
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun localFilterKeepsOneDraftWithoutInvokingPublicSearch() {
        screen()
        val field = compose.onNodeWithTag("nw129-search-query")
        field.performScrollTo().performTextReplacement("shared draft")
        compose.onNodeWithTag("nw57-scope-LOCAL").performScrollTo().performClick()
        field.assertTextContains("shared draft")
        compose.onNodeWithText("Find music").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, retries) }
        compose.onNodeWithTag("nw57-scope-MUSIC").performScrollTo().performClick()
        field.assertTextContains("shared draft")
        compose.runOnIdle { assertEquals(0, retries) }
    }

    @Test fun sourceFiltersAreGroupedAndKeepTheQuery() {
        screen()
        val field = compose.onNodeWithTag("nw129-search-query")
        field.performScrollTo().performTextReplacement("piano")
        compose.onNodeWithTag("nw57-scope-OPENVERSE").assertDoesNotExist()
        compose.onNodeWithTag("nw58-sources-tab").performScrollTo().performClick()
        compose.onNodeWithTag("nw57-scope-ALL").performScrollTo().assertIsSelected()
        for (provider in listOf("AUDIUS", "PEERTUBE", "RADIO", "OPENVERSE")) {
            compose.onNodeWithTag("nw57-scope-$provider").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("nw57-scope-OPENVERSE").performClick().assertIsSelected()
        field.assertTextContains("piano")
        compose.runOnIdle { assertEquals(0, retries) }
    }

    @Test fun recentCompletionDoesNotSubmitUntilTapped() {
        screen(history = listOf("Blue Hour", "Red Moon"))
        val field = compose.onNodeWithTag("nw129-search-query")
        field.performScrollTo().performClick().performTextReplacement("Blu")
        compose.onNodeWithText("Matching recent searches").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, retries) }
        compose.onNodeWithText("Blue Hour").performClick()
        field.assertIsNotFocused().assertTextContains("Blue Hour")
        compose.runOnIdle { assertEquals(1, retries); assertEquals("Blue Hour", requested) }
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
        compose.onNodeWithTag("nw150-search-web").performClick()
        compose.runOnIdle { assertEquals(1, cancellations); assertEquals(1, retries); assertEquals("play date", web) }
    }

    @Test fun typingDuringSlowRequestCancelsItAndOldRowsCannotBecomeCurrent() {
        state = StudioSearchSnapshot(query = "play date", loading = true, videos = listOf(first, second))
        screen()
        compose.onNodeWithTag("nw129-search-query").performScrollTo().performTextReplacement("new music")
        compose.runOnIdle { assertEquals(1, cancellations) }
        compose.onNode(hasTestTag("nightwave-track-search") and hasText("Play Date")).assertDoesNotExist()
    }
    @Test fun matchingRecentRowsStaySelectableWhileRefreshIsLoading() {
        state = StudioSearchSnapshot(query = "play date", loading = true,
            videos = listOf(first, second), fromCache = true)
        screen()
        val row = hasTestTag("nightwave-track-search") and hasText("Video ID: 00000000001")
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(row)
        compose.onNode(row).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf(first.url), played.map { it.url }) }
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(hasTestTag("nw130-cancel-search"))
        compose.onNodeWithTag("nw130-cancel-search").assertIsDisplayed()
    }
    @Test fun recentResultsHaveExplicitRefreshAndOptionalFrozenDetails() {
        val details = "Elapsed 120 ms · Recent results\nHTTP calls 0"
        state = StudioSearchSnapshot(query = "play date", videos = listOf(first, second),
            fromCache = true, details = details)
        screen()
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(hasTestTag("search02-refresh"))
        compose.onNodeWithTag("search02-refresh").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("Search details").performClick()
        compose.onNodeWithText(details).assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
    }
}
