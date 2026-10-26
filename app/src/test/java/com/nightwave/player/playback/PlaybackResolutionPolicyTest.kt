package com.nightwave.player.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResolutionPolicyTest {
    private val a = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val b = "https://www.youtube.com/watch?v=9bZkp7q19f0"

    @Test fun onlyMatchingErrorFreeLoadedTrackIsReusable() {
        assertTrue(PlaybackResolutionPolicy.reusesLoadedTrack(a, a, false))
        assertFalse(PlaybackResolutionPolicy.reusesLoadedTrack(a, b, false))
        assertFalse(PlaybackResolutionPolicy.reusesLoadedTrack(a, null, false))
        assertFalse(PlaybackResolutionPolicy.reusesLoadedTrack(a, a, true))
    }

    @Test fun pendingDuplicateIsSuppressedOnlyForSamePublicUrl() {
        assertTrue(PlaybackResolutionPolicy.awaitsSameResolution(a, a))
        assertFalse(PlaybackResolutionPolicy.awaitsSameResolution(a, b))
        assertFalse(PlaybackResolutionPolicy.awaitsSameResolution(null, a))
        assertFalse(PlaybackResolutionPolicy.awaitsSameResolution(a, null))
    }

    @Test fun explicitSameQueuePlayResumesPausedLookup() {
        assertTrue(PlaybackResolutionPolicy.shouldResumePendingExplicitPlay(
            true, true, false, true, false))
    }

    @Test fun queueOnlyAndStaleSelectionMustNeverResumeLookup() {
        assertFalse(PlaybackResolutionPolicy.shouldResumePendingExplicitPlay(
            false, true, false, true, false))
        assertFalse(PlaybackResolutionPolicy.shouldResumePendingExplicitPlay(
            true, false, false, true, false))
    }

    @Test fun loadedOrAlreadyIntendedPlaybackSkipsRedundantPlay() {
        assertFalse(PlaybackResolutionPolicy.shouldResumePendingExplicitPlay(
            true, true, true, true, false))
        assertFalse(PlaybackResolutionPolicy.shouldResumePendingExplicitPlay(
            true, true, false, true, true))
        assertFalse(PlaybackResolutionPolicy.shouldResumePendingExplicitPlay(
            true, true, false, false, false))
    }

    @Test fun staleGenerationCannotInstallAnotherTrack() {
        assertTrue(PlaybackResolutionPolicy.acceptsResult(11, 11, a, a, false))
        assertFalse(PlaybackResolutionPolicy.acceptsResult(10, 11, a, a, false))
        assertFalse(PlaybackResolutionPolicy.acceptsResult(11, 11, a, b, false))
        assertFalse(PlaybackResolutionPolicy.acceptsResult(11, 11, a, a, true))
        assertFalse(PlaybackResolutionPolicy.acceptsResult(11, 11, a, null, false))
    }

    @Test fun timeoutIsBoundedAndNeverFiresEarly() {
        val max = PlaybackResolutionPolicy.RESOLUTION_TIMEOUT_MS
        assertEquals(35_000L, max)
        assertFalse(PlaybackResolutionPolicy.timedOut(1_000L, 1_000L + max - 1L))
        assertTrue(PlaybackResolutionPolicy.timedOut(1_000L, 1_000L + max))
        assertFalse(PlaybackResolutionPolicy.timedOut(1_000L, 0L))
    }

    @Test fun monotonicTimeoutUnaffectedByWallDateOrTimeZone() {
        assertFalse(PlaybackResolutionPolicy.timedOut(800_000L, 800_001L))
        assertTrue(PlaybackResolutionPolicy.timedOut(800_000L, 835_000L))
    }
}
