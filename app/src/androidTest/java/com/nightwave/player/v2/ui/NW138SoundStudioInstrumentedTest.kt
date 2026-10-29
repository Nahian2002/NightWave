package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Apply component in the production sibling layout; no service or personal preferences. */
@RunWith(AndroidJUnit4::class)
class NW138SoundStudioInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private var applies = 0
    private var discards = 0
    private fun show(edited: Boolean = true, canApply: Boolean = true,
        busy: Boolean = false, large: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else 1f)) {
                NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                    Column(Modifier.fillMaxWidth().height(560.dp)) {
                        LazyColumn(Modifier.weight(1f).testTag("fixture-scroll")) {
                            items(32) { Text("Band fixture $it", Modifier.height(64.dp)) }
                        }
                        SoundStudioApplyBar(edited, canApply, busy,
                            onApply = { applies++ }, onDiscard = { discards++ })
                    }
                }
            }
        }
    }
    @Test fun applyAndDiscardStayReachableAfterScrollingAtLargeType() {
        show(large = true)
        compose.onNodeWithTag("fixture-scroll").performScrollToIndex(31)
        compose.onNodeWithTag("nw138-apply").assertIsDisplayed().performClick()
        compose.onNodeWithTag("nw138-discard").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, applies); assertEquals(1, discards) }
    }
    @Test fun pendingReplyBlocksDuplicateApplyAndDiscard() {
        show(busy = true)
        compose.onNodeWithText("Waiting for playback confirmation").assertIsDisplayed()
        compose.onNodeWithTag("nw138-apply").assertIsNotEnabled()
        compose.onNodeWithTag("nw138-discard").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, applies); assertEquals(0, discards) }
    }
    @Test fun unavailableManualControlsExplainDisabledApply() {
        show(canApply = false)
        compose.onNodeWithText("Manual EQ unavailable · check audio compatibility").assertIsDisplayed()
        compose.onNodeWithTag("nw138-apply").assertIsNotEnabled()
        compose.onNodeWithTag("nw138-discard").assertIsEnabled()
    }
    @Test fun unchangedReadbackHasNoDestructiveDraftAction() {
        show(edited = false)
        compose.onNodeWithTag("nw138-discard").assertDoesNotExist()
        compose.onNodeWithTag("nw138-apply").assertIsEnabled()
    }
}
