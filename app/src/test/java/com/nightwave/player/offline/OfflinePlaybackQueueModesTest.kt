package com.nightwave.player.offline

import com.nightwave.player.playback.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePlaybackQueueModesTest {
    private fun queue(): OfflinePlaybackQueue =
        OfflinePlaybackQueue(pick = { 0 }).apply {
            select(listOf("a.mp3", "b.mp3", "c.mp3"), "a.mp3")
        }

    @Test fun repeatModesCycleAndDoNotMutateQueueIds() {
        val queue = queue()
        val original = queue.snapshot()
        queue.setRepeat(PlaybackQueue.Repeat.ONE)
        assertEquals("a.mp3", queue.next(automatic = true))
        assertEquals("b.mp3", queue.next(automatic = false))
        queue.setRepeat(PlaybackQueue.Repeat.ALL)
        assertEquals("c.mp3", queue.next())
        assertEquals("a.mp3", queue.next())
        assertEquals(original, queue.snapshot())
    }

    @Test fun repeatOffEndsAtTheLastSong() {
        val queue = queue()
        assertEquals("b.mp3", queue.next())
        assertEquals("c.mp3", queue.next())
        assertNull(queue.next())
        assertEquals("c.mp3", queue.current)
    }

    @Test fun shuffledCycleDoesNotRepeatBeforeExhaustion() {
        val queue = queue()
        queue.setShuffle(true)
        val visited = mutableListOf(queue.current)
        visited.add(queue.next())
        visited.add(queue.next())
        assertEquals(3, visited.filterNotNull().distinct().size)
        assertNull(queue.next())
        assertEquals(PlaybackQueue.Repeat.OFF, queue.repeat)
    }

    @Test fun shuffledRepeatAllStartsANewCycle() {
        val queue = queue()
        queue.setShuffle(true)
        queue.setRepeat(PlaybackQueue.Repeat.ALL)
        queue.next()
        queue.next()
        assertTrue(queue.next() != null)
        assertEquals(3, queue.snapshot().size)
    }

    @Test fun previousFollowsActualShuffledHistory() {
        val queue = queue()
        queue.setShuffle(true)
        val first = queue.current
        val second = queue.next()
        val third = queue.next()
        assertEquals(second, queue.previous())
        assertEquals(first, queue.previous())
        assertEquals(third, queue.snapshot()[2])
    }

    @Test fun changingShuffleDoesNotDestroySelectedTrackOrSettings() {
        val queue = queue()
        queue.jump(1)
        queue.setRepeat(PlaybackQueue.Repeat.ONE)
        queue.setShuffle(true)
        assertEquals("b.mp3", queue.current)
        queue.setShuffle(false)
        assertEquals("b.mp3", queue.current)
        assertEquals(PlaybackQueue.Repeat.ONE, queue.repeat)
    }

    @Test fun selectingNewQueuePreservesPreferencesButResetsHistory() {
        val queue = queue()
        queue.setShuffle(true)
        queue.setRepeat(PlaybackQueue.Repeat.ALL)
        queue.next()
        queue.select(listOf("x.mp3", "y.mp3"), "y.mp3")
        assertEquals("y.mp3", queue.current)
        assertTrue(queue.shuffle)
        assertEquals(PlaybackQueue.Repeat.ALL, queue.repeat)
        queue.reset()
        assertEquals(-1, queue.index)
        assertEquals(0, queue.size)
        assertTrue(queue.shuffle)
    }

    @Test fun boundedQueueRemainsFourHundredItems() {
        val queue = OfflinePlaybackQueue(pick = { 0 })
        val ids = (0..500).map { "$it.mp3" }
        queue.select(ids, "0.mp3")
        assertEquals(400, queue.size)
        assertFalse(queue.snapshot().contains("400.mp3"))
    }
}
