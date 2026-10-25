package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** NW47 presentation-only tests; no personal library, service, Drive or network. */
@RunWith(AndroidJUnit4::class)
class NightWaveEverydaySurfacesInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun quickActionHasARealTapOnCompactPhone() {
        var opens = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWaveQuickActionPill(
                        "Offline", Icons.Outlined.LibraryMusic, "nw47-test-offline",
                        onClick = { opens++ }
                    )
                }
            }
        }
        compose.onNodeWithTag("nw47-test-offline").assertExists().performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun recentlyPlayedActionsRemainIndependentFromPlayback() {
        val example = VideoPreview(
            title = "Example song",
            author = "Example creator",
            url = "https://www.youtube.com/watch?v=abcdefghijk",
            thumbnail = "",
            durationSeconds = 180L
        )
        var plays = 0
        var options = 0
        var removals = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWaveRecentShelfTile(
                        song = example, isPlaying = false,
                        onPlay = { plays++ }, onOptions = { options++ },
                        onRemove = { removals++ }
                    )
                }
            }
        }
        compose.onNodeWithText("Example song", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Example creator", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("nw47-recent-play").assertExists().performClick()
        compose.onNodeWithTag("nw47-recent-options").assertExists().performClick()
        compose.onNodeWithTag("nw47-recent-remove").assertExists().performClick()
        compose.runOnIdle {
            assertEquals(1, plays)
            assertEquals(1, options)
            assertEquals(1, removals)
        }
    }
}
