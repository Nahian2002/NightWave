package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

/** Pure Kotlin, no Android device or media service required. */
class PlaybackQueueUpcomingTest {
    private fun song(i: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=track$i", "Track $i", "Artist $i", ""
    )

    @Test fun removeAndUndoUpcomingPreservesCurrentAndOrder() {
        val queue = PlaybackQueue()
        queue.replace((0..4).map(::song), 1)
        val before = queue.snapshot()
        val removed = queue.removeUpcoming(3, song(3).url)
        assertNotNull(removed)
        assertEquals(song(1), queue.current)
        assertEquals(listOf(song(0), song(1), song(2), song(4)), queue.snapshot())
        assertTrue(queue.restoreUpcoming(removed!!))
        assertEquals(before, queue.snapshot())
        assertEquals(1, queue.index)
    }

    @Test fun cannotRemoveCurrentOrPreviouslyPlayedItem() {
        val queue = PlaybackQueue()
        queue.replace((0..2).map(::song), 1)
        assertNull(queue.removeUpcoming(1, song(1).url))
        assertNull(queue.removeUpcoming(0, song(0).url))
        assertEquals(3, queue.size)
        assertEquals(song(1), queue.current)
    }

    @Test fun staleIndexOrUrlCannotRemoveTheWrongTrack() {
        val queue = PlaybackQueue()
        queue.replace((0..3).map(::song), 0)
        assertNull(queue.removeUpcoming(2, song(3).url))
        assertNull(queue.removeUpcoming(9, song(2).url))
        assertNull(queue.removeUpcoming(-1, song(0).url))
        assertEquals((0..3).map(::song), queue.snapshot())
    }

    @Test fun undoIsOneWayAndFailsAfterCurrentTrackChanges() {
        val queue = PlaybackQueue()
        queue.replace((0..3).map(::song), 0)
        val removed = queue.removeUpcoming(2, song(2).url)!!
        queue.jump(1)
        assertFalse(queue.restoreUpcoming(removed))
        assertEquals(3, queue.size)
        queue.jump(0)
        assertTrue(queue.restoreUpcoming(removed))
        assertFalse(queue.restoreUpcoming(removed))
    }

    @Test fun shuffledQueueDeniesRemovalAndUndo() {
        val queue = PlaybackQueue()
        queue.replace((0..3).map(::song), 0)
        val removed = queue.removeUpcoming(2, song(2).url)!!
        queue.shuffle = true
        assertFalse(queue.restoreUpcoming(removed))
        assertNull(queue.removeUpcoming(3, song(3).url))
        assertEquals(3, queue.size)
    }

    @Test fun undoFailsForReplacementOrDuplicateURL() {
        val queue = PlaybackQueue()
        queue.replace((0..3).map(::song), 0)
        val removed = queue.removeUpcoming(2, song(2).url)!!
        queue.replace(listOf(song(0), song(2), song(3)), 0)
        assertFalse(queue.restoreUpcoming(removed))
        assertEquals(3, queue.size)
    }

    @Test fun removalDoesNotCorruptVisitedIndexHistory() {
        val queue = PlaybackQueue()
        queue.replace((0..4).map(::song), 0)
        queue.jump(4)
        queue.jump(0)
        val removed = queue.removeUpcoming(2, song(2).url)!!
        assertEquals(song(4), queue.previous()) // visited index was adjusted
        queue.jump(0)
        assertTrue(queue.restoreUpcoming(removed))
        assertEquals(5, queue.size)
        assertEquals(song(0), queue.current)
    }

    @Test fun maxHundredQueueItemsCanBeRestoredWithoutAddingFiles() {
        val queue = PlaybackQueue()
        queue.replace((0 until 100).map(::song), 0)
        val removed = queue.removeUpcoming(99, song(99).url)!!
        assertEquals(99, queue.size)
        assertTrue(queue.restoreUpcoming(removed))
        assertEquals(100, queue.size)
        assertEquals(song(0), queue.current)
    }
}
