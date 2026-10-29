package com.nightwave.player.offline

import com.nightwave.player.playback.PlaybackQueue
import com.nightwave.player.v2.playback.StudioPlayerState
import org.junit.Assert.*
import org.junit.Test

class OfflineFinishPolicyTest {
    @Test fun ratesWarmUpAndAverageObservedBytes() {
        val window = DownloadRateWindow()
        assertNull(window.observe(0, 0))
        assertNull(window.observe(100_000, 1000))
        assertEquals(100_000L, window.observe(300_000, 3000))
        assertEquals(100_000L, window.observe(400_000, 4000))
        assertFalse(window.canEstimate(4000))
    }
    @Test fun retainedBytesNeverInflateNetworkSpeed() {
        val window = DownloadRateWindow()
        window.observe(0, 0)
        assertNull(window.observe(1_048_576, 1000, 1_048_576))
        assertEquals(100_000L, window.observe(1_348_576, 4000, 1_048_576))
    }
    @Test fun stallsAndRestartsInvalidateOldRates() {
        val window = DownloadRateWindow()
        window.observe(0, 0); window.observe(300_000, 3000)
        assertNull(window.observe(300_000, 12_000))
        assertNull(window.observe(0, 13_000))
        assertFalse(window.canEstimate(13_000))
    }
    @Test fun stableWindowAllowsEtaOnlyAfterWarmupAndMeaningfulBytes() {
        val window = DownloadRateWindow()
        for (second in 0L..12L) window.observe(second * 100_000, second * 1000)
        assertTrue(window.canEstimate(12_000))
        val trickle = DownloadRateWindow()
        for (second in 0L..12L) trickle.observe(second, second * 1000)
        assertFalse(trickle.canEstimate(12_000))
    }
    @Test fun reversedClockCannotManufactureSpeed() {
        val window = DownloadRateWindow()
        window.observe(0, 10_000)
        assertNull(window.observe(100_000, 9000))
    }
    @Test fun bufferingKeepsPauseWithoutClaimingAudiblePlayback() {
        val state = StudioPlayerState(videoUrl="https://www.youtube.com/watch?v=abcdefghijk",
            playing=false, buffering=true, playRequested=true)
        assertTrue(state.canPause)
        assertFalse(state.playing)
        assertFalse(state.copy(playRequested=false).canPause)
        assertFalse(state.copy(error="Unavailable").canPause)
        assertFalse(state.copy(videoUrl="").canPause)
    }
    @Test fun deletingSelectedOfflineItemNeverAutoplaysAnother() {
        val queue = OfflinePlaybackQueue()
        queue.select(listOf("a","b","c"), "b")
        queue.retain(setOf("a","c"))
        assertNull(queue.current)
        assertTrue(queue.snapshot().isEmpty())
        assertNull(queue.next(false))
    }
    @Test fun removingQueuedFilesPreservesOrderSelectionAndRepeat() {
        val queue = OfflinePlaybackQueue()
        queue.select(listOf("a","b","c","d"), "c")
        queue.setRepeat(PlaybackQueue.Repeat.ALL)
        queue.retain(setOf("b","c","d"))
        assertEquals(listOf("b","c","d"), queue.snapshot())
        assertEquals("c", queue.current)
        assertEquals("d", queue.next(false))
        assertEquals("b", queue.next(false))
    }
    @Test fun shuffleVisitsAreRemappedRatherThanRerolled() {
        val queue = OfflinePlaybackQueue { 0 }
        queue.select(listOf("a","b","c","d"), "a")
        queue.setShuffle(true)
        assertEquals("b", queue.next(false))
        assertEquals("c", queue.next(false))
        queue.retain(setOf("a","c","d"))
        assertEquals("c", queue.current)
        assertEquals("a", queue.previous())
        assertEquals("c", queue.next(false))
        assertEquals("d", queue.next(false))
    }
    @Test fun identityRoundtripPreservesUnicodeAndSource() {
        val identity = OfflineTrackIdentityPolicy.from("https://www.youtube.com/watch?v=abcdefghijk",
            "বাংলা গান (Official Audio)", "শিল্পী")!!
        assertEquals(identity, OfflineTrackIdentityPolicy.decode(OfflineTrackIdentityPolicy.encode(identity)))
        assertNull(OfflineTrackIdentityPolicy.from("https://cdn.googlevideo.com/audio?token=secret", "song", "creator"))
    }
    @Test fun optionalMetadataRejectsInvalidOrOversizedPayloads() {
        assertNull(OfflineTrackIdentityPolicy.decode("not json"))
        assertNull(OfflineTrackIdentityPolicy.decode("x".repeat(5000)))
        assertNull(OfflineTrackIdentityPolicy.from("https://www.youtube.com/watch?v=abcdefghijk", "\u0000\n", "author"))
        val bounded = OfflineTrackIdentityPolicy.from("https://www.youtube.com/watch?v=abcdefghijk",
            "x".repeat(1000), "y".repeat(1000))!!
        assertEquals(320, bounded.title.length)
        assertEquals(180, bounded.creator.length)
    }
}
