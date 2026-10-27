package com.nightwave.player.v2.ui

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.offline.*
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.playback.StudioPlayerActions
import com.nightwave.player.v2.playback.StudioPlayerState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated UI/intents. No account, download, file import or service is started. */
@RunWith(AndroidJUnit4::class)
class NightWaveOfflineShellInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val actions = Actions()
    private var main: Pair<Int, Boolean>? = null
    private var navigated: NightWaveOfflineRoute? = null
    private var backs = 0
    private var recoveries = 0

    private fun content(hasTrack: Boolean = false) {
        compose.setContent {
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                Box(Modifier.width(320.dp)) {
                    NightWaveOfflineShell(NightWaveOfflineRoute.LIBRARY,
                        StudioPlayerState(connected = true, title = "Selected fixture", artist = "Example",
                            videoUrl = if (hasTrack) "offline:fixture" else ""),
                        actions, true, { backs++ }, { navigated = it },
                        { tab, expanded -> main = tab to expanded }, { recoveries++ }) {
                        Text("Retained native-content slot")
                    }
                }
            }
        }
    }

    @Test fun routeSwitchesAreNavigationOnlyAndCurrentRouteDoesNotRepeat() {
        content()
        compose.onNodeWithTag("nw128-route-LIBRARY").performClick()
        compose.runOnIdle { assertNull(navigated) }
        compose.onNodeWithTag("nw128-route-CATALOG").performClick()
        compose.runOnIdle { assertEquals(NightWaveOfflineRoute.CATALOG, navigated); assertEquals(0, actions.commands) }
        compose.onNodeWithText("Retained native-content slot").assertExists()
    }

    @Test fun selectedLibraryTabReturnsToMainLibraryInsteadOfBeingInert() {
        content(true)
        compose.onNodeWithTag("nightwave-tab-library").performClick()
        compose.runOnIdle { assertEquals(2 to false, main); assertEquals(0, actions.commands) }
    }

    @Test fun hidingAndRestoringPlayerDoesNotPauseStopOrClearSelectedMusic() {
        content(true)
        compose.onNodeWithContentDescription("Hide mini-player; music keeps playing").performClick()
        compose.onNodeWithTag("nightwave-mini-player").assertDoesNotExist()
        compose.onNodeWithTag("nw128-show-player").performClick()
        compose.onNodeWithTag("nightwave-mini-player").assertExists()
        compose.runOnIdle { assertEquals(0, actions.commands) }
    }

    @Test fun miniPlayerTransportUsesTheExistingActionOwner() {
        content(true)
        compose.onNodeWithContentDescription("Resume playback").performClick()
        compose.runOnIdle { assertEquals(1, actions.commands) }
    }

    @Test fun fullPlayerRequestNeverDispatchesPlay() {
        content(true)
        compose.onNodeWithText("Selected fixture").performClick()
        compose.onNodeWithTag("nw128-full-player").performClick()
        compose.runOnIdle { assertEquals(2 to true, main); assertEquals(0, actions.commands) }
    }

    @Test fun recoveryAndBackRemainExplicitSeparateActions() {
        content()
        compose.onNodeWithTag("nw128-back").performClick()
        compose.onNodeWithContentDescription("Offline navigation options").performClick()
        compose.onNodeWithText("Use native recovery view").performClick()
        compose.runOnIdle { assertEquals(1, backs); assertEquals(1, recoveries); assertEquals(0, actions.commands) }
    }

    @Test fun privateSiblingIntentsReuseButUnrelatedIntentsRemainUntouched() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (route in NightWaveOfflineRoute.entries) {
            val original = Intent().setClassName(context, route.componentName)
            val normalized = NightWaveOfflineNavigator.reuseOfflineIntent(original)
            assertEquals(0, original.flags)
            assertTrue(normalized.flags and Intent.FLAG_ACTIVITY_REORDER_TO_FRONT != 0)
            assertTrue(normalized.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
            assertEquals(0, normalized.flags and (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        val external = Intent(Intent.ACTION_OPEN_DOCUMENT)
        assertSame(external, NightWaveOfflineNavigator.reuseOfflineIntent(external))
        assertNull(NightWaveOfflineNavigator.request(Intent(), null))
    }

    @Test fun nativePagedRowsRemainReachableInAShortViewport() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val rows = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                repeat(50) { addView(TextView(context).apply { text = "Fixture $it" }, LinearLayout.LayoutParams(-1, 72)) }
            }
            val inner = ScrollView(context).apply { addView(rows) }
            val panel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply { text = "Controls" }, LinearLayout.LayoutParams(-1, 500))
                addView(inner, LinearLayout.LayoutParams(-1, 0, 1f))
            }
            val retained = FrameLayout(context).apply { addView(panel, FrameLayout.LayoutParams(-1, -1)) }
            val viewport = createOfflineViewport(context, retained)
            viewport.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY))
            viewport.layout(0, 0, 320, 240)
            assertTrue("Whole panel must scroll", viewport.canScrollVertically(1))
            assertTrue("Weighted song rows cannot collapse", inner.measuredHeight >= 50 * 72)
            viewport.scrollTo(0, Int.MAX_VALUE)
            assertFalse("Last paged row must be reachable", viewport.canScrollVertically(1))
        }
    }

    private class Actions : StudioPlayerActions {
        var commands = 0
        override fun play(video: VideoPreview, collection: List<VideoPreview>) { commands++ }
        override fun playPause() { commands++ }
        override fun retry() { commands++ }
        override fun enqueue(video: VideoPreview) { commands++ }
        override fun previous() { commands++ }
        override fun next() { commands++ }
        override fun seekTo(positionMs: Long) { commands++ }
        override fun toggleShuffle() { commands++ }
        override fun cycleRepeat() { commands++ }
        override fun jumpTo(index: Int) { commands++ }
        override fun removeUpcoming(index: Int, expectedUrl: String): String? { commands++; return null }
        override fun undoUpcoming(token: String) { commands++ }
        override fun refreshQueue() { commands++ }
    }
}
