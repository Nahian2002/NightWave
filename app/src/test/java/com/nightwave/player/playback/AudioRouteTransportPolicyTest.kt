package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/** Media buttons must not override a user's pause during public extraction. */
class AudioRouteTransportPolicyTest {
    private val p = AudioRouteTransportPolicy

    @Test fun headsetTogglePausesPendingPublicLookup() {
        assertEquals(AudioRouteTransportPolicy.Action.PAUSE_PENDING,
            p.decide(AudioRouteTransportPolicy.Key.TOGGLE, true, false, false, true))
    }

    @Test fun headsetToggleResumesPausedPublicLookup() {
        assertEquals(AudioRouteTransportPolicy.Action.RESUME_SAVED,
            p.decide(AudioRouteTransportPolicy.Key.TOGGLE, true, false, false, false))
    }

    @Test fun explicitPlayDuringPendingLookupIsNeverInterpretedAsPause() {
        assertEquals(AudioRouteTransportPolicy.Action.RESUME_SAVED,
            p.decide(AudioRouteTransportPolicy.Key.PLAY, true, false, false, true))
    }

    @Test fun explicitPauseDuringPendingLookupNeverResolvesAgain() {
        assertEquals(AudioRouteTransportPolicy.Action.PAUSE_PENDING,
            p.decide(AudioRouteTransportPolicy.Key.PAUSE, true, false, false, true))
        assertEquals(AudioRouteTransportPolicy.Action.DELEGATE,
            p.decide(AudioRouteTransportPolicy.Key.PAUSE, true, false, false, false))
    }

    @Test fun loadedMediaAndOfflineControlsStayWithMedia3() {
        for (key in AudioRouteTransportPolicy.Key.values()) {
            assertEquals(AudioRouteTransportPolicy.Action.DELEGATE,
                p.decide(key, true, true, false, true))
            assertEquals(AudioRouteTransportPolicy.Action.DELEGATE,
                p.decide(key, true, false, true, true))
        }
    }

    @Test fun emptyQueueDoesNotStartAnUnrelatedSong() {
        for (key in AudioRouteTransportPolicy.Key.values()) {
            assertEquals(AudioRouteTransportPolicy.Action.DELEGATE,
                p.decide(key, false, false, false, true))
        }
    }
}
