package com.nightwave.player.v2.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotlightResumePolicyTest {
    private fun result(current: String = "song", tapped: String = "song",
        index: Int = 0, offline: Boolean = false, playing: Boolean = false,
        buffering: Boolean = false, error: Boolean = false) =
        SpotlightResumePolicy.choose(tapped, current, index, offline, playing, buffering, error)

    @Test fun alreadyPlayingNeverStartsNewStream() {
        assertEquals(SpotlightAction.KEEP_CURRENT, result(playing = true))
    }
    @Test fun stillResolvingNeverRestartsExtractor() {
        assertEquals(SpotlightAction.KEEP_CURRENT, result(buffering = true))
    }
    @Test fun pausedCurrentTrackResumesInsteadOfReplacingQueue() {
        assertEquals(SpotlightAction.RESUME_CURRENT, result())
    }
    @Test fun failedCurrentStreamUsesExplicitRetry() {
        assertEquals(SpotlightAction.RETRY_CURRENT, result(error = true))
    }
    @Test fun differentTrackMayReplaceQueue() {
        assertEquals(SpotlightAction.START_SELECTED, result(tapped = "different"))
    }
    @Test fun offlinePlaybackIsNotMistakenForYoutubeStream() {
        assertEquals(SpotlightAction.START_SELECTED, result(offline = true))
    }
    @Test fun noSavedQueueIsNotResumed() {
        assertEquals(SpotlightAction.START_SELECTED, result(index = -1))
    }
    @Test fun emptyUrlCannotResume() {
        assertEquals(SpotlightAction.START_SELECTED, result(tapped = ""))
    }
}
