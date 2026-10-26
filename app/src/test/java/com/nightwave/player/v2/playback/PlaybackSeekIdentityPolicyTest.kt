package com.nightwave.player.v2.playback

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSeekIdentityPolicyTest {
    @Test fun acceptsSameLoadedPublicTrack() {
        val id = "https://www.youtube.com/watch?v=validtrackid"
        assertTrue(PlaybackSeekIdentityPolicy.accepts(id, id, id))
    }

    @Test fun acceptsSameLoadedOfflineTrack() {
        assertTrue(PlaybackSeekIdentityPolicy.accepts("offline:abc", "offline:abc", "offline:abc"))
    }

    @Test fun rejectsGestureFromPreviousSongAfterQueueChange() {
        assertFalse(PlaybackSeekIdentityPolicy.accepts("offline:first", "offline:second", "offline:second"))
        assertFalse(PlaybackSeekIdentityPolicy.accepts("offline:first", "offline:first", "offline:second"))
    }

    @Test fun blocksMissingControllerAndStaleResolution() {
        assertFalse(PlaybackSeekIdentityPolicy.accepts("track", "track", null))
        assertFalse(PlaybackSeekIdentityPolicy.accepts("", "", ""))
    }

    @Test fun clampsSeekToLoadedTrackKnownDuration() {
        val id = "offline:one"
        assertEquals(0L, PlaybackSeekIdentityPolicy.target(id, id, id, 120_000L, -500L))
        assertEquals(49_000L, PlaybackSeekIdentityPolicy.target(id, id, id, 120_000L, 49_000L))
        assertEquals(120_000L, PlaybackSeekIdentityPolicy.target(id, id, id, 120_000L, Long.MAX_VALUE))
    }

    @Test fun staleSeekCannotTouchNewTrackAfterRapidNextOrJump() {
        val oldId = "offline:old"
        val newId = "offline:new"
        assertNull(PlaybackSeekIdentityPolicy.target(oldId, newId, newId, 120_000L, 40_000L))
        assertNull(PlaybackSeekIdentityPolicy.target(oldId, oldId, newId, 120_000L, 40_000L))
        assertNull(PlaybackSeekIdentityPolicy.target("", "", "", 120_000L, 40_000L))
        assertNull(PlaybackSeekIdentityPolicy.target(null, newId, newId, 120_000L, 40_000L))
    }

    @Test fun noSeekOnErroredPlayerOrUnresolvedDuration() {
        val id = "offline:abc"
        assertNull(PlaybackSeekIdentityPolicy.target(id, id, id, 0L, 300L))
        assertNull(PlaybackSeekIdentityPolicy.target(id, id, id, -9223372036854775807L, 300L))
        assertNull(PlaybackSeekIdentityPolicy.target(id, id, id, 1000L, 300L, playerHasError = true))
    }
}
