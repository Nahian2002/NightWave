package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineLibraryHealthPolicyTest {
    @Test fun classifiesRegularAudioCorrectly() {
        assertEquals(OfflineFileHealth.AVAILABLE,
            OfflineLibraryHealthPolicy.classify(1024L, true, true, true, 1024L))
    }

    @Test fun marksMissingFilesWithoutAlteringIndex() {
        assertEquals(OfflineFileHealth.MISSING,
            OfflineLibraryHealthPolicy.classify(1024L, false, false, false, 0L))
    }

    @Test fun marksSizeChanges() {
        assertEquals(OfflineFileHealth.SIZE_CHANGED,
            OfflineLibraryHealthPolicy.classify(1024L, true, true, true, 13L))
        assertEquals(OfflineFileHealth.SIZE_CHANGED,
            OfflineLibraryHealthPolicy.classify(1024L, true, true, true, 2048L))
    }

    @Test fun requiresReadableRegularFile() {
        assertEquals(OfflineFileHealth.UNREADABLE,
            OfflineLibraryHealthPolicy.classify(1024L, true, false, true, 1024L))
        assertEquals(OfflineFileHealth.UNREADABLE,
            OfflineLibraryHealthPolicy.classify(1024L, true, true, false, 1024L))
    }

    @Test fun requiresBoundedRecordedBytes() {
        assertEquals(OfflineFileHealth.UNSAFE_METADATA,
            OfflineLibraryHealthPolicy.classify(0L, true, true, true, 0L))
        assertEquals(OfflineFileHealth.UNSAFE_METADATA,
            OfflineLibraryHealthPolicy.classify(
                OfflineAudioLibrary.MAX_SONG_BYTES + 1L, true, true, true,
                OfflineAudioLibrary.MAX_SONG_BYTES + 1L))
    }

    @Test fun healthReportCountsOnlyAvailableTracks() {
        val a = OfflineSong("example-a.mp3", "A", "audio/mpeg", 1024L)
        val b = OfflineSong("example-b.mp3", "B", "audio/mpeg", 1024L)
        val report = OfflineLibraryHealthReport(true, listOf(
            OfflineLibraryHealthEntry(a, OfflineFileHealth.AVAILABLE),
            OfflineLibraryHealthEntry(b, OfflineFileHealth.MISSING)
        ))
        assertEquals(1, report.playableCount)
        assertEquals(1, report.attentionCount)
        assertEquals(listOf(a, b), report.entries.map { it.song })
    }
}
