package com.nightwave.player.v2.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** 36 configurations × 17 actual UI cases. Blank source art and in-memory callbacks only.
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
    @Test fun shortStatisticsActionsUseMeasuredWidthAndKeepIndependentTargets() {
        fixture {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                NightWaveCompactActions {
                    NightWaveMatteOutlinedButton(onClick = { action = "7" },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("compact-seven")) { Text("7 days") }
                    NightWaveMatteOutlinedButton(onClick = { action = "30" },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("compact-thirty")) { Text("30 days") }
                }
            }
        }
        val first=compose.onNodeWithTag("compact-seven").fetchSemanticsNode().boundsInRoot
        val second=compose.onNodeWithTag("compact-thirty").fetchSemanticsNode().boundsInRoot
        assertTrue(first.height >= 48f && second.height >= 48f)
        if (scale == 1f) assertEquals(first.top,second.top,1f)
        assertTrue(second.left >= first.right || second.top >= first.bottom)
        compose.onNodeWithTag("compact-thirty").performClick(); assertEquals("30",action)
        compose.onNodeWithTag("compact-seven").performClick(); assertEquals("7",action)
        capture("compact-statistics-actions")
    }
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
            assertEquals(colors.onSurface, androidx.compose.material3.contentColorFor(colors.surface))
            assertTrue(colors.errorContainer != colors.surface)
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
        var snapshot by mutableStateOf(StudioSearchSnapshot())
        var visible by mutableStateOf(true)
        fixture {
            val holder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
            if (visible) holder.SaveableStateProvider("search") {
                StudioSearch(Modifier, snapshot, onPrimarySearch = { _, _ -> },
                    onNativeSearch = { query, _ -> action = query }, onWebSearch = {}, onPlay = { _, _ -> },
                    onSongOptions = {}, playingUrl = "", corpus = LocalMusicCorpusState(0L, 0, failed = true),
                    onRetryCorpus = {}, onOfflineSearch = {}, searchHistory = emptyList(), canUndoSearchRemoval = false,
                    onRecordSearch = {}, onRemoveSearch = {}, onUndoSearchRemoval = {}, onClearSearch = {})
            }
        }
        val field = compose.onNodeWithTag("nw129-search-query")
        field.performClick()
        compose.onNodeWithText("Search songs, artists or links").assertDoesNotExist()
        compose.runOnIdle { assertEquals("", action) }
        field.performTextInput("Fixture query")
        field.performImeAction()
        compose.runOnIdle { assertEquals("Fixture query", action) }
        field.performTextClearance()
        compose.runOnIdle { snapshot = StudioSearchSnapshot(query = "Fixture query") }
        field.assertTextEquals("") // A late provider response cannot overwrite a cleared draft.
        compose.runOnIdle { snapshot = StudioSearchSnapshot(query = "After hours", prefillToken = "explicit-1") }
        field.assertTextEquals("After hours")
        field.performTextClearance()
        compose.runOnIdle { visible = false }
        compose.waitForIdle()
        compose.runOnIdle { visible = true }
        field.performClick()
        field.assertTextEquals("") // Same consumed shortcut cannot replay when the page returns.
        compose.runOnIdle { snapshot = snapshot.copy(prefillToken = "explicit-2") }
        field.assertTextEquals("After hours") // A new intentional shortcut still works.
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
    @Test fun sparseLibraryExposesGuidanceAndRealAutomaticCollections() {
        val collection = com.nightwave.player.v2.library.SmartLibraryCollection(
            "nightwave://smart/fixture", "Fixture recent collection", "From history", songs.take(3))
        fixture {
            StudioLibrary(Modifier, emptyList(), songs, emptyList(), 0, false, listOf(collection),
                PlaylistFolderSnapshot(), emptyList(), onTogglePinPlaylist = {}, playingUrl = "",
                selectedPlaylist = null, onSelectPlaylist = { action = it.orEmpty() },
                getPlaylist = { emptyList() }, onPlay = { _, _ -> error("Unexpected playback") }, onQuickPlay = {},
                onManage = {}, onSearch = { action = "find" }, onCreatePlaylist = { action = "create" },
                onManageFolders = {}, onMovePlaylist = {}, onOffline = { action = "offline" },
                offlineResumeTitle = null, offlineResumePositionMs = 0L, onResumeOffline = {},
                onSongOptions = {}, onRemoveRecent = {}, onClearRecent = {}, onPlaylistSongOptions = { _, _ -> },
                onSharePlaylist = { _, _ -> }, onExportPlaylist = { _, _ -> }, onBulkShareLinks = { _, _ -> },
                onBulkRemove = { _, _ -> }, onBulkCopy = { _, _ -> }, onMovePlaylistSong = { _, _, _, _ -> },
                playlistUndo = null, onUndoPlaylistEdit = {}, onDismissPlaylistUndo = {}, onImportSharedPlaylist = {})
        }
        compose.onNodeWithTag("composition-library-header").assertExists()
        compose.onNodeWithTag("composition-library-find").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("find", action) }
        compose.onNodeWithText(collection.title).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(collection.id, action) }
        compose.onNodeWithTag("nw132-automatic-toggle").performScrollTo().performClick()
        compose.onNodeWithText(collection.title).assertDoesNotExist()
        compose.onNodeWithTag("nw132-automatic-toggle").performClick()
        compose.onNodeWithText(collection.title).performScrollTo().assertIsDisplayed()
        capture("sparse-library")
    }
    @Test fun playerEnterAndInterruptedExitKeepVisibleContentEveryFrame() {
        var expanded by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        fixture(reduceMotion = false) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Magenta)) {
                Text("Underlying screen", modifier = Modifier.testTag("continuity-underlay"))
                StudioNowPlayingOverlay(expanded, StudioPlayerState(connected = true,
                    title = "Transition fixture", artist = "Fixture creator", videoUrl = songs.first().url),
                    Actions(), liked = false, onLike = {}, onOpenQueue = {}, onSaveToPlaylist = {}, onSaveOffline = {},
                    sleepRemainingMs = 0L, onSleepTimer = {}, offlineSpeedPercent = 100, onOfflineSpeed = {},
                    listeningLevelPercent = 100, onListeningLevel = {}, crossfadeSeconds = 0, onCrossfade = {},
                    normalizationEnabled = false, onNormalizationChange = {}, onDismiss = { expanded = false },
                    reduceMotion = false)
            }
        }
        fun assertInk() {
            val bitmap = compose.onNodeWithTag("matte-fixture").captureToImage().asAndroidBitmap()
            val background = NightWaveDesign.palette(theme).background.toInt()
            var ink = 0
            for (y in 8 until bitmap.height - 8 step 4) for (x in 8 until bitmap.width - 8 step 4) {
                val pixel = bitmap.getPixel(x,y)
                val difference = kotlin.math.abs(android.graphics.Color.red(pixel)-android.graphics.Color.red(background)) +
                    kotlin.math.abs(android.graphics.Color.green(pixel)-android.graphics.Color.green(background)) +
                    kotlin.math.abs(android.graphics.Color.blue(pixel)-android.graphics.Color.blue(background))
                if (pixel != android.graphics.Color.MAGENTA && difference > 100) ink++
            }
            assertTrue("Player must paint readable content, not only its backdrop", ink > 20)
            assertTrue(bitmap.getPixel(bitmap.width/2,bitmap.height/2) != android.graphics.Color.MAGENTA)
        }
        compose.runOnIdle { expanded = true }
        repeat(4) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle(); assertInk() }
        compose.runOnIdle { expanded = false }
        compose.mainClock.advanceTimeBy(48); compose.waitForIdle(); assertInk()
        compose.runOnIdle { expanded = true }
        compose.mainClock.advanceTimeByFrame(); compose.waitForIdle(); assertInk()
        compose.runOnIdle { expanded = false }
        compose.mainClock.advanceTimeBy(400); compose.waitForIdle()
        compose.onNodeWithTag("nw58-opaque-player-backdrop").assertDoesNotExist()
        val ended = compose.onNodeWithTag("matte-fixture").captureToImage().asAndroidBitmap()
        assertEquals(android.graphics.Color.MAGENTA,ended.getPixel(ended.width/2,ended.height/2))
        compose.mainClock.autoAdvance = true
        capture("player-continuity")
    }
    @Test fun listeningStatisticsRetainWindowsConsentRefreshAndEraseAtLargeType() {
        val today = com.nightwave.player.v2.insights.ListeningTrendEngine.utcEpochDay(System.currentTimeMillis())
        val memory = ListeningMemorySummary(true,120_000L,3,1,emptyList(),daily = listOf(
            com.nightwave.player.v2.insights.DailyListeningBucket(today,120_000L,3)))
        fixture {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ListeningMemoryPanel(memory, onEnabledChange = { action = "consent:$it" },
                    onErase = { action = "erase" }, onRefresh = { action = "refresh" })
            }
        }
        compose.onNodeWithText("2 min recorded · 3 sessions (30s+)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("listening-window-30").performScrollTo().performClick()
        compose.onNodeWithText("2 min recorded · 3 sessions (30s+)").assertExists()
        compose.onNodeWithTag("listening-window-7").performScrollTo().performClick()
        compose.onNodeWithText("Refresh").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("refresh",action) }
        compose.onNodeWithText("Erase & turn off").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("erase",action) }
        compose.onNodeWithTag("listening-memory-consent").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("consent:false",action) }
        capture("readable-statistics")
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
        val actions = Actions(); val selected = songs.first().copy(
            title = "Fixture song 1 | Film credits | Performer credits | Fixture creator")
        fixture {
            StudioNowPlayingOverlay(expanded = true, state = StudioPlayerState(connected = true, title = selected.title,
                videoUrl = selected.url, artist = selected.author, durationMs = 120_000L), actions = actions, liked = false, onLike = {},
                onOpenQueue = { action = "queue" }, onSaveToPlaylist = { action = "save" }, onSaveOffline = {},
                sleepRemainingMs = 0L, onSleepTimer = {}, offlineSpeedPercent = 100, onOfflineSpeed = {},
                listeningLevelPercent = 100, onListeningLevel = {}, crossfadeSeconds = 0, onCrossfade = {},
                normalizationEnabled = false, onNormalizationChange = {}, onDismiss = {}, reduceMotion = true)
        }
        compose.onNodeWithText("Fixture song 1").assertExists()
        val artBounds = compose.onNodeWithTag("nightwave-now-playing-artwork").fetchSemanticsNode().boundsInRoot
        assertTrue(artBounds.height < artBounds.width)
        compose.onNodeWithTag("nw133-player-content").performScrollToNode(hasTestTag("editorial-track-details"))
        compose.onNodeWithTag("editorial-track-details").performClick()
        compose.onNodeWithTag("editorial-original-title").assertTextEquals(selected.title)
        compose.onNodeWithTag("editorial-original-source").assertTextEquals(selected.url)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("nw133-player-content").performScrollToNode(hasContentDescription("Save track to playlist"))
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
        var choice by mutableIntStateOf(0)
        fixture {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                val mood = DiscoveryShortcuts.picks[choice]
                NightWaveMoodArt(mood.title, mood.subtitle, emptyList(),
                    modifier = Modifier.width(178.dp).testTag("expressive-mood"),
                    onClick = { action = mood.query })
            }
        }
        DiscoveryShortcuts.picks.forEachIndexed { index, mood ->
            compose.runOnIdle { choice = index }
            compose.onNodeWithText(mood.title).assertExists()
            compose.onNodeWithText(mood.subtitle).assertExists()
            val header = compose.onNodeWithTag("mood-art-header", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val art = compose.onNodeWithTag("mood-art-viewport", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val caption = compose.onNodeWithTag("mood-caption", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val card = compose.onNodeWithTag("expressive-mood").fetchSemanticsNode().boundsInRoot
            assertTrue(art.left >= header.left && art.right <= header.right)
            assertTrue(art.top >= header.top && art.bottom <= header.bottom)
            assertTrue(caption.top >= header.bottom && caption.bottom <= card.bottom)
            compose.onNodeWithTag("expressive-mood").performClick()
            compose.runOnIdle { assertEquals(mood.query, action) }; capture("mood-$index")
        }
    }

    @Test fun searchStartsWithRealMusicAndRetainsBrowseRoutes() {
        val index = LocalMusicSearchIndex.build(LocalMusicSnapshot(
            com.nightwave.player.v2.data.room.LegacyLibrarySnapshot(songs.take(3), emptyList(), emptyList(), "fixture"),
            emptyList()))
        fixture {
            StudioSearch(Modifier, StudioSearchSnapshot(), onPrimarySearch = { _, _ -> },
                onNativeSearch = { _, _ -> }, onWebSearch = {}, onPlay = { song, _ -> action = song.url },
                onSongOptions = {}, playingUrl = "", corpus = LocalMusicCorpusState(0L, 0, index),
                onRetryCorpus = {}, onOfflineSearch = { action = "offline" }, searchHistory = emptyList(),
                canUndoSearchRemoval = false, onRecordSearch = {}, onRemoveSearch = {},
                onUndoSearchRemoval = {}, onClearSearch = {})
        }
        compose.onNodeWithText("Fixture song 1").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(songs.first().url, action) }
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(hasText("Offline songs"))
        compose.onNodeWithText("Offline songs").performClick()
        compose.runOnIdle { assertEquals("offline", action) }
        compose.onNodeWithTag("nw129-search-list").performScrollToNode(hasText("Creators"))
        compose.onNodeWithText("Creators").assertExists()
    }

    @Test fun wideSourceArtworkFillsThePreviewWhileRetainingBothEdges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File.createTempFile("nw-content-art-", ".png", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(180, 90, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.GREEN)
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.RED }
            canvas.drawRect(0f, 0f, 18f, 90f, paint)
            paint.color = android.graphics.Color.BLUE
            canvas.drawRect(162f, 0f, 180f, 90f, paint)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            fixture { CoverArt(file.toURI().toString(), Modifier.size(220.dp).testTag("content-art"),
                large = true, trackIdentity = "owned-fixture") }
            compose.waitUntil(5_000) {
                compose.onNodeWithTag("content-art").captureToImage().asAndroidBitmap()
                    .getPixel(110, 110) == android.graphics.Color.GREEN
            }
            val rendered = compose.onNodeWithTag("content-art").captureToImage().asAndroidBitmap()
            assertEquals(android.graphics.Color.RED, rendered.getPixel(8, 110))
            assertEquals(android.graphics.Color.BLUE, rendered.getPixel(211, 110))
            assertTrue(rendered.getPixel(110, 10) != android.graphics.Color.GREEN)
            capture("source-artwork-containment")
        } finally { file.delete() }
    }

    @Test fun shelfEdgeFlingsAndCancelledOrRecoloredPagesCannotStaySplit() {
        val touches = NightWaveShelfGestureState()
        var pager: androidx.compose.foundation.pager.PagerState? = null
        var scope: kotlinx.coroutines.CoroutineScope? = null
        var selectedTheme by mutableStateOf(theme)
        fixture {
            NightWaveMaterialTheme(selectedTheme) {
                val state = androidx.compose.foundation.pager.rememberPagerState { 4 }
                val localScope = rememberCoroutineScope()
                SideEffect { pager = state; scope = localScope }
                NightWavePagerRecovery(state, touches.active)
                CompositionLocalProvider(LocalNightWaveShelfGestures provides touches) {
                androidx.compose.foundation.pager.HorizontalPager(state, Modifier.fillMaxSize().testTag("recovery-pager"), userScrollEnabled = !touches.active) { page ->
                    Column(Modifier.fillMaxSize()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            Modifier.fillMaxWidth().height(96.dp).nightWaveShelfBoundary().testTag("recovery-shelf-$page")) {
                            items(6) { n -> Text("Item $n", Modifier.width(96.dp).height(80.dp)) }
                        }
                        Box(Modifier.fillMaxWidth().weight(1f).testTag("recovery-blank-$page"))
                    }
                }
                }
            }
        }
        compose.onNodeWithTag("recovery-shelf-0").performTouchInput { down(center) }
        compose.runOnIdle { assertTrue(touches.active) }
        compose.onNodeWithTag("recovery-shelf-0").performTouchInput { up() }
        compose.runOnIdle { assertFalse(touches.active) }
        repeat(6) { compose.onNodeWithTag("recovery-shelf-0").performTouchInput { swipeLeft() } }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0, requireNotNull(pager).currentPage)
            assertTrue(kotlin.math.abs(requireNotNull(pager).currentPageOffsetFraction) <= .001f)
        }
        compose.onNodeWithTag("recovery-blank-0").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, requireNotNull(pager).settledPage) }
        compose.runOnIdle {
            requireNotNull(scope).launch { requireNotNull(pager).scrollToPage(1, .35f) }
            selectedTheme = NightWaveTheme.entries[(theme.ordinal + 1) % NightWaveTheme.entries.size]
        }
        compose.waitUntil(5_000) {
            val state = requireNotNull(pager)
            !state.isScrollInProgress && kotlin.math.abs(state.currentPageOffsetFraction) <= .001f
        }
        compose.runOnIdle { assertEquals(1, requireNotNull(pager).settledPage) }; capture("pager-recovery")
    }

}
