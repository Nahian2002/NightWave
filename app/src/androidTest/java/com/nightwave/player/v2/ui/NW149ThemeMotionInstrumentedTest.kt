package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Real Material theme, default Undo and navigation; synthetic callbacks, no stored settings or queue. */
@RunWith(Parameterized::class)
class NW149ThemeMotionInstrumentedTest(private val theme: NightWaveTheme) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = NightWaveTheme.entries.map { arrayOf<Any>(it) }
    }
    @Test fun actualDefaultSnackbarUsesReadableSelectedThemeUndoAndKeepsItsAction() {
        val host = SnackbarHostState(); var undone = false; var role: Int? = null; var background: Int? = null
        compose.setContent {
            NightWaveMaterialTheme(theme, reduceMotion = true) {
                val action = SnackbarDefaults.actionColor
                val inverse = MaterialTheme.colorScheme.inverseSurface
                SideEffect { role = action.toArgb(); background = inverse.toArgb() }
                SnackbarHost(host)
                LaunchedEffect(Unit) {
                    undone = host.showSnackbar("Synthetic removed entry", "Undo", duration = SnackbarDuration.Indefinite) ==
                        SnackbarResult.ActionPerformed
                }
            }
        }
        compose.onNodeWithText("Undo").assertIsDisplayed().performClick()
        compose.runOnIdle {
            val p = NightWaveDesign.palette(theme)
            assertEquals(paletteColor(NightWaveThemeMotionPolicy.inverseAction(p)).toArgb(), role)
            assertTrue(NightWaveContrastPolicy.ratio(role!!.toLong(), background!!.toLong()) >= 4.5)
            assertTrue(undone)
        }
    }
    @Test fun actualNavigationRetainsAllFourActionsAndSelectionWithReducedMotion() {
        var selected by mutableIntStateOf(0); var reduced = false
        compose.setContent {
            NightWaveMaterialTheme(theme, reduceMotion = true) {
                val effective = LocalNightWaveReduceMotion.current
                SideEffect { reduced = effective }
                Column(Modifier.requiredWidth(360.dp)) {
                    NightWavePremiumNavigation(selected, effective, onNavigate = { selected = it })
                }
            }
        }
        for (tag in listOf("search", "library", "profile", "home")) {
            compose.onNodeWithTag("nightwave-tab-$tag").assertIsDisplayed().performClick().assertIsSelected()
        }
        compose.runOnIdle { assertTrue(reduced); assertEquals(0, selected) }
    }
}
