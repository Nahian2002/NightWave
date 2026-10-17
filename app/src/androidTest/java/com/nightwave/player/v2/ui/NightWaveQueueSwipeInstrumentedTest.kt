package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.playback.StudioPlayerState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs in a disposable emulator/profile with a fabricated queue.
 * No Media3 playback, stored playlists, app-private audio or files are changed.
 */
@RunWith(AndroidJUnit4::class)
class NightWaveQueueSwipeInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun track(index: Int) = VideoPreview(
        "Track $index", "Artist $index",
        "https://www.youtube.com/watch?v=queue$index", "", 0L
    )

    private fun setupSheet() {
        compose.setContent {
            var items by remember { mutableStateOf(listOf(track(0), track(1), track(2))) }
            var removed by remember { mutableStateOf<Pair<Int, VideoPreview>?>(null) }
            MaterialTheme {
                StudioQueueSheet(
                    state = StudioPlayerState(
                        connected = true, videoUrl = track(0).url,
                        queue = items, queueIndex = 0, playing = true
                    ),
                    onDismiss = {},
                    onSelect = {},
                    onRemoveUpcoming = { index, url ->
                        if (index <= 0 || items.getOrNull(index)?.url != url) null
                        else {
                            removed = index to items[index]
                            items = items.toMutableList().also { it.removeAt(index) }
                            "synthetic-undo-token"
                        }
                    },
                    onUndoRemoval = { token ->
                        val pending = removed
                        if (token == "synthetic-undo-token" && pending != null) {
                            items = items.toMutableList().also {
                                it.add(pending.first, pending.second)
                            }
                            removed = null
                        }
                    },
                    reduceMotion = true
                )
            }
        }
        compose.waitForIdle()
    }

    @Test fun accessibleRemoveButtonAndUndoRestoreUpcomingOnly() {
        setupSheet()
        compose.onNodeWithContentDescription("Remove Track 1 from Up Next").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Track 1").assertDoesNotExist()
        compose.onNodeWithText("Track 0").assertExists()
        compose.onNodeWithText("Undo").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Track 1").assertExists()
    }

    @Test fun swipeLeftRemovesUpcomingButNotCurrentlyPlaying() {
        setupSheet()
        compose.onNodeWithContentDescription("Remove Track 0 from Up Next").assertDoesNotExist()
        compose.onNodeWithTag("nightwave-upcoming-2").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Track 1").assertDoesNotExist()
        compose.onNodeWithText("Track 0").assertExists()
    }

    @Test fun duplicatedQueueUrlsUseUniqueComposeRowKeys() {
        val duplicate = track(1)
        compose.setContent {
            MaterialTheme {
                StudioQueueSheet(
                    state = StudioPlayerState(
                        connected = true,
                        videoUrl = track(0).url,
                        queue = listOf(track(0), duplicate, duplicate),
                        queueIndex = 0,
                        playing = true
                    ),
                    onDismiss = {},
                    onSelect = {},
                    onRemoveUpcoming = { _, _ -> null },
                    onUndoRemoval = {},
                    reduceMotion = true
                )
            }
        }
        compose.waitForIdle()
        // Under old URL-based LazyColumn keys, rendering repeated public URLs
        // threw a duplicate key exception before both items appeared.
        compose.onNodeWithTag("nightwave-upcoming-2").assertExists()
        compose.onNodeWithTag("nightwave-upcoming-3").assertExists()
    }
}
