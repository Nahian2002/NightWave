package com.nightwave.player.v2.locker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Policy-level upload recovery coverage. No Google account or personal files. */
class LockerUploadResumePolicyTest {
    @Test fun noRangeMeansNoBytesAccepted() {
        assertEquals(0L, LockerUploadResumePolicy.acknowledged(null, 2000000, 0))
    }
    @Test fun firstFullChunkAcknowledgementIsNextStart() {
        assertEquals(1048576L, LockerUploadResumePolicy.acknowledged(
            "bytes=0-1048575", 2000000, 0))
    }
    @Test fun repeatedStatusCanReportPreviouslyKnownProgress() {
        assertEquals(1048576L, LockerUploadResumePolicy.acknowledged(
            "bytes=0-1048575", 2000000, 1048576))
    }
    @Test fun statusProbeCanShowMoreProgressThanLastAcknowledgement() {
        assertEquals(1572864L, LockerUploadResumePolicy.acknowledged(
            "bytes=0-1572863", 2000000, 1048576))
    }
    @Test fun missingRangeAfterCommittedBytesMustFailClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.acknowledged(null, 2000000, 1048576)
        }
    }
    @Test fun staleRangeCannotRewindUpload() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.acknowledged("bytes=0-1000", 2000000, 1048576)
        }
    }
    @Test fun malformedRangeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.acknowledged("bytes=1-100", 2000000, 0)
        }
    }
    @Test fun rangeCannotExceedFileLength() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.acknowledged("bytes=0-2000000", 2000000, 0)
        }
    }
    @Test fun rangeOfEntireFileCanBeReported() {
        assertEquals(2000000L, LockerUploadResumePolicy.acknowledged(
            "bytes=0-1999999", 2000000, 1000000))
    }
    @Test fun zeroOrHugeFileRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.acknowledged(null, 0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.acknowledged(
                null, LockerAudioPolicy.MAX_FILE_BYTES + 1, 0)
        }
    }
    @Test fun firstChunkIsAligned() {
        assertTrue(LockerUploadResumePolicy.canSendNext(0, 2000000))
    }
    @Test fun advancedWholeChunkIsAligned() {
        assertTrue(LockerUploadResumePolicy.canSendNext(1048576, 2000000))
    }
    @Test fun misalignedNonfinalChunkIsBlocked() {
        assertFalse(LockerUploadResumePolicy.canSendNext(200000, 2000000))
    }
    @Test fun misalignedFinalChunkIsAllowed() {
        assertTrue(LockerUploadResumePolicy.canSendNext(1750000, 2000000))
    }
    @Test fun cannotSendPastEnd() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.canSendNext(2000000, 2000000)
        }
    }
    @Test fun temporaryDriveStatusesCanBeProbed() {
        assertTrue(LockerUploadResumePolicy.transientStatus(408))
        assertTrue(LockerUploadResumePolicy.transientStatus(429))
        assertTrue(LockerUploadResumePolicy.transientStatus(503))
        assertTrue(LockerUploadResumePolicy.transientStatus(500))
    }
    @Test fun permissionDenialIsNotATransientStatus() {
        assertFalse(LockerUploadResumePolicy.transientStatus(401))
        assertFalse(LockerUploadResumePolicy.transientStatus(403))
        assertFalse(LockerUploadResumePolicy.transientStatus(404))
    }
    @Test fun threeConsecutiveFailuresStopRetries() {
        assertTrue(LockerUploadResumePolicy.retryAllowed(1))
        assertTrue(LockerUploadResumePolicy.retryAllowed(2))
        assertFalse(LockerUploadResumePolicy.retryAllowed(3))
    }
    @Test fun noFailureNeedsNoRetry() {
        assertFalse(LockerUploadResumePolicy.retryAllowed(0))
    }
    @Test fun rangeCannotHaveNegativeOrExtraSuffix() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.acknowledged("bytes=0--1", 2000000, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LockerUploadResumePolicy.acknowledged("bytes=0-10,20-30", 2000000, 0)
        }
    }
}
