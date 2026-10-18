package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueDisplayOrderTest {
    private val tracks = listOf(
        QueueEntry("https://www.youtube.com/watch?v=abc12345670", "A", "C", ""),
        QueueEntry("https://www.youtube.com/watch?v=abc12345671", "B", "C", ""),
        QueueEntry("https://www.youtube.com/watch?v=abc12345672", "C", "C", "")
    )

    @Test fun explicitOrderDisablesPreviouslySelectedShuffle() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(tracks, 0)
        queue.shuffle = true
        queue.useExplicitDisplayOrder()
        assertFalse(queue.shuffle)
        assertEquals(tracks[1], queue.next())
    }

    @Test fun clearingShuffleCycleAllowsFutureShuffleToStartFresh() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(tracks, 0)
        queue.shuffle = true
        assertEquals(tracks[1], queue.next()) // marks index 0 as seen
        queue.useExplicitDisplayOrder()
        assertFalse(queue.shuffle)
        queue.shuffle = true
        assertEquals(tracks[0], queue.next()) // old seen index 0 was cleared
    }

    @Test fun normalReplacementDoesNotChangeUserChosenShuffle() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.shuffle = true
        queue.replace(tracks, 0)
        assertTrue(queue.shuffle)
    }

    @Test fun explicitOrderDoesNotAlterRepeatMode() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.repeat = PlaybackQueue.Repeat.ALL
        queue.replace(tracks, 0)
        queue.shuffle = true
        queue.useExplicitDisplayOrder()
        assertEquals(PlaybackQueue.Repeat.ALL, queue.repeat)
        assertEquals(tracks[1], queue.next())
    }

    @Test fun emptyQueueRemainsEmptyAfterOrderReset() {
        val queue = PlaybackQueue(pick = { 0 })
        queue.shuffle = true
        queue.useExplicitDisplayOrder()
        assertFalse(queue.shuffle)
        assertEquals(null, queue.next())
    }
}
