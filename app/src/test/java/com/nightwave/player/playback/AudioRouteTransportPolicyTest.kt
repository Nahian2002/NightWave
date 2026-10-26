package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/** Media buttons must not override a user's pause during public extraction. */
class AudioRouteTransportPolicyTest {
    private val p = AudioRouteTransportPolicy

    @Test fun headsetTogglePausesPendingPublicLookup() {
        assertEquals(p.Action.PAUSE_PENDING,
            p.decide(p.Key.TOGGLE, true, false, false, true))
    }

    @Test fun headsetToggleResumesPausedPublicLookup() {
        assertEquals(p.Action.RESUME_SAVED,
            p.decide(p.Key.TOGGLE, true, false, false, false))
    }

    @Test fun explicitPlayDuringPendingLookupIsNeverInterpretedAsPause() {
        assertEquals(p.Action.RESUME_SAVED,
            p.decide(p.Key.PLAY, true, false, false, true))
    }

    @Test fun explicitPauseDuringPendingLookupNeverResolvesAgain() {
        assertEquals(p.Action.PAUSE_PENDING,
            p.decide(p.Key.PAUSE, true, false, false, true))
        assertEquals(p.Action.DELEGATE,
            p.decide(p.Key.PAUSE, true, false, false, false))
    }

    @Test fun loadedMediaAndOfflineControlsStayWithMedia3() {
        for (key in p.Key.values()) {
            assertEquals(p.Action.DELEGATE,
                p.decide(key, true, true, false, true))
            assertEquals(p.Action.DELEGATE,
                p.decide(key, true, false, true, true))
        }
    }

    @Test fun emptyQueueDoesNotStartAnUnrelatedSong() {
        for (key in p.Key.values()) {
            assertEquals(p.Action.DELEGATE,
                p.decide(key, false, false, false, true))
        }
    }
}
