package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression for linear Previous unexpectedly returning to an old jumped song. */
class PlaybackQueueNavigationModeTest {
    private fun song(index: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=track$index", "Track $index", "Creator", ""
    )
    private val songs = (0..4).map(::song)

    @Test fun unshuffledPreviousFollowsVisibleRowAfterJump() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(songs, 0)
        queue.jump(4)
        assertEquals(song(3), queue.previous())
        assertEquals(song(2), queue.previous())
    }

    @Test fun invalidJumpCannotContaminatePrevious() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(songs, 0)
        assertNull(queue.jump(-1))
        assertNull(queue.jump(99))
        assertEquals(song(0), queue.current)
        assertEquals(song(4), queue.jump(4))
        assertEquals(song(3), queue.previous())
    }

    @Test fun repeatedLinearNextPreviousUsesDisplayedSequence() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(songs, 1)
        queue.jump(3)
        queue.jump(1)
        assertEquals(song(2), queue.next())
        assertEquals(song(1), queue.previous())
        assertEquals(song(0), queue.previous())
    }

    @Test fun shuffleBacktracksActualHistory() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(songs, 0)
        queue.shuffle = true
        assertEquals(song(1), queue.next())
        assertEquals(song(4), queue.jump(4))
        assertEquals(song(1), queue.previous())
        assertEquals(song(0), queue.previous())
    }

    @Test fun shuffleModeChangesDiscardOldHistory() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(songs, 0)
        queue.shuffle = true
        assertEquals(song(1), queue.next())
        queue.jump(4)
        queue.shuffle = false
        assertFalse(queue.shuffle)
        assertEquals(song(3), queue.previous())
        queue.shuffle = true
        assertTrue(queue.shuffle)
        assertEquals(song(0), queue.next()) // fresh shuffled candidates, not stale seen history
    }

    @Test fun repeatAllWrapsPreviousWhenShuffleOff() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(songs, 0)
        queue.repeat = PlaybackQueue.Repeat.ALL
        assertEquals(song(4), queue.previous())
        assertEquals(song(3), queue.previous())
    }

    @Test fun selectedDuplicateKeepsCorrectLinearPrevious() {
        val queue = PlaybackQueue(pick = { 0 })
        val input = listOf(song(0), song(0), song(2), song(3))
        assertEquals(song(2), queue.replace(input, 2))
        assertEquals(song(0), queue.previous())
    }

    @Test fun emptyQueuePreviousStaysEmpty() {
        val queue = PlaybackQueue(pick = { 0 })
        assertNull(queue.previous())
        queue.shuffle = true
        assertNull(queue.previous())
        queue.reset()
        assertNull(queue.previous())
    }
}
