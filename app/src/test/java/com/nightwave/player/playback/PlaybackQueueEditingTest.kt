package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackQueueEditingTest {
    private fun song(i: Int) = QueueEntry("https://www.youtube.com/watch?v=" +
        i.toString().padStart(11, '0'), "Song $i", "Creator $i", "cover-$i")
    private fun queue(selected: Int = 1) = PlaybackQueue { 0 }.apply { replace((0..5).map(::song), selected) }

    @Test fun moveRetainsCurrentEarlierMetadataAndNextOrder() {
        val q = queue()
        assertTrue(q.moveUpcoming(5, 2, q.editSnapshot()))
        assertEquals(listOf(0,1,5,2,3,4).map(::song), q.snapshot())
        assertEquals(song(1), q.current); assertEquals(1, q.index)
        assertEquals(song(5), q.next()); assertEquals(song(2), q.next())
    }
    @Test fun movingDownUsesFinalSlotAndRejectsNoOpOrPast() {
        val q = queue(); val expected = q.editSnapshot()
        for ((from,to) in listOf(1 to 2, 2 to 0, -1 to 2, 2 to 99, 2 to 2))
            assertFalse(q.moveUpcoming(from, to, expected))
        assertTrue(q.moveUpcoming(2, 5, expected))
        assertEquals(listOf(0,1,3,4,5,2).map(::song),q.snapshot())
    }
    @Test fun changedQueueOrCurrentCannotAcceptAnOldMoveOrClear() {
        val q = queue(); val old = q.editSnapshot(); q.add(song(9))
        assertFalse(q.moveUpcoming(2,3,old)); assertFalse(q.clearUpcoming(old))
        val beforeJump = q.editSnapshot(); q.jump(2)
        assertFalse(q.moveUpcoming(3,4,beforeJump)); assertFalse(q.clearUpcoming(beforeJump))
    }
    @Test fun prioritiesCannotBeCrossedButCanBeReorderedWithinTheirSection() {
        val q = queue(); q.insertNext(song(8)); q.insertNext(song(9))
        assertFalse(q.moveUpcoming(2,4,q.editSnapshot()))
        assertTrue(q.moveUpcoming(3,2,q.editSnapshot()))
        assertEquals(listOf(song(8).url,song(9).url),q.priorityNextUrls())
        assertEquals(song(8),q.next()); assertEquals(song(9),q.next()); assertEquals(song(2),q.next())
    }
    @Test fun clearPreservesEarlierCurrentRepeatAndEndsWithoutPhantomSongs() {
        val q = queue(); q.repeat = PlaybackQueue.Repeat.ONE; q.insertNext(song(9))
        assertTrue(q.clearUpcoming(q.editSnapshot()))
        assertEquals(listOf(song(0),song(1)),q.snapshot()); assertEquals(song(1),q.current)
        assertEquals(PlaybackQueue.Repeat.ONE,q.repeat); assertTrue(q.priorityNextUrls().isEmpty())
        assertEquals(song(1),q.next(automatic=true)); assertNull(q.next())
        assertFalse(q.clearUpcoming(q.editSnapshot()))
    }
    @Test fun shuffleAndEmptyQueuesRejectEditsWithoutChangingTheirHistory() {
        val q = queue(); q.shuffle = true; q.next(); val old = q.shuffleRecoveryState()
        assertFalse(q.moveUpcoming(3,4,q.editSnapshot())); assertFalse(q.clearUpcoming(q.editSnapshot()))
        assertEquals(old,q.shuffleRecoveryState())
        val empty = PlaybackQueue()
        assertFalse(empty.clearUpcoming(empty.editSnapshot())); assertFalse(empty.moveUpcoming(0,1,empty.editSnapshot()))
    }
    @Test fun undoRestoresPlayNextPriorityBeforeOtherManualChoices() {
        val q = queue(); q.insertNext(song(8)); q.insertNext(song(9))
        val removed = q.removeUpcoming(2,song(9).url)!!
        assertTrue(q.restoreUpcoming(removed))
        assertEquals(listOf(song(9).url,song(8).url),q.priorityNextUrls())
        assertEquals(song(9),q.next()); assertEquals(song(8),q.next())
    }
    @Test fun undoRejectsChangedRemainderEvenWhenCurrentAndInsertionStillMatch() {
        val q = queue(); val removed = q.removeUpcoming(3,song(3).url)!!
        q.add(song(9)); val now=q.snapshot()
        assertFalse(q.restoreUpcoming(removed)); assertEquals(now,q.snapshot())
    }
    @Test fun editedOrderSurvivesIdentityBasedReconstruction() {
        val q=queue(); q.insertNext(song(8)); q.insertNext(song(9))
        q.moveUpcoming(3,2,q.editSnapshot())
        val copy=PlaybackQueue().apply {
            replace(q.snapshot(),q.index); repeat=q.repeat; shuffle=q.shuffle
            restoreNextPriorities(q.priorityNextUrls()); restoreShuffleRecovery(q.shuffleRecoveryState())
        }
        assertEquals(q.editSnapshot(),copy.editSnapshot())
        repeat(5) { assertEquals(q.next(),copy.next()) }
    }
    @Test fun singleSearchSelectionNeverGainsAnUnrelatedSongFromEditing() {
        val q=PlaybackQueue(); q.replace(listOf(song(7)),0)
        assertFalse(q.clearUpcoming(q.editSnapshot())); assertFalse(q.moveUpcoming(0,1,q.editSnapshot()))
        assertNull(q.next()); assertEquals(listOf(song(7)),q.snapshot())
    }
}
