package com.nightwave.player.v2.ui

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable Android profile/emulator. Read-only UI test: do not touch backup
 * restore, edit a profile, delete music or change persistence.
 */
@RunWith(AndroidJUnit4::class)
class NightWaveProfileCompactInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<NightWaveStudioActivity>()

    @Test fun premiumYouTabOpensCompactGroups() {
        compose.onNodeWithContentDescription("You").performClick()
        compose.onNodeWithText("Your space").assertExists()
        compose.onNodeWithText("Appearance & playback").assertExists()
        compose.onNodeWithText("Library & backup").assertExists()
    }

    @Test fun profileAppearanceIsExpandedOnlyOnRequest() {
        compose.onNodeWithContentDescription("You").performClick()
        compose.onNodeWithText("Theme", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Colour theme").assertExists()
    }
}
