package com.nightwave.player.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryPolicyTest {
    private val first = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val second = "https://www.youtube.com/watch?v=9bZkp7q19f0"

    private fun schedule(
        selected: String? = first,
        loaded: String? = first,
        offline: Boolean = false,
        timerPaused: Boolean = false,
        wantsPlayback: Boolean = true,
        alreadyRetried: Boolean = false,
        retriableError: Boolean = true,
        resolvingUrl: String? = null
    ) = PlaybackRecoveryPolicy.mayAutoRetry(
        expectedUrl = first,
        selectedUrl = selected,
        loadedMediaId = loaded,
        offline = offline,
        timerPaused = timerPaused,
        wantsPlayback = wantsPlayback,
        alreadyRetried = alreadyRetried,
        retriableError = retriableError,
        resolvingUrl = resolvingUrl
    )

    private fun dispatch(
        generation: Int = 12,
        selected: String? = first,
        loaded: String? = first,
        offline: Boolean = false,
        timerPaused: Boolean = false,
        wantsPlayback: Boolean = true,
        retryMarker: String? = first,
        resolvingUrl: String? = null
    ) = PlaybackRecoveryPolicy.acceptsPostedRetry(
        expectedUrl = first,
        scheduledGeneration = 12,
        currentGeneration = generation,
        selectedUrl = selected,
        loadedMediaId = loaded,
        offline = offline,
        timerPaused = timerPaused,
        wantsPlayback = wantsPlayback,
        retryMarkerUrl = retryMarker,
        resolvingUrl = resolvingUrl
    )

    @Test fun transientNetworkErrorCanScheduleOneRefresh() {
        assertTrue(schedule())
        assertTrue(dispatch())
    }

    @Test fun userPauseBeforeErrorPreventsAutomaticRestart() {
        assertFalse(schedule(wantsPlayback = false))
    }

    @Test fun userPauseAfterErrorButBeforePostedCallbackPreventsRestart() {
        assertFalse(dispatch(wantsPlayback = false))
    }

    @Test fun sleepTimerExpiryBlocksBothSchedulingAndDispatch() {
        assertFalse(schedule(timerPaused = true))
        assertFalse(dispatch(timerPaused = true))
    }

    @Test fun anotherSelectionOrLoadedItemMustNeverBeRetried() {
        assertFalse(schedule(selected = second))
        assertFalse(schedule(loaded = second))
        assertFalse(dispatch(selected = second))
        assertFalse(dispatch(loaded = second))
    }

    @Test fun offlineSwitchAndNewerGenerationInvalidateOldCallback() {
        assertFalse(schedule(offline = true))
        assertFalse(dispatch(offline = true))
        assertFalse(dispatch(generation = 13))
        assertFalse(dispatch(generation = 11))
    }

    @Test fun noRetryLoopAndOnlyRetriableTransportErrors() {
        assertFalse(schedule(alreadyRetried = true))
        assertFalse(schedule(retriableError = false))
        assertFalse(dispatch(retryMarker = null))
        assertFalse(dispatch(retryMarker = second))
    }

    @Test fun concurrentLookupPreventsDuplicateRequest() {
        assertFalse(schedule(resolvingUrl = first))
        assertFalse(schedule(resolvingUrl = second))
        assertFalse(dispatch(resolvingUrl = first))
    }

    @Test fun onlyExactCanonicalResolvedVideoMayReplaceSelection() {
        assertTrue(PlaybackRecoveryPolicy.acceptsResolvedTrack(first, first))
        assertFalse(PlaybackRecoveryPolicy.acceptsResolvedTrack(first, second))
        assertFalse(PlaybackRecoveryPolicy.acceptsResolvedTrack(first, first + "&token=secret"))
        assertFalse(PlaybackRecoveryPolicy.acceptsResolvedTrack(
            first, "https://rr.example.googlevideo.com/audio?sig=secret"))
    }
}
