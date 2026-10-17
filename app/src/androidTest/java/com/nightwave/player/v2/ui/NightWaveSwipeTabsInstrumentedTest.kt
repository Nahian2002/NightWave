package com.nightwave.player.v2.ui

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Activity Compose gestures on a disposable emulator/profile.
 * Never modifies app storage, music queues or downloaded audio.
 */
@RunWith(AndroidJUnit4::class)
class NightWaveSwipeTabsInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<NightWaveStudioActivity>()

    @Test fun swipesChangeTheSelectedBottomTab() {
        val pager = compose.onNodeWithTag("nightwave-tab-pager")
        compose.onNodeWithTag("nightwave-tab-home").assertIsSelected()
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag("nightwave-tab-search").assertIsSelected()
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag("nightwave-tab-library").assertIsSelected()
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag("nightwave-tab-profile").assertIsSelected()
        pager.performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithTag("nightwave-tab-library").assertIsSelected()
    }

    @Test fun bottomTabClicksAndVerticalScrollingShareOnePageState() {
        val pager = compose.onNodeWithTag("nightwave-tab-pager")
        pager.performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithTag("nightwave-tab-home").assertIsSelected()
        compose.onNodeWithTag("nightwave-tab-profile").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("nightwave-tab-profile").assertIsSelected()
        pager.performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithTag("nightwave-tab-library").assertIsSelected()
        compose.onNodeWithTag("nightwave-tab-home").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("nightwave-tab-home").assertIsSelected()
    }
}
