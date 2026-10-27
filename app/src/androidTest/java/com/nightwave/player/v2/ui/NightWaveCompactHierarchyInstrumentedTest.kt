package com.nightwave.player.v2.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.recommendation.PersonalMix
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** 36 window/type/theme combinations. Synthetic UI only: no service or account.
 * Captures are component evidence, never full Home/Search/Library/You acceptance.
 */
@RunWith(Parameterized::class)
class NightWaveCompactHierarchyInstrumentedTest(
    private val width: Int, private val scale: Float, private val theme: NightWaveTheme
) {
    @get:Rule val compose = createComposeRule()
    private val song = VideoPreview("An original listening fixture", "Example creator", "fixture://nw127", "", 120)

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}dp-{1}x-{2}")
        fun configurations(): List<Array<Any>> = listOf(320, 360, 412).flatMap { w ->
            listOf(1f, 2f, 3f).flatMap { f -> NightWaveTheme.entries.map { t -> arrayOf<Any>(w, f, t) } }
        }
    }

    private fun fixture(content: @Composable () -> Unit) {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { screenWidthDp = width }
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalDensity provides Density(1f, scale)) {
                NightWaveMaterialTheme(theme, reduceMotion = true) {
                    Column(Modifier.requiredWidth(width.dp).testTag("nw127-fixture")) { content() }
                }
            }
        }
    }

    private fun noOverflow(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Missing layout for $text", layouts.isNotEmpty())
        assertTrue("Clipped $text at $width/$scale/$theme", layouts.none { it.hasVisualOverflow })
    }

    private fun capture(component: String) {
        val context = InstrumentationRegistry.getInstrumentation().context
        val folder = File(context.cacheDir, "nw127-synthetic-${System.currentTimeMillis()}")
        check(folder.mkdirs())
        File(folder, "$component-$width-$scale-$theme.png").outputStream().use {
            check(compose.onNodeWithTag("nw127-fixture").captureToImage()
                .asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    @Test fun navigationTargetsAndLabelsRemainIndependent() {
        var destination = -1
        fixture { NightWavePremiumNavigation(0, true) { destination = it } }
        val tags = listOf("home", "search", "library", "profile")
        val bounds = tags.map { compose.onNodeWithTag("nightwave-tab-$it").fetchSemanticsNode().boundsInRoot }
        bounds.forEach { assertTrue(it.width >= 48f); assertTrue(it.height >= 48f) }
        bounds.zipWithNext().forEach { (a, b) -> assertTrue(a.right <= b.left) }
        listOf("Home", "Search", "Library", "You").forEach(::noOverflow)
        compose.onNodeWithTag("nightwave-tab-library").performClick()
        compose.runOnIdle { assertEquals(2, destination) }
        capture("navigation")
    }

    @Test fun recentRemovalRequiresDisclosureAndNeverPlaysMusic() {
        var plays = 0; var removals = 0; var options = 0
        fixture { NightWaveRecentShelfTile(song, false, { plays++ }, { options++ }, { removals++ }) }
        compose.onNodeWithTag("nw47-recent-remove").assertDoesNotExist()
        val play = compose.onNodeWithTag("nw47-recent-play").fetchSemanticsNode().boundsInRoot
        val overflow = compose.onNodeWithTag("nw47-recent-options").fetchSemanticsNode().boundsInRoot
        assertTrue(play.bottom <= overflow.top)
        assertTrue(overflow.width >= 48f && overflow.height >= 48f)
        compose.onNodeWithTag("nw47-recent-options").performClick()
        compose.runOnIdle { assertEquals(0, plays); assertEquals(0, options); assertEquals(0, removals) }
        compose.onNodeWithTag("nw47-recent-remove").performClick()
        compose.runOnIdle { assertEquals(1, removals); assertEquals(0, plays) }
        capture("recent")
    }

    @Test fun sectionActionReflowsAndKeepsItsFullLabel() {
        var calls = 0
        fixture {
            NightWaveSectionActions(heading = { SectionHeading("Recently played", "Your private listening history") },
                action = { TextButton(onClick = { calls++ }, modifier = Modifier.testTag("action")) { Text("Show more") } })
        }
        listOf("Recently played", "Your private listening history", "Show more").forEach(::noOverflow)
        val heading = compose.onNodeWithText("Recently played", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithTag("action").fetchSemanticsNode().boundsInRoot
        assertTrue(heading.bottom <= action.top || heading.right <= action.left)
        assertTrue(action.height >= 48f && action.width >= 48f)
        compose.onNodeWithTag("action").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
        capture("section-action")
    }

    @Test fun mixCaptionsGrowWithoutDependingOnArtwork() {
        fixture { NightWaveMixCard(PersonalMix("fixture", "A quiet evening", "", "", listOf(song)), false) {} }
        listOf("FROM YOUR LIBRARY", "A quiet evening", "1 track · View tracks").forEach(::noOverflow)
        capture("mix")
    }

    @Test fun continueAndSettingsKeepTheirOriginalCallbacks() {
        var resumes = 0; var settings = 0
        fixture {
            NightWaveCompactTopBar { settings++ }
            NightWaveHomeHero("Good evening", "Example", lastTrack = song, onContinue = { resumes++ })
        }
        compose.onNodeWithTag("nw127-open-settings").performClick()
        compose.onNodeWithTag("nightwave-home-hero").performClick()
        compose.runOnIdle { assertEquals(1, resumes); assertEquals(1, settings) }
        noOverflow("CONTINUE LISTENING")
        capture("continue")
    }
}
