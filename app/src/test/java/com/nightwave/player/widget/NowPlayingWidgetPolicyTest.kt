package com.nightwave.player.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingWidgetPolicyTest {
    @Test fun withoutTrackWidgetAlwaysShowsSafeNoAudioState() {
        val input = WidgetPlaybackSnapshot(false, true, "Ghost song", "Ghost creator")
        assertEquals(WidgetPlaybackSnapshot(), NowPlayingWidgetPolicy.normalized(input))
        assertEquals("Ready when you are", NowPlayingWidgetPolicy.title(input))
        assertEquals("Open NightWave to start listening",
            NowPlayingWidgetPolicy.subtitle(input))
        assertEquals("Open NightWave", NowPlayingWidgetPolicy.playbackDescription(input))
    }

    @Test fun currentPlayingTrackShowsExactTitleAndCreator() {
        val state = WidgetPlaybackSnapshot(true, true, "My Song", "My Artist")
        assertEquals("My Song", NowPlayingWidgetPolicy.title(state))
        assertEquals("My Artist", NowPlayingWidgetPolicy.subtitle(state))
        assertEquals("Pause playback", NowPlayingWidgetPolicy.playbackDescription(state))
    }

    @Test fun pausedTrackAdvertisesResumeWithoutAutoplay() {
        val paused = WidgetPlaybackSnapshot(true, false, "Track", "Artist")
        assertEquals("Resume playback", NowPlayingWidgetPolicy.playbackDescription(paused))
        assertFalse(paused.isPlaying)
    }

    @Test fun whitespaceAndControlCharactersCannotBreakWidgetLayout() {
        val state = WidgetPlaybackSnapshot(true, true, "  He\nllo\tWorld  ", "  Ar\rtist  ")
        val safe = NowPlayingWidgetPolicy.normalized(state)
        assertEquals("HelloWorld", safe.title)
        assertEquals("Artist", safe.artist)
    }

    @Test fun veryLongMetadataIsBoundedBeforeCachingAndRendering() {
        val state = WidgetPlaybackSnapshot(true, false, "x".repeat(400), "a".repeat(300))
        val safe = NowPlayingWidgetPolicy.normalized(state)
        assertEquals(NowPlayingWidgetPolicy.MAX_TITLE, safe.title.length)
        assertEquals(NowPlayingWidgetPolicy.MAX_ARTIST, safe.artist.length)
    }

    @Test fun emptyTrackMetadataHasVisibleFallbacks() {
        val empty = WidgetPlaybackSnapshot(true, true, "  ", "")
        assertEquals("Unknown track", NowPlayingWidgetPolicy.title(empty))
        assertEquals("NightWave audio", NowPlayingWidgetPolicy.subtitle(empty))
    }

    @Test fun renderingPolicyDoesNotMutateSource() {
        val source = WidgetPlaybackSnapshot(true, true, "\tMusic", " Artist ")
        NowPlayingWidgetPolicy.normalized(source)
        assertEquals("\tMusic", source.title)
        assertEquals(" Artist ", source.artist)
    }

    @Test fun playButtonLabelsAlwaysMatchState() {
        val stopped = WidgetPlaybackSnapshot()
        val paused = WidgetPlaybackSnapshot(true, false, "A", "B")
        val playing = paused.copy(isPlaying = true)
        assertEquals("Open NightWave", NowPlayingWidgetPolicy.playbackDescription(stopped))
        assertEquals("Resume playback", NowPlayingWidgetPolicy.playbackDescription(paused))
        assertEquals("Pause playback", NowPlayingWidgetPolicy.playbackDescription(playing))
        assertTrue(playing.hasTrack)
    }
}
