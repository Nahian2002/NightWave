package com.nightwave.player.v2.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.playback.StudioPlayerState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Actual sheet, synthetic queue and callbacks; no engine/account or private data. */
class NW142QueueViewportInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun largeTextSummaryCanScrollAwayToReachEveryQueueRow() {
        val songs = (1..8).map { i -> VideoPreview("দীর্ঘ বাংলা mixed title ".repeat(6) + i,
            "Fixture creator", "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'), "", 120L) }
        var chosen = -1
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 3f)) {
                NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                    StudioQueueSheet(StudioPlayerState(connected = true, videoUrl = songs[0].url,
                        queue = songs, queueIndex = 0), {}, { chosen = it }, { _, _ -> null }, {}, true,
                        onClearUpcoming = { true })
                }
            }
        }
        compose.onNodeWithTag("nw142-queue-content").performScrollToNode(hasTestTag("nightwave-upcoming-8"))
        compose.onNodeWithTag("nightwave-upcoming-8").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(7, chosen) }
        compose.onNodeWithTag("nw142-queue-content").performScrollToNode(hasTestTag("nw134-clear-upcoming"))
        compose.onNodeWithTag("nw134-clear-upcoming").assertIsDisplayed().performClick()
        compose.onNodeWithTag("nw134-confirm-clear").assertExists()
    }
}
