package com.nightwave.player.v2.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Production components, disposable local callbacks; no service, settings or personal data. */
@RunWith(Parameterized::class)
class NW147SecondaryLayoutsInstrumentedTest(
    private val width: Int, private val height: Int, private val scale: Float, private val theme: NightWaveTheme
) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}x{1}-font{2}-{3}")
        fun profiles(): List<Array<Any>> = listOf(Triple(320, 640, 1f), Triple(360, 640, 1f),
            Triple(411, 700, 1f), Triple(320, 640, 2f), Triple(640, 320, 3f)).flatMap { (w, h, f) ->
            NightWaveTheme.entries.map { t -> arrayOf<Any>(w, h, f, t) }
        }
    }
    private fun fixture(content: @Composable () -> Unit) {
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).apply { screenWidthDp = width; screenHeightDp = height }
            CompositionLocalProvider(LocalConfiguration provides config, LocalDensity provides Density(1f, scale)) {
                NightWaveMaterialTheme(theme, reduceMotion = true) {
                    Column(Modifier.requiredSize(width.dp, height.dp).testTag("nw147-fixture")
                        .verticalScroll(rememberScrollState())) { content() }
                }
            }
        }
    }
    private fun noOverflow(label: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty()); assertTrue(layouts.none { it.hasVisualOverflow })
    }
    private fun capture(name: String) {
        val folder = File(InstrumentationRegistry.getInstrumentation().context.cacheDir, "nw147-layouts").apply { mkdirs() }
        File(folder, "$name-$width-$height-$scale-$theme.png").outputStream().use {
            assertTrue(compose.onNodeWithTag("nw147-fixture").captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
    @Test fun longSettingsTextWrapsAndTrailingTouchDoesNotTriggerParent() {
        var rows = 0; var actions = 0
        val title = "দীর্ঘ সেটিংস শিরোনাম — Appearance and playback"
        val detail = "Your library stays on this device. Review the complete settings without losing any explanation."
        fixture {
            NightWaveSettingsRow(title, detail, Icons.Outlined.Settings, { rows++ }, Modifier.testTag("settings"), trailing = {
                TextButton(onClick = { actions++ }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("action")) { Text("Details") }
            })
        }
        noOverflow(title); noOverflow(detail); noOverflow("Details")
        compose.onNodeWithTag("action").performScrollTo().assertIsDisplayed()
        val bounds = compose.onNodeWithTag("action").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width >= 48f && bounds.height >= 48f)
        compose.onNodeWithTag("action").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, actions); assertEquals(0, rows) }
        compose.onNodeWithTag("settings").performClick()
        compose.runOnIdle { assertEquals(1, rows) }
        capture("settings")
    }
    @Test fun technicalDisclosureRetainsDetailsWithoutChangingAnAction() {
        var refreshes = 0
        val explanation = "Effects depend on Android's audio session and selected headphones. Enabled does not guarantee an audible change on every output route."
        fixture {
            Text("Current EQ readback remains visible")
            NightWaveTechnicalDetails("Playback compatibility") { Text(explanation) }
            TextButton(onClick = { refreshes++ }, modifier = Modifier.heightIn(min = 48.dp).testTag("refresh")) { Text("Refresh audio device") }
        }
        compose.onNodeWithText(explanation).assertDoesNotExist()
        compose.onNodeWithTag("nw147-technical-disclosure").performScrollTo().performClick()
        compose.onNodeWithText(explanation).assertExists(); noOverflow(explanation)
        compose.runOnIdle { assertEquals(0, refreshes) }
        capture("details-expanded")
        compose.onNodeWithTag("nw147-technical-disclosure").performScrollTo().performClick()
        compose.onNodeWithText(explanation).assertDoesNotExist()
        compose.onNodeWithText("Current EQ readback remains visible").assertExists()
        compose.onNodeWithTag("refresh").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
        capture("details-collapsed")
    }
}
