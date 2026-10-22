package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackQueueTest {
    private fun e(n: Int) = QueueEntry("https://youtu.be/video$n", "Track $n", "Artist", "")

    @Test fun fullNativeSearchResultsPlayNextInCorrectOrder() {
        val queue = PlaybackQueue()
        queue.replace((1..40).map(::e), 12)
        assertEquals(e(13), queue.current)
        assertEquals(e(14), queue.next(true))
        assertEquals(e(13), queue.previous())
    }

    @Test fun userNextIgnoresRepeatOneButCompletionReplaysCurrent() {
        val queue = PlaybackQueue()
        queue.replace(listOf(e(1), e(2), e(3)), 0)
        queue.repeat = PlaybackQueue.Repeat.ONE
        assertEquals(e(1), queue.next(true))
        assertEquals(e(2), queue.next(false))
        assertEquals(e(2), queue.next(true))
    }

    @Test fun shuffleRestartsWithoutImmediateRepeatAcrossTwoTracks() {
        val queue = PlaybackQueue { 0 }
        queue.replace(listOf(e(1), e(2)), 0)
        queue.shuffle = true
        queue.repeat = PlaybackQueue.Repeat.ALL
        repeat(15) {
            val before = queue.current
            assertNotEquals(before, queue.next(true))
        }
    }

    @Test fun endStopsWhenRepeatOff() {
        val q = PlaybackQueue()
        q.replace(listOf(e(1), e(2)), 0)
        assertEquals(e(2), q.next(true))
        assertNull(q.next(true))
        assertEquals(e(2), q.current)
    }

    @Test fun repeatAllWrapsAndOneReplaysOnlyOnCompletion() {
        val q = PlaybackQueue()
        q.replace(listOf(e(1), e(2)), 1)
        q.repeat = PlaybackQueue.Repeat.ALL
        assertEquals(e(1), q.next(true))
        q.repeat = PlaybackQueue.Repeat.ONE
        assertEquals(e(1), q.next(true))
        assertEquals(e(2), q.next(false))
    }

    @Test fun shuffleExhaustsTracksWithoutRepeat() {
        val q = PlaybackQueue { 0 }
        q.replace(listOf(e(1), e(2), e(3)), 1)
        q.shuffle = true
        val played = mutableSetOf(q.current)
        repeat(2) {
            val next = q.next(true)
            assertNotNull(next)
            assertTrue(played.add(next))
        }
        assertNull(q.next(true))
    }

    @Test fun shuffleRepeatAllReshufflesWithoutImmediateDuplicate() {
        val q = PlaybackQueue { 0 }
        q.replace(listOf(e(1), e(2), e(3)), 1)
        q.shuffle = true
        q.repeat = PlaybackQueue.Repeat.ALL
        repeat(12) {
            val previous = q.current
            assertNotEquals(previous, q.next(true))
        }
    }

    @Test fun backTracksPreviousSelection() {
        val q = PlaybackQueue()
        q.replace(listOf(e(1), e(2), e(3)), 0)
        q.next()
        q.next()
        assertEquals(e(2), q.previous())
        assertEquals(e(1), q.previous())
    }

    @Test fun jumpHandlesBoundsAndPreservesPrevious() {
        val q = PlaybackQueue()
        q.replace(listOf(e(1), e(2), e(3)), 0)
        assertNull(q.jump(9))
        assertEquals(e(3), q.jump(2))
        assertEquals(e(2), q.previous())
        assertEquals(e(1), q.previous())
    }

    @Test fun deduplicatedReplacementKeepsRequestedSongSelected() {
        val q = PlaybackQueue()
        assertEquals(e(2), q.replace(listOf(e(1), e(1), e(2), e(3)), 2))
        assertEquals(1, q.index)
        assertEquals(listOf(e(1), e(2), e(3)), q.snapshot())
        assertEquals(e(3), q.next())
    }

    @Test fun duplicateSelectedUrlResolvesToTheRetainedSong() {
        val q = PlaybackQueue()
        assertEquals(e(1), q.replace(listOf(e(1), e(2), e(1)), 2))
        assertEquals(0, q.index)
        assertEquals(2, q.size)
    }

    @Test fun queueCapsAt100AndDoesNotDuplicate() {
        val q = PlaybackQueue()
        q.replace((0..150).map(::e), 10)
        assertEquals(100, q.size)
        assertFalse(q.add(e(10)))
        assertFalse(q.add(e(151)))
        assertEquals(10, q.index)
    }
}
