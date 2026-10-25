package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

/** NW43: Play Next must never mean a random Search-result playlist. */
class PlaybackQueuePlayNextTest {
    private fun song(i: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
        "Title $i", "Artist $i", "cover-$i"
    )

    @Test fun playNextInsertsDirectlyAfterCurrentWithoutInterruptingIt() {
        val q = PlaybackQueue()
        q.replace(listOf(song(1), song(2), song(3)), selected = 0)
        assertTrue(q.insertNext(song(9)))
        assertEquals(song(1), q.current)
        assertEquals(listOf(song(1), song(9), song(2), song(3)), q.snapshot())
        assertEquals(song(9), q.next())
        assertEquals(song(2), q.next())
    }

    @Test fun playNextAndAddToQueueHaveDifferentOrder() {
        val q = PlaybackQueue()
        q.replace(listOf(song(1), song(2)), 0)
        assertTrue(q.add(song(8)))
        assertTrue(q.insertNext(song(9)))
        assertEquals(listOf(song(1), song(9), song(2), song(8)), q.snapshot())
        assertEquals(song(9), q.next())
    }

    @Test fun latestPlayNextTakesPriorityWhilePreservingEarlierChoices() {
        val q = PlaybackQueue()
        q.replace(listOf(song(1), song(2)), 0)
        assertTrue(q.insertNext(song(8)))
        assertTrue(q.insertNext(song(9)))
        assertEquals(listOf(song(1), song(9), song(8), song(2)), q.snapshot())
        assertEquals(song(9), q.next())
        assertEquals(song(8), q.next())
    }

    @Test fun movingExistingUpcomingDoesNotDuplicateOrLoseSavedMetadata() {
        val q = PlaybackQueue()
        q.replace(listOf(song(1), song(2), song(3), song(4)), 1)
        assertTrue(q.insertNext(song(4).copy(title = "Misleading stale title")))
        assertEquals(listOf(song(1), song(2), song(4), song(3)), q.snapshot())
        assertEquals("Title 4", q.snapshot()[2].title)
        assertEquals(song(2), q.current)
        assertEquals(song(4), q.next())
    }

    @Test fun currentAndPreviouslyPlayedItemsAreNotSilentlyRewritten() {
        val q = PlaybackQueue()
        q.replace(listOf(song(1), song(2), song(3)), 1)
        val before = q.snapshot()
        assertFalse(q.insertNext(song(2)))
        assertFalse(q.insertNext(song(1)))
        assertEquals(before, q.snapshot())
        assertEquals(1, q.index)
    }

    @Test fun fullQueueMayReorderExistingButCannotAppendBeyondLimit() {
        val q = PlaybackQueue()
        q.replace((1..100).map(::song), 0)
        assertFalse(q.insertNext(song(101)))
        assertEquals(100, q.size)
        assertTrue(q.insertNext(song(100)))
        assertEquals(100, q.size)
        assertEquals(song(100), q.snapshot()[1])
        assertEquals(song(1), q.current)
    }

    @Test fun manualNextOutranksShuffleButKeepsShuffleEnabled() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..4).map(::song), 0)
        q.shuffle = true
        assertTrue(q.insertNext(song(4)))
        assertTrue(q.shuffle)
        assertEquals(song(4), q.next(automatic = true))
        assertEquals(song(2), q.next(automatic = true))
        assertEquals(song(4), q.previous())
        assertEquals(song(1), q.previous())
    }

    @Test fun newestExplicitNextWinsEvenWhenShuffleOn() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..4).map(::song), 0)
        q.shuffle = true
        q.insertNext(song(3))
        q.insertNext(song(4))
        assertEquals(song(4), q.next())
        assertEquals(song(3), q.next())
        assertTrue(q.shuffle)
    }

    @Test fun repeatOneStillRepeatsUntilExplicitUserSkip() {
        val q = PlaybackQueue()
        q.replace(listOf(song(1), song(2)), 0)
        q.repeat = PlaybackQueue.Repeat.ONE
        q.insertNext(song(9))
        assertEquals(song(1), q.next(automatic = true))
        assertEquals(song(9), q.next(automatic = false))
    }

    @Test fun replacingOrClearingQueueInvalidatesOldManualPriority() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace(listOf(song(1), song(2)), 0)
        q.shuffle = true
        q.insertNext(song(9))
        q.replace(listOf(song(6), song(7)), 0)
        assertEquals(song(7), q.next())
        q.reset()
        assertNull(q.next())
    }

    @Test fun manualPrioritySurvivesReconstructionEvenUnderShuffle() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..4).map(::song), 0)
        q.shuffle = true
        q.insertNext(song(4))
        q.insertNext(song(3))
        val priorities = q.priorityNextUrls()
        assertEquals(listOf(song(3).url, song(4).url), priorities)
        val copy = PlaybackQueue(pick = { 0 })
        copy.replace(q.snapshot(), q.index)
        copy.shuffle = true
        copy.restoreNextPriorities(priorities)
        assertEquals(song(3), copy.next())
        assertEquals(song(4), copy.next())
    }

    @Test fun staleOrPastPriorityFromCorruptSnapshotIsIgnored() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..4).map(::song), 1)
        q.shuffle = true
        q.restoreNextPriorities(listOf(song(1).url, song(2).url,
            "https://youtube.com.evil.test/unsafe", song(4).url, song(4).url))
        assertEquals(listOf(song(4).url), q.priorityNextUrls())
        assertEquals(song(4), q.next())
        assertTrue(q.priorityNextUrls().isEmpty())
    }

    @Test fun emptyQueuePlayNextCreatesOneSelectedSong() {
        val q = PlaybackQueue()
        assertTrue(q.insertNext(song(9)))
        assertEquals(listOf(song(9)), q.snapshot())
        assertEquals(0, q.index)
        assertEquals(song(9), q.current)
        assertNull(q.next())
    }

    @Test fun manualNextRemovalNeverLeavesPhantomTrack() {
        val q = PlaybackQueue()
        q.replace(listOf(song(1), song(2)), 0)
        q.insertNext(song(9))
        val at = q.snapshot().indexOf(song(9))
        assertNotNull(q.removeUpcoming(at, song(9).url))
        assertEquals(song(2), q.next())
    }
}
