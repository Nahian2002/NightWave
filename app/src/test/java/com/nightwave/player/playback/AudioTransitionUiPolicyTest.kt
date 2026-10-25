package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTransitionUiPolicyTest {
    @Test fun streamingNeverPromisesCrossfadeOrGapless() {
        val message = AudioTransitionUiPolicy.description(
            offline = false, crossfadeSeconds = 7,
            shuffled = false, repeatOne = false, normalSpeed = true)
        assertTrue(message.contains("Streaming playback"))
        assertFalse(message.contains("7s requested"))
    }

    @Test fun offOnlyMentionsGaplessAsEligibilityNotGuarantee() {
        val message = AudioTransitionUiPolicy.description(
            offline = true, crossfadeSeconds = 0,
            shuffled = false, repeatOne = false, normalSpeed = true)
        assertTrue(message.contains("eligible"))
        assertTrue(message.contains("Crossfade off"))
    }

    @Test fun incompatiblePlaybackModesNeverClaimActiveTransition() {
        assertTrue(AudioTransitionUiPolicy.description(
            true, 3, true, false, true).contains("Shuffle"))
        assertTrue(AudioTransitionUiPolicy.description(
            true, 3, false, true, true).contains("Repeat One"))
        assertTrue(AudioTransitionUiPolicy.description(
            true, 3, false, false, false).contains("1×"))
    }

    @Test fun requestedOverlapIsBoundedAndNeedsNextVerifiedFile() {
        assertEquals("10s requested · starts only when the next verified offline track is ready.",
            AudioTransitionUiPolicy.description(true, 999, false, false, true))
        assertEquals("Off · use normal track transitions",
            AudioTransitionUiPolicy.dialogDetail(0))
        assertEquals("7 seconds · eligible local tracks",
            AudioTransitionUiPolicy.dialogDetail(7))
    }
}
