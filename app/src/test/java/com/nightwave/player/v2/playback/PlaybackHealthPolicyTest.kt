package com.nightwave.player.v2.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHealthPolicyTest {
    private fun state(
        track: Boolean = true,
        connected: Boolean = true,
        offline: Boolean = false,
        playing: Boolean = false,
        buffering: Boolean = false,
        resolving: Boolean = false,
        error: String? = null
    ) = PlaybackHealthPolicy.classify(track, connected, offline,
        playing, buffering, resolving, error)

    @Test fun disconnectionTakesPriorityOverAllMediaSignals() {
        val result = state(connected = false, playing = true, resolving = true)
        assertEquals(PlaybackPhase.DISCONNECTED, result.phase)
        assertTrue(result.needsUserAction)
    }

    @Test fun errorTakesPriorityOverBuffering() {
        val result = state(buffering = true, error = "Retry playback")
        assertEquals(PlaybackPhase.ERROR, result.phase)
        assertTrue(result.needsUserAction)
    }

    @Test fun genuinelyEmptyConnectedPlayerIsIdle() {
        val result = state(track = false)
        assertEquals(PlaybackPhase.IDLE, result.phase)
        assertFalse(result.needsUserAction)
    }

    @Test fun publicStreamLookupIsNotMisreportedAsBuffering() {
        val result = state(buffering = true, resolving = true)
        assertEquals(PlaybackPhase.LOOKING_UP, result.phase)
        assertEquals("Finding audio", result.title)
    }

    @Test fun offlineAudioNeverPretendsToUsePublicExtractor() {
        val result = state(offline = true, buffering = true, resolving = true)
        assertEquals(PlaybackPhase.BUFFERING, result.phase)
        assertTrue(result.explanation.contains("local audio"))
    }

    @Test fun actualMedia3BufferingIsSeparateFromLookup() {
        assertEquals(PlaybackPhase.BUFFERING,
            state(buffering = true, resolving = false).phase)
    }

    @Test fun playAndPauseAreBasedOnRealMediaState() {
        assertEquals(PlaybackPhase.PLAYING, state(playing = true).phase)
        assertEquals(PlaybackPhase.PAUSED, state().phase)
        assertFalse(state().needsUserAction)
    }

    @Test fun errorsDoNotLeakProviderOrSongDetails() {
        val result = state(error = "private-link=https://secret.example")
        assertFalse(result.explanation.contains("secret.example"))
        assertFalse(result.title.contains("secret.example"))
    }
}
