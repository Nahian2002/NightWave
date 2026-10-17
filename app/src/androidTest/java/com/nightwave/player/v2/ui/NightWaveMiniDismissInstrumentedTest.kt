package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.playback.StudioPlayerActions
import com.nightwave.player.v2.playback.StudioPlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI-only test. No Media3 session, user data, credentials or network. */
@RunWith(AndroidJUnit4::class)
class NightWaveMiniDismissInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val first = VideoPreview("One", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 100L)
    private val second = VideoPreview("Two", "Artist", "https://www.youtube.com/watch?v=9bZkp7q19f0", "", 100L)
    private class Actions : StudioPlayerActions {
        var mutations = 0
        override fun play(video: VideoPreview, collection: List<VideoPreview>) { mutations++ }
        override fun playPause() { mutations++ }
        override fun retry() { mutations++ }
        override fun enqueue(video: VideoPreview) { mutations++ }
        override fun previous() { mutations++ }
        override fun next() { mutations++ }
        override fun seekTo(positionMs: Long) { mutations++ }
        override fun toggleShuffle() { mutations++ }
        override fun cycleRepeat() { mutations++ }
        override fun jumpTo(index: Int) { mutations++ }
        override fun removeUpcoming(index: Int, expectedUrl: String): String? {
            mutations++
            return null
        }
        override fun undoUpcoming(token: String) { mutations++ }
        override fun refreshQueue() { mutations++ }
    }

    @Test fun closeHidesMiniPlayerWithoutTouchingPlayback() {
        val actions = Actions()
        var hidden by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                if (!hidden) StudioMiniPlayer(
                    state = StudioPlayerState(connected = true,
                        title = first.title, artist = first.author,
                        videoUrl = first.url, playing = true,
                        queue = listOf(first, second), queueIndex = 0),
                    actions = actions,
                    onExpand = {},
                    onHide = { hidden = true }
                )
            }
        }
        compose.onNodeWithContentDescription("Hide mini-player; music keeps playing")
            .assertExists().performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Hide mini-player; music keeps playing")
            .assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(hidden)
            assertEquals(0, actions.mutations)
        }
    }

    @Test fun narrowMiniPlayerRetainsCloseButMovesNextToFullPlayer() {
        val actions = Actions()
        compose.setContent {
            MaterialTheme {
                StudioMiniPlayer(
                    state = StudioPlayerState(connected = true,
                        title = first.title, artist = first.author,
                        videoUrl = first.url, playing = false,
                        queue = listOf(first, second), queueIndex = 0),
                    actions = actions, onExpand = {}, onHide = {},
                    modifier = Modifier.width(320.dp)
                )
            }
        }
        compose.onNodeWithContentDescription("Hide mini-player; music keeps playing").assertExists()
        compose.onNodeWithContentDescription("Pause playback").assertDoesNotExist()
        compose.onNodeWithContentDescription("Resume playback").assertExists()
        compose.onNodeWithContentDescription("Next track").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, actions.mutations) }
    }
}
