package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable Compose-only surfaces. No Drive, playback service or private files. */
@RunWith(AndroidJUnit4::class)
class NightWaveSearchLibraryChromeInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactSearchHeaderFitsWithoutImaginaryArtwork() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWaveDiscoveryHeader()
                }
            }
        }
        compose.onNodeWithTag("nightwave-discovery-header").assertExists()
        compose.onNodeWithText("Search").assertExists()
    }

    @Test fun libraryHeaderHandlesLongCollectionNames() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWaveCollectionHeader(
                        name = "A very long playlist name ".repeat(15),
                        description = "42 saved tracks"
                    )
                }
            }
        }
        compose.onNodeWithTag("nightwave-collection-header").assertExists()
        compose.onNodeWithText("42 saved tracks").assertExists()
    }

    @Test fun savedCollectionRowKeepsOriginalTapAction() {
        var tapped = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    MusicCollectionTile(
                        name = "Liked songs",
                        subtitle = "7 tracks",
                        cover = null,
                        onClick = { tapped++ }
                    )
                }
            }
        }
        compose.onNodeWithTag("nightwave-library-collection").assertExists()
            .performClick()
        compose.runOnIdle { assertEquals(1, tapped) }
    }
}
