package com.nightwave.player.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTransitionPolicyTest {
    private val online = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val offline = "offline:private-id"

    private fun allow(
        ended: PlaybackTransitionPolicy.Ended = PlaybackTransitionPolicy.Ended(7, online, false),
        generation: Int = 7,
        loaded: String? = online,
        selected: String? = online,
        offlineMode: Boolean = false,
        isEnded: Boolean = true,
        timerPaused: Boolean = false,
        handled: Boolean = false
    ): Boolean = PlaybackTransitionPolicy.shouldAdvance(
        ended, generation, loaded, selected, offlineMode, isEnded, timerPaused, handled
    )

    @Test fun normalOnlineEndMayAdvance() { assertTrue(allow()) }
    @Test fun normalOfflineEndMayAdvance() {
        assertTrue(allow(
            ended = PlaybackTransitionPolicy.Ended(7, offline, true),
            loaded = offline, selected = offline, offlineMode = true))
    }
    @Test fun oldGenerationNeverSkipsNewSelection() {
        assertFalse(allow(generation = 8))
    }
    @Test fun newlyLoadedDifferentTrackIsProtected() {
        assertFalse(allow(loaded = "https://www.youtube.com/watch?v=9bZkp7q19f0"))
    }
    @Test fun userSelectedDifferentQueueEntryIsProtected() {
        assertFalse(allow(selected = "https://www.youtube.com/watch?v=9bZkp7q19f0"))
    }
    @Test fun queuedOfflineEndCannotSkipOnlineAudio() {
        assertFalse(allow(ended = PlaybackTransitionPolicy.Ended(7, offline, true)))
    }
    @Test fun queuedOnlineEndCannotSkipOfflineAudio() {
        assertFalse(allow(offlineMode = true))
    }
    @Test fun callbacksForNonEndedPlayerCannotAdvance() {
        assertFalse(allow(isEnded = false))
    }
    @Test fun sleepTimerExpiredPreventsAdvance() {
        assertFalse(allow(timerPaused = true))
    }
    @Test fun duplicatePostedEndCanAdvanceOnlyOnce() {
        assertFalse(allow(handled = true))
    }
    @Test fun missingMediaAndSelectionAreRejected() {
        assertFalse(allow(ended = PlaybackTransitionPolicy.Ended(7, null, false),
            loaded = null, selected = null))
        assertFalse(allow(loaded = null))
        assertFalse(allow(selected = null))
    }
    @Test fun emptyMediaIdIsRejected() {
        assertFalse(allow(ended = PlaybackTransitionPolicy.Ended(7, "", false),
            loaded = "", selected = ""))
    }
}
