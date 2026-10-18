package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
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

/**
 * UI-only premium shell tests. No media service, storage, Google Drive,
 * network, personal songs, or installed-app state is accessed.
 */
@RunWith(AndroidJUnit4::class)
class NightWavePremiumChromeInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun premiumNavigationKeepsAllFourTappableDestinationsOnSmallScreens() {
        var selection by mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWavePremiumNavigation(
                        selectedIndex = selection,
                        reducedMotion = true,
                        onNavigate = { selection = it }
                    )
                }
            }
        }
        compose.onNodeWithTag("nightwave-tab-home").assertIsSelected()
        compose.onNodeWithTag("nightwave-tab-search").assertExists().performClick()
        compose.onNodeWithTag("nightwave-tab-search").assertIsSelected()
        compose.onNodeWithTag("nightwave-tab-library").assertExists().performClick()
        compose.onNodeWithTag("nightwave-tab-library").assertIsSelected()
        compose.onNodeWithTag("nightwave-tab-profile").assertExists().performClick()
        compose.onNodeWithTag("nightwave-tab-profile").assertIsSelected()
        compose.runOnIdle { assertEquals(3, selection) }
    }

    @Test fun premiumHomeHeroHandlesUserProfileWithoutTrackMetadata() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    NightWaveHomeHero(greeting = "Good evening", displayName = "Demo Listener")
                }
            }
        }
        compose.onNodeWithTag("nightwave-home-hero").assertExists()
        compose.onNodeWithText("Good evening").assertExists()
        compose.onNodeWithText("Welcome, Demo · Your music, your pace").assertExists()
    }

    @Test fun blankProfileNameStillShowsStableHomeHeader() {
        compose.setContent {
            MaterialTheme {
                NightWaveHomeHero(greeting = "Good morning", displayName = "")
            }
        }
        compose.onNodeWithTag("nightwave-home-hero").assertExists()
        compose.onNodeWithText("Welcome, your space · Your music, your pace")
            .assertExists()
    }
}
