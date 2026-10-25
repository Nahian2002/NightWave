package com.nightwave.player.offline

import com.nightwave.player.playback.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NW69 covers the legacy offline queue used when a verified Media3 timeline
 * cannot be prepared. Media3's prepared shuffle order remains player-owned.
 */
class OfflineShuffleForwardNavigationTest {
    private fun queue(): OfflinePlaybackQueue =
        OfflinePlaybackQueue(pick = { 0 }).apply {
            select(listOf("a.mp3", "b.mp3", "c.mp3", "d.mp3"), "a.mp3")
            setShuffle(true)
        }

    @Test fun multiplePreviousThenNextRetracesExactlyTheSongsHeard() {
        val q = queue()
        assertEquals("b.mp3", q.next())
        assertEquals("c.mp3", q.next())
        assertEquals("d.mp3", q.next())
        assertEquals("c.mp3", q.previous())
        assertEquals("b.mp3", q.previous())
        assertEquals("a.mp3", q.previous())
        assertEquals("b.mp3", q.next())
        assertEquals("c.mp3", q.next())
        assertEquals("d.mp3", q.next())
        assertNull(q.next()) // all four songs were heard; no repeat with Repeat Off
    }

    @Test fun repeatOneDoesNotConsumeForwardHistoryDuringAutoReplay() {
        val q = queue()
        assertEquals("b.mp3", q.next())
        assertEquals("a.mp3", q.previous())
        q.setRepeat(PlaybackQueue.Repeat.ONE)
        assertEquals("a.mp3", q.next(automatic = true))
        assertEquals("b.mp3", q.next(automatic = false))
    }

    @Test fun explicitJumpCreatesANewShuffleBranch() {
        val q = queue()
        q.next() // b
        q.previous() // a, forward b
        assertEquals("c.mp3", q.jump(2))
        // The next random *unplayed* track is d, not stale forward b.
        assertEquals("d.mp3", q.next())
        assertEquals("c.mp3", q.previous())
        assertEquals("d.mp3", q.next())
    }

    @Test fun togglingShuffleOrSelectingNewPlaylistDropsOldForwardTrail() {
        val q = queue()
        q.next()
        q.previous()
        q.setShuffle(false)
        q.setShuffle(true)
        assertEquals("b.mp3", q.next())
        q.previous()
        assertEquals("x.mp3", q.select(listOf("x.mp3", "y.mp3"), "x.mp3"))
        assertEquals("y.mp3", q.next())
        q.reset()
        assertNull(q.next())
    }

    @Test fun explicitSameSlotJumpDoesNotEraseValidForwardHistory() {
        val q = queue()
        q.next()
        q.previous()
        assertEquals("a.mp3", q.jump(0))
        assertEquals("b.mp3", q.next())
    }

    @Test fun noQueueMutationOrNewMediaIdsFromNavigation() {
        val q = queue()
        val snapshot = q.snapshot()
        repeat(5) {
            q.next()
            q.previous()
        }
        assertEquals(snapshot, q.snapshot())
        assertTrue(q.size <= OfflinePlaybackQueue.MAX_ITEMS)
    }
}
