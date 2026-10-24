package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI-only checks; do not initialize player services or touch private music. */
@RunWith(AndroidJUnit4::class)
class NightWaveAdaptiveChromeInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fourTabsRemainSelectableAtTwoHundredPercentFontScaleOnNarrowScreen() {
        var selected by mutableIntStateOf(0)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        NightWavePremiumNavigation(
                            selectedIndex = selected,
                            reducedMotion = true,
                            onNavigate = { selected = it }
                        )
                    }
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
    }

    @Test fun homeHeaderKeepsMeaningfulTextUnderLargeFontScale() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        NightWaveHomeHero(
                            greeting = "Good evening",
                            displayName = "Demo Listener"
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("nightwave-home-hero").assertExists()
        compose.onNodeWithText("Good evening").assertExists()
        compose.onNodeWithText("Welcome, Demo · Your music, your pace").assertExists()
    }
}
