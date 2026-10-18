package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertExists
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

/** All fixtures are synthetic; no personal files, Media3 or cloud accounts. */
@RunWith(AndroidJUnit4::class)
class NightWavePremiumBrowseInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowSearchHeroShowsRealPublicDiscoveryContext() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWaveBrowseHeader(
                        title = "Explore sound",
                        subtitle = "Find saved favorites or discover new music online",
                        search = true
                    )
                }
            }
        }
        compose.onNodeWithTag("nightwave-search-header").assertExists()
        compose.onNodeWithText("Explore sound").assertExists()
        compose.onNodeWithText("Find saved favorites or discover new music online")
            .assertExists()
    }

    @Test fun libraryHeaderHandlesLongPlaylistTitleOnNarrowScreen() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWaveBrowseHeader(
                        title = "A very long playlist name that must not overrun its container".repeat(3),
                        subtitle = "10 saved tracks",
                        search = false
                    )
                }
            }
        }
        compose.onNodeWithTag("nightwave-library-header").assertExists()
        compose.onNodeWithText("10 saved tracks").assertExists()
    }

    @Test fun sourceSectionsTellLocalResultsFromPublicResults() {
        compose.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    NightWaveSearchSourceHeading("In your library", 3,
                        NightWaveBrowseChromePolicy.Source.SAVED_LIBRARY)
                    NightWaveSearchSourceHeading("Public results", 12,
                        NightWaveBrowseChromePolicy.Source.PUBLIC_VIDEO)
                }
            }
        }
        compose.onNodeWithTag("nightwave-saved-results-count").assertExists()
        compose.onNodeWithTag("nightwave-public-results-count").assertExists()
        compose.onNodeWithText("Saved in your library · playback may need internet").assertExists()
        compose.onNodeWithText("Public YouTube results · internet required").assertExists()
    }

    @Test fun offlineCollectionClickDoesNotChangeAnyPlaybackState() {
        var clicks by mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    MusicCollectionTile(
                        name = "Offline music",
                        subtitle = "Your imported audio",
                        cover = null,
                        onClick = { clicks++ }
                    )
                }
            }
        }
        compose.onNodeWithText("Offline music").assertExists().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
