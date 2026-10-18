package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import com.nightwave.player.v2.lyrics.LyricsEditorDraft
import com.nightwave.player.v2.lyrics.LyricsEditorPolicy
import com.nightwave.player.v2.lyrics.TimedLyricsCodec
import org.junit.Assert.assertEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Test

/**
 * Runs on a disposable test-only Compose Activity; doesn't open an MP3,
 * access Google, write private LRC, or interact with personal installations.
 */
@RunWith(AndroidJUnit4::class)
class NightWaveLyricsSearchUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val baseline: LyricsEditorDraft = LyricsEditorPolicy.begin(
        TimedLyricsCodec.parse(
            ("[00:01.000]Hello first\n[00:02.000]Another line\n" +
                "[00:03.000]HELLO last").toByteArray(Charsets.UTF_8)
        )
    )

    private fun render() {
        compose.setContent {
            MaterialTheme {
                NightWaveLyricsEditorDialog(
                    songId = "test-offline-song.mp3",
                    songTitle = "Test lyric",
                    draft = baseline,
                    dirty = false,
                    canUndo = false,
                    saving = false,
                    playbackMs = 0L,
                    offsetMs = 0,
                    reduceMotion = true,
                    onApply = {},
                    onUndo = {},
                    onSave = {},
                    onExport = {},
                    onDiscard = {}
                )
            }
        }
    }

    @Test fun searchingShowsMatchesWithoutTouchingStoredLyrics() {
        render()
        compose.onNodeWithTag("lyrics-search-toggle").performClick()
        compose.onNodeWithTag("lyrics-search-input").performTextInput("hello")
        compose.onNodeWithTag("lyrics-search-status").assertTextContains("Match 1 of 2")
        compose.onNodeWithTag("lyrics-match-next").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("lyrics-search-status").assertTextContains("Match 2 of 2")
        assertEquals("Hello first", baseline.cues.first().text)
        assertEquals("HELLO last", baseline.cues.last().text)
    }

    @Test fun noMatchesDisablePreviousAndNext() {
        render()
        compose.onNodeWithTag("lyrics-search-toggle").performClick()
        compose.onNodeWithTag("lyrics-search-input").performTextInput("missing")
        compose.onNodeWithTag("lyrics-search-status")
            .assertTextContains("No matching lyric lines")
        compose.onNodeWithTag("lyrics-match-next").assertIsNotEnabled()
        compose.onNodeWithTag("lyrics-match-previous").assertIsNotEnabled()
    }

    @Test fun clearingSearchHidesMatchResults() {
        render()
        compose.onNodeWithTag("lyrics-search-toggle").performClick()
        compose.onNodeWithTag("lyrics-search-input").performTextInput("hello")
        compose.onNodeWithTag("lyrics-search-input").performTextClearance()
        compose.onNodeWithTag("lyrics-search-status").assertDoesNotExist()
        assertEquals(3, baseline.cues.size)
    }
}
