package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackQueueManualNextAvailabilityTest {
    private fun track(i: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
        "Song $i", "Artist", "")
    private fun linear(): PlaybackQueue = PlaybackQueue(pick = { 0 }).apply {
        replace((1..4).map(::track), 0)
    }

    @Test fun noSelectionOrEndedLinearQueueCannotAcknowledgeNext() {
        val q = PlaybackQueue()
        assertFalse(q.hasNext())
        val linear = linear()
        assertTrue(linear.hasNext())
        linear.jump(3)
        assertFalse(linear.hasNext())
        assertNull(linear.next())
    }

    @Test fun repeatAllAllowsWrapButRepeatOneDoesNotForceManualReplay() {
        val q = linear()
        q.jump(3)
        q.repeat = PlaybackQueue.Repeat.ONE
        assertFalse(q.hasNext())
        q.repeat = PlaybackQueue.Repeat.ALL
        assertTrue(q.hasNext())
        assertEquals(track(1), q.next())
    }

    @Test fun shuffleExhaustionDoesNotAdvertiseAnotherRandomSong() {
        val q = linear()
        q.shuffle = true
        repeat(3) {
            assertTrue(q.hasNext())
            assertNotNull(q.next())
        }
        assertFalse(q.hasNext())
        assertNull(q.next())
        q.repeat = PlaybackQueue.Repeat.ALL
        assertTrue(q.hasNext())
        assertNotNull(q.next())
    }

    @Test fun shuffledPreviousMakesForwardRetraceAvailableWithoutRandomReroll() {
        val q = linear()
        q.shuffle = true
        val second = q.next()
        assertEquals(track(1), q.previous())
        val state = q.shuffleRecoveryState()
        assertTrue(q.hasNext())
        assertTrue(q.hasNext())
        assertEquals(state, q.shuffleRecoveryState())
        assertEquals(second, q.next())
    }

    @Test fun playNextPriorityIsNotConsumedByAvailabilityProbe() {
        val q = linear()
        q.shuffle = true
        assertTrue(q.insertNext(track(10)))
        val snapshot = q.snapshot()
        val priority = q.priorityNextUrls()
        val history = q.shuffleRecoveryState()
        repeat(5) { assertTrue(q.hasNext()) }
        assertEquals(snapshot, q.snapshot())
        assertEquals(priority, q.priorityNextUrls())
        assertEquals(history, q.shuffleRecoveryState())
        assertEquals(track(10), q.next())
    }

    @Test fun oneSongShuffleDoesNotCreatePhantomNextUnlessRepeatAll() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace(listOf(track(1)), 0)
        q.shuffle = true
        assertFalse(q.hasNext())
        q.repeat = PlaybackQueue.Repeat.ALL
        assertTrue(q.hasNext())
        assertEquals(track(1), q.next())
    }
}
