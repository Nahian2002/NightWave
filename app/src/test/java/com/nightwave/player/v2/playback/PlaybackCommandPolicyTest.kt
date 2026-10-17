package com.nightwave.player.v2.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Independent pure guard: MediaController must not play unrelated cached media. */
class PlaybackCommandPolicyTest {
    private val a = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val b = "https://www.youtube.com/watch?v=9bZkp7q19f0"
    private val offline = "offline:12345678-1234-1234-1234-123456789abc.mp3"

    @Test fun exactSavedPublicTrackMayResume() {
        assertTrue(PlaybackCommandPolicy.canResumeLoaded(a, a, false))
    }

    @Test fun differentLoadedSongIsNotReusable() {
        assertFalse(PlaybackCommandPolicy.canResumeLoaded(a, b, false))
    }

    @Test fun localFileNeverPassesPublicTrackResume() {
        assertFalse(PlaybackCommandPolicy.canResumeLoaded(a, offline, false))
    }

    @Test fun publicVideoNeverPassesOfflineTrackResume() {
        assertFalse(PlaybackCommandPolicy.canResumeLoaded(offline, a, false))
    }

    @Test fun erroredLoadedItemMustBeRetriedThroughService() {
        assertFalse(PlaybackCommandPolicy.canResumeLoaded(a, a, true))
    }

    @Test fun missingOrBlankSelectionNeverAutoplaysLoadedMedia() {
        assertFalse(PlaybackCommandPolicy.canResumeLoaded(null, a, false))
        assertFalse(PlaybackCommandPolicy.canResumeLoaded("", a, false))
        assertFalse(PlaybackCommandPolicy.canResumeLoaded(a, null, false))
    }

    @Test fun publicAndOfflineSelectionsAreDisjoint() {
        assertTrue(PlaybackCommandPolicy.isPublicSelection(a))
        assertFalse(PlaybackCommandPolicy.isOfflineSelection(a))
        assertTrue(PlaybackCommandPolicy.isOfflineSelection(offline))
        assertFalse(PlaybackCommandPolicy.isPublicSelection(offline))
    }

    @Test fun unsupportedSelectionDoesNotCountAsSavedMusic() {
        assertFalse(PlaybackCommandPolicy.isPublicSelection("https://example.invalid/foo"))
        assertFalse(PlaybackCommandPolicy.isOfflineSelection(null))
    }
    @Test fun homeResumeIsOnlyIdempotentForTheExactPlayingSelection() {
        assertTrue(PlaybackCommandPolicy.resumeSatisfied(a, a, false, true))
        assertFalse(PlaybackCommandPolicy.resumeSatisfied(a, b, false, true))
        assertFalse(PlaybackCommandPolicy.resumeSatisfied(a, a, false, false))
        assertFalse(PlaybackCommandPolicy.resumeSatisfied(a, a, true, true))
        assertFalse(PlaybackCommandPolicy.resumeSatisfied(a, null, false, true))
    }

    @Test fun homeResumeRejectsCrossSourceCurrentlyPlayingTrack() {
        assertFalse(PlaybackCommandPolicy.resumeSatisfied(a, offline, false, true))
        assertFalse(PlaybackCommandPolicy.resumeSatisfied(offline, a, false, true))
        assertTrue(PlaybackCommandPolicy.resumeSatisfied(offline, offline, false, true))
    }
}
