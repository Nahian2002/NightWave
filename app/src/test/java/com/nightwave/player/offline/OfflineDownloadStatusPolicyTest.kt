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
        assertEquals("Ready offline · Local file present (size checked)",
            OfflineDownloadStatusPolicy.statusForCompleted(true))
        assertEquals("Saved previously · No available indexed audio",
            OfflineDownloadStatusPolicy.statusForCompleted(false))
    }

    @Test fun missingFileDoesNotBecomeReadyAfterOldWorkSuccess() {
        assertFalse(OfflineDownloadStatusPolicy.isReady(true, "missing-id", emptySet()))
        assertEquals("Saved previously · Audio file missing (index retained)",
            OfflineDownloadStatusPolicy.statusForCompleted(
                false, OfflineFileHealth.MISSING))
    }

    @Test fun staleSizeCannotBeMisrepresentedAsPlayabilityProof() {
        assertEquals("Saved previously · File size changed (not ready)",
            OfflineDownloadStatusPolicy.statusForCompleted(
                false, OfflineFileHealth.SIZE_CHANGED))
        assertEquals("Ready offline · Local file present (size checked)",
            OfflineDownloadStatusPolicy.statusForCompleted(
                true, OfflineFileHealth.AVAILABLE))
    }

    @Test fun unreadableAndUnsafeFilesGetHonestExplanations() {
        assertEquals("Saved previously · File unreadable (not ready)",
            OfflineDownloadStatusPolicy.statusForCompleted(
                false, OfflineFileHealth.UNREADABLE))
        assertEquals("Saved previously · Unsafe file metadata (not ready)",
            OfflineDownloadStatusPolicy.statusForCompleted(
                false, OfflineFileHealth.UNSAFE_METADATA))
    }
}
