package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW69: Play Next priority must be upcoming, never a past-row auto jump. */
class PublicQueuePastPriorityTest {
    private fun song(n: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'),
        "Song $n", "Artist", ""
    )

    @Test fun jumpingPastSeveralPlayNextEntriesDoesNotAutoRewind() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..5).map(::song), 0)
        assertTrue(q.insertNext(song(4)))
        assertTrue(q.insertNext(song(3)))
        assertEquals(listOf(song(3).url, song(4).url), q.priorityNextUrls())
        assertEquals(song(4), q.jump(2)) // skip song 3 priority, select song 4
        assertTrue(q.priorityNextUrls().isEmpty())
        assertEquals(song(2), q.next()) // sequential upcoming item, not song 3
    }

    @Test fun validFuturePrioritySurvivesJumpToEarlierSong() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..5).map(::song), 0)
        assertTrue(q.insertNext(song(5)))
        assertEquals(song(1), q.jump(0))
        assertEquals(listOf(song(5).url), q.priorityNextUrls())
        assertEquals(song(5), q.next())
    }

    @Test fun skippingPriorityDoesNotRemoveSongFromDisplayedQueue() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..4).map(::song), 0)
        q.insertNext(song(4))
        val before = q.snapshot()
        q.jump(2)
        assertTrue(q.priorityNextUrls().isEmpty())
        assertEquals(before, q.snapshot()) // no deletion or renaming
    }

    @Test fun shuffledPastPriorityIsNoLongerForced() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..5).map(::song), 0)
        q.shuffle = true
        q.insertNext(song(5))
        q.jump(2) // jump beyond the inserted priority
        assertTrue(q.priorityNextUrls().isEmpty())
        assertFalse(q.current == song(5))
        // Shuffled random candidates remain legitimate, but forced priority is gone.
        assertTrue(q.next() != null)
    }
}
