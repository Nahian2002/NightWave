package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW115: interrupted public playback must not replay an expired IDLE stream. */
class PlaybackIdleSessionRecoveryPolicyTest {
    private val songA = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val songB = "https://www.youtube.com/watch?v=9bZkp7q19f0"
    private val policy = PlaybackResolutionPolicy

    @Test fun healthyLoadedMediaCanResumeWithoutAnotherLookup() {
        assertTrue(policy.canResumeLoadedPublic(songA, songA, false, false))
        assertTrue(policy.canResumeLoadedPublic(songB, songB, false, false))
    }

    @Test fun idleSameSongMustNotReplayItsOldSignedStream() {
        assertFalse(policy.canResumeLoadedPublic(songA, songA, false, true))
        assertTrue(policy.shouldResolveSavedSelection(
            true, true, songA, reusableLoaded = false, pendingMatching = false))
    }

    @Test fun missingLoadedMediaAfterServiceRestartRequiresFreshLookup() {
        assertFalse(policy.canResumeLoadedPublic(songA, null, false, true))
        assertTrue(policy.shouldResolveSavedSelection(
            true, true, songA, reusableLoaded = false, pendingMatching = false))
    }

    @Test fun wrongLoadedSongAndPlayerErrorsCannotResumeOldStream() {
        assertFalse(policy.canResumeLoadedPublic(songA, songB, false, false))
        assertFalse(policy.canResumeLoadedPublic(songA, songA, true, false))
        assertFalse(policy.canResumeLoadedPublic(songA, null, false, false))
    }

    @Test fun signedPrivateOrMalformedMediaIdCannotAuthorizeReuse() {
        val signed = "https://rr.example.googlevideo.com/videoplayback?sig=private"
        assertFalse(policy.canResumeLoadedPublic(signed, signed, false, false))
        assertFalse(policy.canResumeLoadedPublic(songA + "&token=private",
            songA + "&token=private", false, false))
        assertFalse(policy.canResumeLoadedPublic("", "", false, false))
        assertFalse(policy.canResumeLoadedPublic(null, null, false, false))
        assertFalse(policy.shouldResolveSavedSelection(
            true, true, signed, reusableLoaded = false, pendingMatching = false))
    }

    @Test fun pendingPublicResolutionDoesNotStartDuplicateExtractor() {
        assertFalse(policy.shouldResolveSavedSelection(
            true, true, songA, reusableLoaded = false, pendingMatching = true))
        assertFalse(policy.shouldResolveSavedSelection(
            true, true, songA, reusableLoaded = true, pendingMatching = false))
        assertTrue(policy.awaitsSameResolution(songA, songA))
    }

    @Test fun queueOnlyOrDifferentSelectionCannotTriggerSameQueueResume() {
        assertFalse(policy.shouldResolveSavedSelection(
            true, false, songA, reusableLoaded = false, pendingMatching = false))
        assertFalse(policy.shouldResolveSavedSelection(
            false, true, songA, reusableLoaded = false, pendingMatching = false))
        assertFalse(policy.shouldResolveSavedSelection(
            true, true, null, reusableLoaded = false, pendingMatching = false))
        assertFalse(policy.shouldResolveSavedSelection(
            true, true, "", reusableLoaded = false, pendingMatching = false))
    }

    @Test fun savedShuffleAndManualNextRemainUnchangedByResumeDecision() {
        val entries = listOf(songA, songB,
            "https://www.youtube.com/watch?v=J---aiyznGQ").map {
            QueueEntry(it, "Song", "Artist", "")
        }
        val queue = PlaybackQueue(pick = { 0 })
        queue.replace(entries, 0)
        queue.shuffle = true
        assertTrue(queue.insertNext(entries[2]))
        val tracksBefore = queue.snapshot()
        val priorityBefore = queue.priorityNextUrls()
        val shuffleBefore = queue.shuffleRecoveryState()
        val indexBefore = queue.index
        repeat(8) {
            assertTrue(policy.shouldResolveSavedSelection(
                sameQueue = true, explicitPlay = true,
                selectedId = queue.current?.url,
                reusableLoaded = false, pendingMatching = false))
        }
        assertEquals(tracksBefore, queue.snapshot())
        assertEquals(priorityBefore, queue.priorityNextUrls())
        assertEquals(shuffleBefore, queue.shuffleRecoveryState())
        assertEquals(indexBefore, queue.index)
        assertEquals(entries[2], queue.next())
    }

    @Test fun recoveryDoesNotMutateUserSelectedTrackOrForceAutoplay() {
        val q = PlaybackQueue(pick = { 0 })
        q.replace(listOf(QueueEntry(songA, "A", "Artist", ""),
            QueueEntry(songB, "B", "Artist", "")), 1)
        assertEquals(songB, q.current?.url)
        assertFalse(policy.shouldResolveSavedSelection(
            sameQueue = true, explicitPlay = false, selectedId = q.current?.url,
            reusableLoaded = false, pendingMatching = false))
        assertEquals(songB, q.current?.url)
        assertEquals(songA, q.previous()?.url) // original linear order remains
    }
}
