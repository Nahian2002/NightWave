package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Disposable Compose fixture only. Never launches personal playback or writes an app store. */
@RunWith(Parameterized::class)
class NW150SearchStoppedInstrumentedTest(private val theme: NightWaveTheme) {
    companion object { @JvmStatic @Parameterized.Parameters(name="{0}") fun themes() =
        NightWaveTheme.entries.map { arrayOf(it) } }
    @get:Rule val compose = createComposeRule()

    private fun check(reason: SearchStopReason, hasResults: Boolean, title: String) {
        var retries = 0
        compose.setContent {
            NightWaveMaterialTheme(theme, reduceMotion = true) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    Column(Modifier.requiredWidth(320.dp)) {
                        NightWaveSearchStoppedCard(reason, hasResults) { retries++ }
                    }
                }
            }
        }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText("Online search unavailable").assertDoesNotExist()
        compose.onNodeWithTag("nw150-search-retry").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
    @Test fun deliberateCancelOffersRetryWithoutClaimingNetworkFailure() =
        check(SearchStopReason.CANCELLED, false, "Search cancelled")
    @Test fun backgroundStopKeepsExistingResultsAndOffersRetry() =
        check(SearchStopReason.BACKGROUNDED, true, "Search paused while away")
}
