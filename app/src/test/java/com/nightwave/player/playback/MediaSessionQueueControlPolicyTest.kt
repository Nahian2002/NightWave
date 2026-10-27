package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class MediaSessionQueueControlPolicyTest {
    private val p = MediaSessionQueueControlPolicy

    @Test fun appAndNotificationControllersMayReceiveQueueButtons() {
        assertTrue(p.trustedController(true, false))
        assertTrue(p.trustedController(false, true))
        assertTrue(p.trustedController(true, true))
    }

    @Test fun externalControllerCannotObtainPrivilegedCustomQueueButtons() {
        assertFalse(p.trustedController(false, false))
    }

    @Test fun commandNamesMustBeExactAndDistinct() {
        assertTrue(p.knownAction("next", "next", "previous"))
        assertTrue(p.knownAction("previous", "next", "previous"))
        assertFalse(p.knownAction("seek", "next", "previous"))
        assertFalse(p.knownAction(null, "next", "previous"))
    }

    @Test fun corruptedCommandIdentifiersCannotAuthoriseNavigation() {
        assertFalse(p.knownAction("next", "", "previous"))
        assertFalse(p.knownAction("next", "next", "next"))
        assertFalse(p.knownAction("previous", "next", ""))
    }

    @Test fun publicSelectionCanBeNavigatedWithoutLoadedMediaItem() {
        assertTrue(p.canNavigate(false, false, true))
    }

    @Test fun offlineSelectionSupportsGaplessAndLegacyFallback() {
        assertTrue(p.canNavigate(true, true, false))
    }

    @Test fun noSelectedTrackCannotAcknowledgeSuccessfulNavigation() {
        assertFalse(p.canNavigate(false, false, false))
        assertFalse(p.canNavigate(true, false, false))
    }

    @Test fun switchingModeNeverNavigatesAnOldSelectionFromOtherMode() {
        assertFalse(p.canNavigate(true, false, true))
        assertFalse(p.canNavigate(false, true, false))
    }

    @Test fun exhaustedPublicQueueCannotClaimHeadsetNextSucceeded() {
        assertFalse(p.canAcknowledgeNext(
            offlinePlaying = false, offlineSelected = false,
            publicSelected = true, hasNext = false))
    }

    @Test fun queuedPlayNextOrUnseenShuffleTargetAllowsPublicNext() {
        assertTrue(p.canAcknowledgeNext(
            offlinePlaying = false, offlineSelected = false,
            publicSelected = true, hasNext = true))
    }

    @Test fun offlineGaplessEndCannotClaimNextWithoutActualNextItem() {
        assertFalse(p.canAcknowledgeNext(
            offlinePlaying = true, offlineSelected = true,
            publicSelected = false, hasNext = false))
        assertTrue(p.canAcknowledgeNext(
            offlinePlaying = true, offlineSelected = true,
            publicSelected = false, hasNext = true))
    }

    @Test fun wrongModeOrMissingSelectionCannotAcceptNext() {
        assertFalse(p.canAcknowledgeNext(
            offlinePlaying = true, offlineSelected = false,
            publicSelected = true, hasNext = true))
        assertFalse(p.canAcknowledgeNext(
            offlinePlaying = false, offlineSelected = true,
            publicSelected = false, hasNext = true))
        assertFalse(p.canAcknowledgeNext(
            offlinePlaying = false, offlineSelected = false,
            publicSelected = false, hasNext = true))
    }

    @Test fun readOnlyAvailabilityProbeLeavesActualQueueAndShufflePriorityIntact() {
        fun track(i: Int) = QueueEntry(
            "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
            "Song $i", "Artist", "")
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(listOf(track(1), track(2), track(3)), 0)
        queue.shuffle = true
        assertTrue(queue.insertNext(track(3)))
        val selection = queue.current
        val before = queue.snapshot()
        val priority = queue.priorityNextUrls()
        val history = queue.shuffleRecoveryState()
        repeat(6) {
            assertTrue(p.canAcknowledgeNext(
                offlinePlaying = false, offlineSelected = false,
                publicSelected = queue.current != null, hasNext = queue.hasNext()))
        }
        assertEquals(selection, queue.current)
        assertEquals(before, queue.snapshot())
        assertEquals(priority, queue.priorityNextUrls())
        assertEquals(history, queue.shuffleRecoveryState())
        assertEquals(track(3), queue.next())
    }
}
