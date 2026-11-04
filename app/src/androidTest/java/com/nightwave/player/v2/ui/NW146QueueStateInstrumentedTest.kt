package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nightwave.player.R
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.playback.StudioPlayerState
import org.junit.Rule
import org.junit.Test

/** Isolated view contract: no player/service/network/library mutation. */
class NW146QueueStateInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun currentRowAnnouncesActualPhaseThroughPauseBufferingAndDisconnect() {
        val song = VideoPreview("Fixture", "Fixture creator", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 100L)
        val state = mutableStateOf(StudioPlayerState(connected = true, title = song.title,
            videoUrl = song.url, playing = false, queue = listOf(song), queueIndex = 0))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent { MaterialTheme {
            StudioQueueSheet(state.value, {}, {}, { _, _ -> null }, {}, true)
        } }
        fun phase(resource: Int) = compose.onNodeWithTag("nightwave-upcoming-1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, context.getString(resource)))
        phase(R.string.nw141_phase_paused)
        compose.runOnIdle { state.value = state.value.copy(playing = true) }
        phase(R.string.nw141_phase_playing)
        compose.runOnIdle { state.value = state.value.copy(buffering = true) }
        phase(R.string.nw141_phase_buffering)
        compose.runOnIdle { state.value = state.value.copy(connected = false) }
        phase(R.string.nw141_phase_disconnected)
    }
}
