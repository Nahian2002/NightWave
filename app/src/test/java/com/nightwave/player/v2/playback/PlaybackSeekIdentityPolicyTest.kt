package com.nightwave.player.v2.playback

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
}
