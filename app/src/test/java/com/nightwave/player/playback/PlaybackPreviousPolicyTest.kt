package com.nightwave.player.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stateless navigation fence: never rewind an unrelated, stale MediaItem. */
class PlaybackPreviousPolicyTest {
    private val song = "https://www.youtube.com/watch?v=01234567890"
    private val another = "https://www.youtube.com/watch?v=abcdefghijk"

    @Test fun repeatPreviousOnTheSameLoadedSongRewindsWithoutResolvingAgain() {
        assertTrue(PlaybackPreviousPolicy.canRestartLoaded(song, song, song, false))
    }

    @Test fun movingToAnActualPreviousTrackMustUseSelectionPipeline() {
        assertFalse(PlaybackPreviousPolicy.canRestartLoaded(song, another, song, false))
        assertFalse(PlaybackPreviousPolicy.canRestartLoaded(song, another, another, false))
    }

    @Test fun errorOrMismatchedLoadedMediaNeverGetsRewound() {
        assertFalse(PlaybackPreviousPolicy.canRestartLoaded(song, song, another, false))
        assertFalse(PlaybackPreviousPolicy.canRestartLoaded(song, song, null, false))
        assertFalse(PlaybackPreviousPolicy.canRestartLoaded(song, song, song, true))
        assertFalse(PlaybackPreviousPolicy.canRestartLoaded("", "", "", false))
    }
}
