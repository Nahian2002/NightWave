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

    @Test fun transientHttpResponsesPermitOneGuardedStreamRefresh() {
        for (status in listOf(408, 425, 429, 500, 502, 503, 504)) {
            assertTrue("HTTP $status is plausibly transient",
                PlaybackRecoveryPolicy.retryableHttpStatus(status))
            assertTrue(schedule(retriableError =
                PlaybackRecoveryPolicy.retryableHttpStatus(status)))
        }
    }

    @Test fun permanentSourceFailuresNeverTriggerAutomaticRefresh() {
        for (status in listOf(200, 201, 204, 206, 301, 302, 304, 400, 401,
            402, 403, 404, 405, 410, 416, 418, 422, 451, 501, 505)) {
            assertFalse("HTTP $status must not auto-retry",
                PlaybackRecoveryPolicy.retryableHttpStatus(status))
            assertFalse(schedule(retriableError =
                PlaybackRecoveryPolicy.retryableHttpStatus(status)))
        }
    }

    @Test fun unknownOrMalformedHttpCodeNeverEnablesRefresh() {
        for (status in listOf<Int?>(null, -1, 0, 1, 99, 600, 999)) {
            assertFalse(PlaybackRecoveryPolicy.retryableHttpStatus(status))
        }
    }

    @Test fun transientHttpStillHonorsPauseTimerAndChangedSelectionFences() {
        val retriable = PlaybackRecoveryPolicy.retryableHttpStatus(503)
        assertTrue(retriable)
        assertFalse(schedule(retriableError = retriable, wantsPlayback = false))
        assertFalse(schedule(retriableError = retriable, timerPaused = true))
        assertFalse(schedule(retriableError = retriable, selected = second))
        assertFalse(schedule(retriableError = retriable, loaded = second))
        assertFalse(schedule(retriableError = retriable, alreadyRetried = true))
        assertFalse(schedule(retriableError = retriable, resolvingUrl = first))
    }

    @Test fun retryPolicyDoesNotAuthorizeALatePostedRecovery() {
        assertFalse(dispatch(generation = 13))
        assertFalse(dispatch(wantsPlayback = false))
        assertFalse(dispatch(offline = true))
        assertFalse(dispatch(timerPaused = true))
        assertFalse(dispatch(selected = second))
        assertFalse(dispatch(retryMarker = null))
        assertTrue(dispatch())
    }
}
