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
        assertEquals(3, queue.size)
        assertEquals("a", queue.next())
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
