package com.nightwave.player.v2.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.nightwave.player.v2.library.PlaylistFolderSnapshot
import com.nightwave.player.net.*
import com.nightwave.player.v2.insights.ListeningMemorySummary
import com.nightwave.player.v2.model.*
import com.nightwave.player.v2.playback.*
import com.nightwave.player.v2.recommendation.PersonalMix
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** 36 configurations × 10 actual UI cases. Blank source art and in-memory callbacks only.
 * Does not install data, start playback, connect accounts or contact providers.
 * Captures exist only when these cases actually execute on Android.
 */
@RunWith(Parameterized::class)
class NightWaveMatteAcceptanceInstrumentedTest(
    private val width: Int, private val scale: Float, private val theme: NightWaveTheme
) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}dp-{1}x-{2}")
        fun configurations(): List<Array<Any>> = listOf(320, 360, 412).flatMap { w ->
            listOf(1f, 2f, 3f).flatMap { s -> NightWaveTheme.entries.map { t -> arrayOf<Any>(w, s, t) } }
        }
    }
    private fun song(n: Int) = VideoPreview("Fixture song $n", "Fixture creator",
        "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'), "", 120L)
    private val songs = (1..24).map(::song)
    private var action = ""
    private fun fixture(reduceMotion: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { screenWidthDp = width }
            CompositionLocalProvider(LocalConfiguration provides configuration, LocalDensity provides Density(1f, scale)) {
                NightWaveMaterialTheme(theme, reduceMotion = reduceMotion) {
                    Box(Modifier.requiredWidth(width.dp).requiredHeight(780.dp).testTag("matte-fixture")) { content() }
                }
            }
        }
    }
    private fun capture(name: String) {
        val folder = File(InstrumentationRegistry.getInstrumentation().context.cacheDir, "matte-acceptance")
        check(folder.isDirectory || folder.mkdirs())
        val file = File.createTempFile("$name-$width-$scale-$theme-", ".png", folder)
        file.outputStream().use { output ->
            check(compose.onNodeWithTag("matte-fixture").captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
    @Test fun selectedThemeAndEveryNavigationDestinationAreRetained() {
        fixture {
            val palette = NightWaveDesign.palette(theme)
            assertEquals(palette.background.toInt(), MaterialTheme.colorScheme.background.toArgb())
            assertEquals(palette.foreground.toInt(), MaterialTheme.colorScheme.onBackground.toArgb())
            val colors = MaterialTheme.colorScheme
            assertEquals(palette.surface.toInt(), colors.surfaceContainerLow.toArgb())
            assertEquals(palette.surface.toInt(), colors.surfaceContainer.toArgb())
            assertEquals(palette.raised.toInt(), colors.surfaceContainerHigh.toArgb())
            assertEquals(palette.raised.toInt(), colors.surfaceContainerHighest.toArgb())
            assertEquals((if (palette.isDark) palette.background else palette.surface).toInt(),
                colors.surfaceContainerLowest.toArgb())
            assertEquals((if (palette.isDark) palette.background else palette.raised).toInt(), colors.surfaceDim.toArgb())
            assertEquals((if (palette.isDark) palette.raised else palette.surface).toInt(), colors.surfaceBright.toArgb())
            Column { NightWavePremiumNavigation(0, true) { action = "tab:$it" } }
        }
        for (tag in listOf("home", "search", "library", "profile")) {
            val node = compose.onNodeWithTag("nightwave-tab-$tag")
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.width >= 48f && bounds.height >= 48f)
            node.performClick()
        }
        compose.runOnIdle { assertEquals("tab:3", action) }; capture("navigation")
    }
    @Test fun homeKeepsContinueAndExplicitMixPlayWithOriginalReferences() {
        var resumed: List<VideoPreview> = emptyList(); var played: List<VideoPreview> = emptyList()
        fixture {
            StudioHome(Modifier, ProfileSettings("matte-fixture", theme = theme), songs, songs, songs,
                ListeningMemorySummary(false, 0L, 0, 0, emptyList()), "",
                onSearch = { action = it }, onResumeSpotlight = { _, queue -> resumed = queue },
                onPlay = { _, queue -> played = queue }, onSongOptions = {}, onRemoveRecent = {},
                onClearRecent = {}, openRecentHistory = {}, openLibrary = {}, openLikedSongs = {},
                openOffline = {}, openSearch = {})
        }
        compose.onNodeWithTag("nightwave-home-hero").performClick()
        compose.runOnIdle { assertEquals(songs, resumed); assertTrue(played.isEmpty()) }
        compose.onNodeWithTag("nw131-home-list").performScrollToNode(hasTestTag("nw131-mix-daily_mix_1"))
        compose.onNodeWithTag("nw131-mix-daily_mix_1").performClick()
        compose.runOnIdle { assertTrue(played.isEmpty()) }
        compose.onNodeWithTag("nw131-home-list").performScrollToNode(hasTestTag("nw131-play-mix"))
        compose.onNodeWithTag("nw131-play-mix").performClick()
        compose.runOnIdle { assertTrue(played.isNotEmpty()); assertTrue(played.all { candidate -> songs.any { it === candidate } }) }
        capture("home")
    }
    @Test fun searchInputKeepsTheExactUserSubmittedQueryAndExistingDispatch() {
        fixture {
            StudioSearch(Modifier, StudioSearchSnapshot(), onPrimarySearch = { _, _ -> },
                onNativeSearch = { query, _ -> action = query }, onWebSearch = {}, onPlay = { _, _ -> },
                onSongOptions = {}, playingUrl = "", corpus = LocalMusicCorpusState(0L, 0, failed = true),
                onRetryCorpus = {}, onOfflineSearch = {}, searchHistory = emptyList(), canUndoSearchRemoval = false,
                onRecordSearch = {}, onRemoveSearch = {}, onUndoSearchRemoval = {}, onClearSearch = {})
        }
        compose.onNodeWithTag("nw129-search-query").performTextInput("Fixture query")
        compose.onNodeWithTag("nw129-search-query").performImeAction()
        compose.runOnIdle { assertEquals("Fixture query", action) }; capture("search")
    }
    @Test fun libraryCreateAndImportRemainAvailableWithoutStartingPlayback() {
        fixture {
            StudioLibrary(Modifier, songs, songs, emptyList(), 0, false, emptyList(), PlaylistFolderSnapshot(), emptyList(),
                onTogglePinPlaylist = {}, playingUrl = "", selectedPlaylist = null, onSelectPlaylist = {},
                getPlaylist = { emptyList() }, onPlay = { _, _ -> error("Unexpected playback") }, onQuickPlay = {},
                onManage = {}, onSearch = {}, onCreatePlaylist = { action = "create" }, onManageFolders = {},
                onMovePlaylist = {}, onOffline = {}, offlineResumeTitle = null, offlineResumePositionMs = 0L,
                onResumeOffline = {}, onSongOptions = {}, onRemoveRecent = {}, onClearRecent = {},
                onPlaylistSongOptions = { _, _ -> }, onSharePlaylist = { _, _ -> }, onExportPlaylist = { _, _ -> },
                onBulkShareLinks = { _, _ -> }, onBulkRemove = { _, _ -> }, onBulkCopy = { _, _ -> },
                onMovePlaylistSong = { _, _, _, _ -> }, playlistUndo = null, onUndoPlaylistEdit = {},
                onDismissPlaylistUndo = {}, onImportSharedPlaylist = { action = "import" })
        }
        compose.onNodeWithTag("nw47-library-new-playlist").performClick()
        compose.runOnIdle { assertEquals("create", action) }
        compose.onNodeWithTag("nw132-library-options").performClick()
        compose.onNodeWithText("Import playlist (.txt or pasted links)").performClick()
        compose.runOnIdle { assertEquals("import", action) }; capture("library")
    }
    private class Actions : StudioPlayerActions {
        var committed: Pair<String, Long>? = null
        override fun play(video: VideoPreview, collection: List<VideoPreview>) = Unit
        override fun playPause() = Unit
        override fun retry() = Unit
        override fun enqueue(video: VideoPreview) = Unit
        override fun previous() = Unit
        override fun next() = Unit
        override fun seekTo(positionMs: Long) { error("Unscoped seek") }
        override fun seekToForTrack(expectedMediaId: String, positionMs: Long) { committed = expectedMediaId to positionMs }
        override fun toggleShuffle() = Unit
        override fun cycleRepeat() = Unit
        override fun jumpTo(index: Int) = Unit
        override fun removeUpcoming(index: Int, expectedUrl: String): String? = null
        override fun undoUpcoming(token: String) = Unit
        override fun refreshQueue() = Unit
    }
    @Test fun fullPlayerRetainsSaveQueueLyricsAndTrackScopedSeek() {
        val actions = Actions(); val selected = songs.first()
        fixture {
            StudioNowPlayingOverlay(expanded = true, state = StudioPlayerState(connected = true, title = selected.title,
                videoUrl = selected.url, durationMs = 120_000L), actions = actions, liked = false, onLike = {},
                onOpenQueue = { action = "queue" }, onSaveToPlaylist = { action = "save" }, onSaveOffline = {},
                sleepRemainingMs = 0L, onSleepTimer = {}, offlineSpeedPercent = 100, onOfflineSpeed = {},
                listeningLevelPercent = 100, onListeningLevel = {}, crossfadeSeconds = 0, onCrossfade = {},
                normalizationEnabled = false, onNormalizationChange = {}, onDismiss = {}, reduceMotion = true)
        }
        compose.onNodeWithContentDescription("Save track to playlist").performClick()
        compose.runOnIdle { assertEquals("save", action) }
        compose.onNodeWithTag("nw133-player-content").performScrollToNode(hasTestTag("nightwave-player-seek"))
        compose.onNodeWithTag("nightwave-player-seek").performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(.5f)) }
        compose.runOnIdle { assertEquals(selected.url to 60_000L, actions.committed) }
        compose.onNodeWithTag("nw133-player-content").performScrollToNode(hasTestTag("nightwave-now-playing-lyrics-action"))
        compose.onNodeWithTag("nightwave-now-playing-lyrics-action").assertExists()
        compose.onNodeWithTag("nw133-player-content").performScrollToNode(hasTestTag("nightwave-now-playing-queue-action"))
        compose.onNodeWithTag("nightwave-now-playing-queue-action").performClick()
        compose.runOnIdle { assertEquals("queue", action) }; capture("player")
    }
    @Test fun bentoPreservesBothCollectionActionsInMeasuredReadingOrder() {
        val first = PersonalMix("matte-first", "First collection", "", "", songs.take(4))
        val second = PersonalMix("matte-second", "Second collection", "", "", songs.takeLast(4))
        fixture {
            Column(Modifier.fillMaxWidth().padding(NightWavePremiumDesignTokens.pageGutterDp(width.toFloat()).dp)) {
                NightWaveBentoPair(first = { NightWaveMixCard(first, false) { action = "first" } },
                    second = { NightWaveMixCard(second, false) { action = "second" } })
            }
        }
        val a = compose.onNodeWithTag("nw131-mix-matte-first").fetchSemanticsNode().boundsInRoot
        val b = compose.onNodeWithTag("nw131-mix-matte-second").fetchSemanticsNode().boundsInRoot
        if (width == 320 || scale >= 1.3f) assertTrue(a.bottom <= b.top) else assertTrue(a.right <= b.left)
        compose.onNodeWithTag("nw131-mix-matte-second").performClick()
        compose.runOnIdle { assertEquals("second", action) }; capture("bento")
    }
    @Test fun pressCancellationKeepsTheTargetAndDoesNotInvokeAnAction() {
        var calls = 0
        fixture(reduceMotion = false) { NightWaveMatteButton({ calls++ }, Modifier.testTag("matte-test-button")) { Text("Play") } }
        val node = compose.onNodeWithTag("matte-test-button")
        val before = node.fetchSemanticsNode().boundsInRoot
        node.performTouchInput { down(center); moveTo(Offset(-100f, -100f)); up() }
        compose.runOnIdle { assertEquals(0, calls) }
        assertEquals(before, node.fetchSemanticsNode().boundsInRoot)
        assertTrue(before.height >= 48f)
        node.performClick(); compose.runOnIdle { assertEquals(1, calls) }
    }
    @Test fun backdropRendersOnlyAProvidedDrawableOrTheMatteFallback() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val image = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GREEN) }
        val drawable = android.graphics.drawable.BitmapDrawable(context.resources, image)
        fixture { NightWaveArtworkBackdrop(drawable, Modifier.fillMaxSize()) }
        val capture = compose.onNodeWithTag("matte-fixture").captureToImage().asAndroidBitmap()
            .copy(Bitmap.Config.ARGB_8888, false)
        val pixel = capture.getPixel(capture.width / 2, capture.height / 2)
        if (android.os.Build.VERSION.SDK_INT >= 31) assertTrue(android.graphics.Color.green(pixel) > android.graphics.Color.red(pixel))
        else assertEquals(NightWaveDesign.palette(theme).background.toInt(), pixel)
        capture("backdrop")
    }

    @Test fun singletonCollectionDoesNotGrowAFullWidthSquareArtworkWall() {
        val mix = PersonalMix("single", "Last collection", "", "", songs.take(4))
        fixture {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                NightWaveMixCard(mix, false, modifier = Modifier.fillMaxWidth(),
                    featured = NightWaveThemeExpressionPolicy.featuredMixCell(1), onClick = { action = "single" })
            }
        }
        val card = compose.onNodeWithTag("nw131-mix-single")
        if (scale == 1f) assertTrue(card.fetchSemanticsNode().boundsInRoot.height < 200f)
        card.performClick(); compose.runOnIdle { assertEquals("single", action) }; capture("single-collection")
    }
    @Test fun themedMoodKeepsItsExplicitSearchActionAndReadableLabels() {
        fixture {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                NightWaveMoodArt("After hours", "Midnight drives", emptyList(),
                    modifier = Modifier.fillMaxWidth().testTag("expressive-mood"), onClick = { action = "mood-search" })
            }
        }
        compose.onNodeWithText("After hours").assertExists()
        compose.onNodeWithText("Midnight drives").assertExists()
        compose.onNodeWithTag("expressive-mood").performClick()
        compose.runOnIdle { assertEquals("mood-search", action) }; capture("mood-color")
    }

}
