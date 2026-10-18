package com.nightwave.player.v2.locker

import org.junit.Assert.*
import org.junit.Test

class LockerCheckpointReadinessTest {
    private val remote = LockerRemoteTrack(
        "DriveFile123", "Owned.mp3", "audio/mpeg", 10_000L,
        "a".repeat(32), "b".repeat(64)
    )
    private fun pending(committed: Long = 4_000L) = LockerPendingDownload(
        "owner@example.test", remote,
        "00000000-0000-0000-0000-000000000001.pending", committed
    )
    private fun state(
        item: LockerPendingDownload? = pending(),
        broken: Boolean = false,
        sameAccount: Boolean = true,
        exists: Boolean = true,
        regular: Boolean = true,
        unsafe: Boolean = false,
        bytes: Long = 4_000L
    ) = LockerCheckpointReadiness.classify(
        item, broken, sameAccount, exists, regular, unsafe, bytes
    )

    @Test fun missingCheckpointDoesNotInventDownload() {
        assertEquals(LockerCheckpointState.NONE, state(item = null))
    }

    @Test fun corruptCheckpointRequestsExplicitDiscardOnly() {
        assertEquals(LockerCheckpointState.NEEDS_REPAIR,
            state(item = null, broken = true))
    }

    @Test fun originalGoogleAccountMustMatchBeforeResumption() {
        assertEquals(LockerCheckpointState.ACCOUNT_MISMATCH,
            state(sameAccount = false))
    }

    @Test fun committedMissingFileCannotResume() {
        val result = state(exists = false, regular = false, bytes = 0)
        assertEquals(LockerCheckpointState.MISSING_DATA, result)
    }

    @Test fun missingFreshZeroByteStagingIsAllowed() {
        assertEquals(LockerCheckpointState.READY,
            state(item = pending(committed = 0), exists = false,
                regular = false, bytes = 0))
    }

    @Test fun shortFileCannotResumeAndIsNeverAutoDeleted() {
        assertEquals(LockerCheckpointState.SHORT_DATA,
            state(bytes = 1_024L))
    }

    @Test fun symlinkOrDirectoryCannotEnterRecovery() {
        assertEquals(LockerCheckpointState.UNSAFE_STAGING,
            state(unsafe = true))
        assertEquals(LockerCheckpointState.UNSAFE_STAGING,
            state(regular = false))
    }

    @Test fun extraUncommittedBytesAreNotClaimedVerified() {
        assertEquals(LockerCheckpointState.READY,
            state(bytes = 5_000))
    }

    @Test fun completedByteCountStillRequiresChecksumVerification() {
        val result = state(item = pending(committed = remote.bytes), bytes = remote.bytes)
        assertEquals(LockerCheckpointState.HASH_CHECK_PENDING, result)
        val preview = LockerCheckpointPreview(result, pending(remote.bytes), remote.bytes)
        assertTrue(preview.canResume)
        assertTrue(preview.explanation.contains("SHA-256"))
    }

    @Test fun unsafeAndMissingStatesHaveNoResumeAction() {
        assertFalse(LockerCheckpointPreview(LockerCheckpointState.MISSING_DATA).canResume)
        assertFalse(LockerCheckpointPreview(LockerCheckpointState.NEEDS_REPAIR).canResume)
        assertFalse(LockerCheckpointPreview(LockerCheckpointState.ACCOUNT_MISMATCH).canResume)
    }
}
