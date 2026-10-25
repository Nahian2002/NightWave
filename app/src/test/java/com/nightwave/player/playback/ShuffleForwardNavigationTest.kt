package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW67: navigation must honor what was heard, not random candidate ordering. */
class ShuffleForwardNavigationTest {
    private fun song(n: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'),
        "Song $n", "Artist", ""
    )
    private fun queue() = PlaybackQueue(pick = { candidates -> candidates - 1 }).apply {
        replace((1..5).map(::song), 0)
        shuffle = true
    }

    @Test fun nextAfterPreviousReturnsExactlyTheDepartedSong() {
        val q = queue()
        assertEquals(song(5), q.next())
        assertEquals(song(1), q.previous())
        assertEquals(song(5), q.next())
    }

    @Test fun multiplePreviousStepsCanBeRetracedForwardInOrder() {
        val q = queue()
        val a = q.current
        val b = q.next()
        val c = q.next()
        val d = q.next()
        assertEquals(c, q.previous())
        assertEquals(b, q.previous())
        assertEquals(a, q.previous())
        assertEquals(b, q.next())
        assertEquals(c, q.next())
        assertEquals(d, q.next())
    }

    @Test fun invalidJumpDoesNotEraseForwardButValidJumpDoes() {
        val q = queue()
        val second = q.next()
        assertEquals(song(1), q.previous())
        assertNull(q.jump(-2))
        assertEquals(second, q.next())
        q.previous()
        assertEquals(song(3), q.jump(2))
        assertEquals(song(3), q.current)
        // The new shuffle pick is allowed to revisit any unfailed candidate,
        // but must never replay the currently selected song as its own Next.
        assertFalse(q.next() == song(3))
    }

    @Test fun explicitPlayNextOutranksForwardAndClearsStaleTrail() {
        val q = queue()
        val jumped = q.next()
        q.previous()
        assertTrue(q.insertNext(song(6)))
        assertEquals(song(6), q.next())
        assertFalse(q.next() == song(6)) // queued song is not repeated immediately
    }

    @Test fun shuffleToggleReplacementAndQueueEditsClearForward() {
        val q = queue()
        q.next()
        q.previous()
        q.shuffle = false
        q.shuffle = true
        assertEquals(song(5), q.next()) // new cycle, deterministic last candidate
        q.previous()
        q.replace(listOf(song(7), song(8)), 0)
        assertEquals(song(8), q.next())
        q.reset()
        assertNull(q.next())
    }

    @Test fun repeatOneDoesNotConsumeShuffledForwardDuringAutomaticReplay() {
        val q = queue()
        val second = q.next()
        q.previous()
        q.repeat = PlaybackQueue.Repeat.ONE
        assertEquals(song(1), q.next(automatic = true))
        assertEquals(second, q.next(automatic = false))
    }
}
