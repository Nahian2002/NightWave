package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Real UI-only components; screen profile constraints are synthetic, not physical evidence. */
@RunWith(Parameterized::class)
class NW142AdaptiveProfileInstrumentedTest(
    private val width: Int, private val height: Int, private val scale: Float,
    private val theme: NightWaveTheme
) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}x{1} font{2} {3}")
        fun profiles(): List<Array<Any>> = listOf(320 to 640, 360 to 720, 412 to 840,
            640 to 320, 840 to 600, 240 to 400).flatMap { (width, height) ->
            listOf(1f, 2f, 3f).flatMap { scale -> NightWaveTheme.entries.map { theme ->
                arrayOf<Any>(width, height, scale, theme)
            } }
        }
    }

    @Test fun realAudioDetailsReflowsAndKeepsItsIndependentAction() {
        var toggles = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                NightWaveMaterialTheme(theme, reduceMotion = true) {
                    Column(Modifier.requiredWidth(width.dp).heightIn(max = height.dp)
                        .verticalScroll(rememberScrollState())) {
                        NightWaveAudioDetailsHeader(false) { toggles++ }
                    }
                }
            }
        }
        val node = compose.onNodeWithTag("nw142-audio-details")
        node.performScrollTo().assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.height >= 50f)
        node.performClick()
        compose.runOnIdle { assertEquals(1, toggles) }
    }

    @Test fun realNavigationKeepsAllFourExplicitDestinations() {
        var destination = -1
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                NightWaveMaterialTheme(theme, reduceMotion = true) {
                    Box(Modifier.requiredWidth(width.dp)) {
                        NightWavePremiumNavigation(0, true) { destination = it }
                    }
                }
            }
        }
        for ((index, tag) in listOf("home", "search", "library", "profile").withIndex()) {
            compose.onNodeWithTag("nightwave-tab-$tag").assertExists().performClick()
            // Reselecting Home invokes only the explicit reselect callback, which is absent here.
            if (index > 0) compose.runOnIdle { assertEquals(index, destination) }
        }
    }
}
