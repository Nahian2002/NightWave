package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.recommendation.PersonalMix
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Deferred UI tests. Never open a player, network connection or saved library. */
@RunWith(AndroidJUnit4::class)
class NightWaveMixDensityInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun mix() = PersonalMix(
        key = "daily", title = "Your daily rotation",
        description = "Music you know", reason = "Based on your saved songs",
        tracks = listOf(VideoPreview(
            "Song One", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 60L
        ))
    )

    @Test fun compactSingleMixOpensPreviewWithoutPlaying() {
        var clicked = 0
        compose.setContent {
            MaterialTheme {
                NightWaveMixCard(mix(), selected = false,
                    compact = true, modifier = Modifier.width(300.dp),
                    onClick = { clicked++ })
            }
        }
        compose.onNodeWithText("Your daily rotation").performClick()
        compose.runOnIdle { assertEquals(1, clicked) }
    }

    @Test fun selectedMixExplainsHowToCollapse() {
        compose.setContent {
            MaterialTheme {
                NightWaveMixCard(mix(), selected = true,
                    compact = false, modifier = Modifier.width(170.dp),
                    onClick = {})
            }
        }
        compose.onNodeWithText("1 track · Collapse").assertExists()
    }
}
