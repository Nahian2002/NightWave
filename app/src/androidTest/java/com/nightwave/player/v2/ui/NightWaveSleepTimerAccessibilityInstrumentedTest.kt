package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI only, no media service; complete on-phone TalkBack still pending. */
@RunWith(AndroidJUnit4::class)
class NightWaveSleepTimerAccessibilityInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun adjustableDialSupportsScreenReaderProgressAndConfirm() {
        var selectedMinutes = 0
        compose.setContent {
            MaterialTheme {
                NightWaveSleepTimerDialog(
                    remainingMs = 0L,
                    onDismiss = {},
                    onSet = { selectedMinutes = it },
                    onCancel = {}
                )
            }
        }
        compose.onNodeWithTag("sleep-timer-dial")
            .performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action(90f)
            }
        compose.onNodeWithTag("sleep-timer-confirm").performClick()
        compose.runOnIdle { assertEquals(90, selectedMinutes) }
    }

    @Test fun existingCountdownUsesHonestReplaceAndCancelWording() {
        compose.setContent {
            MaterialTheme {
                NightWaveSleepTimerDialog(
                    remainingMs = 10L * 60_000L,
                    onDismiss = {},
                    onSet = {},
                    onCancel = {}
                )
            }
        }
        compose.onNodeWithText("Replace timer").assertExists()
        compose.onNodeWithText("Cancel active timer").assertExists()
    }
}
