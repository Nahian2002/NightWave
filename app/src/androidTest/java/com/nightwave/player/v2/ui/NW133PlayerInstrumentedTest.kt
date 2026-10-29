package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.playback.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual player screens, blank art and synthetic callbacks; no Media3, provider, notification or personal store. */
@RunWith(AndroidJUnit4::class)
class NW133PlayerInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private var state by mutableStateOf(StudioPlayerState(connected = true, title = "Fixture song",
        artist = "Fixture uploader", videoUrl = "https://www.youtube.com/watch?v=00000000001", durationMs = 120_000L))
    private class Actions : StudioPlayerActions {
        val seeks = mutableListOf<Pair<String, Long>>()
        var retryCount = 0
        var mutations = 0
        override fun play(video: VideoPreview, collection: List<VideoPreview>) { mutations++ }
        override fun playPause() { mutations++ }
        override fun retry() { retryCount++ }
        override fun enqueue(video: VideoPreview) { mutations++ }
        override fun previous() { mutations++ }
        override fun next() { mutations++ }
        override fun seekTo(positionMs: Long) { error("Unscoped seek must never be used") }
        override fun seekToForTrack(expectedMediaId: String, positionMs: Long) { seeks += expectedMediaId to positionMs }
        override fun toggleShuffle() { mutations++ }
        override fun cycleRepeat() { mutations++ }
        override fun jumpTo(index: Int) { mutations++ }
        override fun removeUpcoming(index: Int, expectedUrl: String): String? = null
        override fun undoUpcoming(token: String) { mutations++ }
        override fun refreshQueue() { mutations++ }
    }
    private val actions = Actions()
    private var queueOpened = 0
    private var importedLyrics by mutableStateOf<com.nightwave.player.v2.lyrics.TimedLyrics?>(null)
    private var lyricImports = 0
    private fun player() {
        compose.setContent {
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                StudioNowPlayingOverlay(expanded = true, state = state, actions = actions, reduceMotion = true,
                    liked = false, onLike = {}, onOpenQueue = { queueOpened++ }, onSaveToPlaylist = {}, onSaveOffline = {},
                    sleepRemainingMs = 0L, onSleepTimer = {}, offlineSpeedPercent = 100, onOfflineSpeed = {},
                    listeningLevelPercent = 100, onListeningLevel = {}, crossfadeSeconds = 0, onCrossfade = {},
                    normalizationEnabled = false, onNormalizationChange = {}, onDismiss = {},
                    lyrics = importedLyrics, onImportLyrics = { lyricImports++ })
            }
        }
    }
    private fun scroll(tag: String) = compose.onNodeWithTag("nw133-player-content").performScrollToNode(hasTestTag(tag))

    @Test fun publicVideoOffersOwnedImportWithoutStartingAnyProviderOrPlayback() {
        player(); scroll("nightwave-now-playing-lyrics-action")
        compose.onNodeWithTag("nightwave-now-playing-lyrics-action").performClick()
        compose.onNodeWithTag("nw133-player-content").performScrollToNode(hasText("Import synchronized lyrics"))
        compose.onNodeWithText("Import synchronized lyrics").performClick()
        compose.runOnIdle { assertEquals(1, lyricImports); assertEquals(0, actions.mutations) }
    }
    @Test fun publicOwnedLyricsUseTheSameSourceGuardedCueSeek() {
        importedLyrics = com.nightwave.player.v2.lyrics.TimedLyricsCodec.parse("[00:01.00] Owned words".toByteArray())
        player(); scroll("nightwave-now-playing-lyrics-action")
        compose.onNodeWithTag("nightwave-now-playing-lyrics-action").performClick()
        compose.onNodeWithTag("nw133-player-content").performScrollToNode(hasText("Open full-screen synchronized lyrics"))
        compose.onNodeWithText("Open full-screen synchronized lyrics").performClick()
        compose.onNodeWithTag("lyrics-cue-0").performClick()
        compose.runOnIdle { assertEquals(listOf(state.videoUrl to 1000L), actions.seeks); assertEquals(0, actions.mutations) }
    }

    @Test fun miniAnnouncesBufferingForSameSelectedTitleWithoutStartingPlayback() {
        state = state.copy(playing = true, buffering = true)
        compose.setContent {
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                StudioMiniPlayer(state, actions, onExpand = {}, onHide = {})
            }
        }
        compose.onNodeWithTag("nightwave-mini-player")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Buffering audio"))
        compose.onNodeWithText("Fixture song").assertExists()
        compose.runOnIdle { assertEquals(0, actions.mutations) }
    }
    @Test fun resolvingDisablesSeekAndShowsCurrentStatus() {
        state = state.copy(resolving = true)
        player(); scroll("nw133-player-status")
        compose.onNodeWithText("Finding audio").assertExists()
        scroll("nightwave-player-seek")
        compose.onNodeWithTag("nightwave-player-seek").assertIsNotEnabled()
        compose.runOnIdle { assertTrue(actions.seeks.isEmpty()) }
    }
    @Test fun accessibilitySeekCommitsExactlyCurrentUrlAndCurrentDurationAfterTrackChange() {
        player(); scroll("nightwave-player-seek")
        compose.onNodeWithTag("nightwave-player-seek").performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(.25f)) }
        compose.runOnIdle { assertEquals(listOf(state.videoUrl to 30_000L), actions.seeks) }
        compose.runOnIdle { state = state.copy(videoUrl = "https://www.youtube.com/watch?v=00000000002", title = "Another fixture", durationMs = 100_000L) }
        compose.onNodeWithTag("nightwave-player-seek").performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(.5f)) }
        compose.runOnIdle { assertEquals(state.videoUrl to 50_000L, actions.seeks.last()); assertEquals(2, actions.seeks.size) }
    }
    @Test fun failureOffersRetryWithoutChangingSelectedSongOrQueue() {
        state = state.copy(error = "Fixture playback failure")
        player(); scroll("nw133-player-status")
        compose.onNodeWithText("Playback unavailable").assertExists()
        compose.onNodeWithContentDescription("Retry playback").performClick()
        compose.runOnIdle { assertEquals(1, actions.retryCount); assertEquals(0, actions.mutations); assertTrue(actions.seeks.isEmpty()) }
    }
    @Test fun queueAndLyricsPathsRemainReachableWithoutAutoPlayback() {
        player(); scroll("nightwave-now-playing-lyrics-action")
        compose.onNodeWithTag("nightwave-now-playing-lyrics-action").assertExists()
        scroll("nightwave-now-playing-queue-action")
        compose.onNodeWithTag("nightwave-now-playing-queue-action").performClick()
        compose.runOnIdle { assertEquals(1, queueOpened); assertEquals(0, actions.mutations) }
    }
    @Test fun absentArtworkHasAnHonestAccessibleFallback() {
        compose.setContent {
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                CoverArt("", Modifier.size(240.dp), description = "Artwork for fixture", trackIdentity = "fixture", large = true)
            }
        }
        compose.onNodeWithContentDescription("Artwork for fixture · image unavailable").assertExists()
    }
}
