package com.nightwave.player.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineDownloadStatusPolicyTest {
    @Test fun completedWorkNeedsAnActualReadableLibraryFile() {
        val present = setOf("audio-1.mp3")
        assertTrue(OfflineDownloadStatusPolicy.isReady(
            true, "audio-1.mp3", present))
        assertFalse(OfflineDownloadStatusPolicy.isReady(
            true, "audio-deleted.mp3", present))
    }

    @Test fun queuedFailedOrCancelledTransfersAreNeverOfflineReady() {
        assertFalse(OfflineDownloadStatusPolicy.isReady(
            false, "audio-1.mp3", setOf("audio-1.mp3")))
        assertFalse(OfflineDownloadStatusPolicy.isReady(
            true, null, setOf("audio-1.mp3")))
        assertFalse(OfflineDownloadStatusPolicy.isReady(
            true, "", setOf("audio-1.mp3")))
        assertFalse(OfflineDownloadStatusPolicy.isReady(
            true, "audio-1.mp3", emptySet()))
    }

    @Test fun oldSuccessMetadataDoesNotRemainMisleadingAfterDeletion() {
        assertEquals("Ready offline · Verified local file",
            OfflineDownloadStatusPolicy.statusForCompleted(true))
        assertEquals("Saved previously · Local audio is missing or unavailable",
            OfflineDownloadStatusPolicy.statusForCompleted(false))
    }
}
