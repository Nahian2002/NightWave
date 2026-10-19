package com.nightwave.player.offline

import org.junit.Assert.*
import org.junit.Test

class OfflinePlaybackQueueTest {
    @Test fun selectionRespectsVisibleOrder() {
        val queue = OfflinePlaybackQueue()
        assertEquals("b", queue.select(listOf("c", "b", "a"), "b"))
        assertEquals("a", queue.next())
        assertNull(queue.next())
        assertEquals("b", queue.previous())
        assertEquals("c", queue.previous())
        assertNull(queue.previous())
    }

    @Test fun singleFileHasNoAutomaticRepeat() {
        val queue = OfflinePlaybackQueue()
        assertEquals("only", queue.select(listOf("only"), "only"))
        assertNull(queue.next())
        assertNull(queue.previous())
        assertEquals("only", queue.current)
    }

    @Test fun rejectsMissingSelectionWithoutDestroyingCurrentQueue() {
        val queue = OfflinePlaybackQueue()
        queue.select(listOf("a", "b"), "a")
        assertNull(queue.select(listOf("x", "y"), "missing"))
        assertEquals("a", queue.current)
    }

    @Test fun duplicatesAndBlankEntriesAreIgnored() {
        val queue = OfflinePlaybackQueue()
        queue.select(listOf("", "b", "b", "a", "  "), "b")
        assertEquals(2, queue.size)
        assertEquals("a", queue.next())
    }

    @Test fun jumpHandlesBoundsWithoutDamagingSelection() {
        val queue = OfflinePlaybackQueue()
        queue.select(listOf("a", "b", "c"), "a")
        assertEquals("c", queue.jump(2))
        assertEquals("b", queue.previous())
        assertNull(queue.jump(-1))
        assertNull(queue.jump(3))
        assertEquals("b", queue.current)
        assertEquals(listOf("a", "b", "c"), queue.snapshot())
    }

    @Test fun jumpForgetsOldLinearVisitHistory() {
        val queue = OfflinePlaybackQueue()
        queue.select(listOf("a", "b", "c", "d"), "a")
        assertEquals("b", queue.next())
        assertEquals("d", queue.jump(3))
        assertEquals("c", queue.previous())
        assertEquals("b", queue.previous())
        assertEquals("a", queue.previous())
        assertNull(queue.previous())
    }

    @Test fun shuffledJumpStillReturnsToPreviousListeningPosition() {
        val queue = OfflinePlaybackQueue { 0 }
        queue.setShuffle(true)
        queue.select(listOf("a", "b", "c"), "a")
        assertEquals("c", queue.jump(2))
        assertNull(queue.jump(4))
        assertEquals("c", queue.current)
        assertEquals("a", queue.previous())
    }

    @Test fun sequentialPreviousIgnoresOldForwardHistory() {
        val queue = OfflinePlaybackQueue()
        queue.select(listOf("a", "b", "c", "d"), "a")
        assertEquals("b", queue.next())
        assertEquals("c", queue.next())
        assertEquals("b", queue.previous())
        assertEquals("a", queue.previous())
        assertNull(queue.previous())
        assertEquals("b", queue.next())
        assertEquals("c", queue.next())
        assertEquals("b", queue.previous())
    }

    @Test fun invalidShuffleJumpKeepsHistoryAndUnplayedOrder() {
        val queue = OfflinePlaybackQueue { 0 }
        queue.setShuffle(true)
        queue.select(listOf("a", "b", "c"), "a")
        assertEquals("b", queue.jump(1))
        assertNull(queue.jump(-1))
        assertNull(queue.jump(99))
        assertEquals("a", queue.previous())
        assertEquals("c", queue.next())
        assertEquals(listOf("a", "b", "c"), queue.snapshot())
    }

    @Test fun linearPreviousRepeatAllWrapsAtTheStart() {
        val queue = OfflinePlaybackQueue()
        queue.select(listOf("a", "b", "c"), "a")
        queue.setRepeat(com.nightwave.player.playback.PlaybackQueue.Repeat.ALL)
        assertEquals("c", queue.previous())
        assertEquals("b", queue.previous())
        assertEquals("a", queue.previous())
        assertEquals("c", queue.previous())
    }

    @Test fun turningShuffleOffRestoresVisibleBackNavigation() {
        val queue = OfflinePlaybackQueue { 0 }
        queue.select(listOf("a", "b", "c"), "a")
        queue.setShuffle(true)
        assertEquals("b", queue.next())
        queue.setShuffle(false)
        assertEquals("a", queue.previous())
        assertNull(queue.previous())
        assertEquals("b", queue.next())
        assertEquals("a", queue.previous())
    }

    @Test fun boundAndReset() {
        val queue = OfflinePlaybackQueue()
        queue.select((1..500).map(Int::toString), "400")
        assertEquals(OfflinePlaybackQueue.MAX_ITEMS, queue.size)
        assertNull(queue.next())
        queue.reset()
        assertEquals(0, queue.size)
        assertEquals(-1, queue.index)
        assertNull(queue.current)
    }
}
