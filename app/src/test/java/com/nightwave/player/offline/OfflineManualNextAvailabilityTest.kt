package com.nightwave.player.offline

import com.nightwave.player.playback.PlaybackQueue
import org.junit.Assert.*
import org.junit.Test

class OfflineManualNextAvailabilityTest {
    private fun queue() = OfflinePlaybackQueue(pick = { 0 }).apply {
        select(listOf("a", "b", "c", "d"), "a")
    }

    @Test fun emptyQueueAndEndedRepeatOffCannotNavigate() {
        assertFalse(OfflinePlaybackQueue().hasNext())
        val q = queue()
        q.jump(3)
        assertFalse(q.hasNext())
        assertNull(q.next(false))
    }

    @Test fun repeatAllAtEndIsAvailableButManualRepeatOneDoesNotReplay() {
        val q = queue()
        q.jump(3)
        q.setRepeat(PlaybackQueue.Repeat.ONE)
        assertFalse(q.hasNext())
        q.setRepeat(PlaybackQueue.Repeat.ALL)
        assertTrue(q.hasNext())
        assertEquals("a", q.next(false))
    }

    @Test fun shuffleCycleExhaustionAndRepeatAllAreHonest() {
        val q = queue()
        q.setShuffle(true)
        repeat(3) { assertTrue(q.hasNext()); assertNotNull(q.next(false)) }
        assertFalse(q.hasNext())
        assertNull(q.next(false))
        q.setRepeat(PlaybackQueue.Repeat.ALL)
        assertTrue(q.hasNext())
        assertNotNull(q.next(false))
    }

    @Test fun previousForwardRedoDoesNotDisappearOnProbe() {
        val q = queue()
        q.setShuffle(true)
        assertEquals("b", q.next(false))
        assertEquals("a", q.previous())
        val initialIds = q.snapshot()
        repeat(4) { assertTrue(q.hasNext()) }
        assertEquals(initialIds, q.snapshot())
        assertEquals("b", q.next(false))
    }

    @Test fun singleShuffledSongOnlyReplaysWhenRepeatAll() {
        val q = OfflinePlaybackQueue(pick = { 0 })
        q.select(listOf("solo"), "solo")
        q.setShuffle(true)
        assertFalse(q.hasNext())
        q.setRepeat(PlaybackQueue.Repeat.ONE)
        assertFalse(q.hasNext())
        q.setRepeat(PlaybackQueue.Repeat.ALL)
        assertTrue(q.hasNext())
        assertEquals("solo", q.next(false))
    }

    @Test fun branchAfterPreviousCannotResurrectPlayedCandidates() {
        val q = queue()
        q.setShuffle(true)
        assertEquals("b", q.next(false))
        assertEquals("a", q.previous())
        assertEquals("d", q.jump(3))
        assertTrue(q.hasNext())
        assertEquals("c", q.next(false))
        assertFalse(q.hasNext())
        assertNull(q.next(false))
    }
}
