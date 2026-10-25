package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.playback.StudioPlayerState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Visual queue contracts with fake metadata; never starts a service or writes audio. */
@RunWith(AndroidJUnit4::class)
class NightWaveQueueSectionsInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private fun song(i: Int) = VideoPreview("NW43 Song $i", "Artist $i",
        "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
        "", 180L)

    private fun render(tracks: List<VideoPreview>, selected: Int, shuffled: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                StudioQueueSheet(
                    state = StudioPlayerState(
                        connected = true,
                        queue = tracks, queueIndex = selected,
                        videoUrl = tracks.getOrNull(selected)?.url.orEmpty(),
                        shuffle = shuffled, playing = true
                    ),
                    onDismiss = {}, onSelect = {},
                    onRemoveUpcoming = { _, _ -> null },
                    onUndoRemoval = {}, reduceMotion = true
                )
            }
        }
        compose.waitForIdle()
    }

    @Test fun singleSearchSongCannotDisplayPhantomUpcomingItems() {
        render(listOf(song(1)), 0)
        compose.onNodeWithText("PLAYING NOW").assertExists()
        compose.onNodeWithText("NW43 Song 1").assertExists()
        compose.onNodeWithText("Nothing else in your queue.").assertExists()
        compose.onNodeWithTag("nightwave-upcoming-2").assertDoesNotExist()
    }

    @Test fun playlistQueueShowsCurrentAndRealNext() {
        render(listOf(song(1), song(2)), 0)
        compose.onNodeWithText("UP NEXT").assertExists()
        compose.onNodeWithTag("nightwave-upcoming-2").assertExists()
    }

    @Test fun shufflePoolIsNeverClaimedToBeOrderedUpNext() {
        render(listOf(song(1), song(2)), 0, shuffled = true)
        compose.onNodeWithText("SHUFFLE POOL · ORDER VARIES").assertExists()
        compose.onNodeWithText("UP NEXT").assertDoesNotExist()
    }
}
