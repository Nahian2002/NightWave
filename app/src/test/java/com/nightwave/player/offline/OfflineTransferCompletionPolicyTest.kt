package com.nightwave.player.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineTransferCompletionPolicyTest {
    private val p = OfflineTransferCompletionPolicy

    @Test fun onlyIndexedReadableAndCurrentAudioIsCommitEligible() {
        val verified = setOf("song-1", "song-2")
        assertTrue(p.canCommit(false, "song-1", verified))
        assertTrue(p.canCommit(false, "song-2", verified))
        assertFalse(p.canCommit(false, "old-or-deleted", verified))
        assertFalse(p.canCommit(false, "", verified))
        assertFalse(p.canCommit(false, null, verified))
        assertFalse(p.canCommit(false, "song-1", emptySet()))
    }

    @Test fun stopAfterImportCannotFalselyMarkWorkSuccessful() {
        assertFalse(p.canCommit(true, "song-1", setOf("song-1")))
        assertFalse(p.canCommit(true, null, emptySet()))
        assertTrue(p.canCommit(false, "song-1", setOf("song-1")))
    }

    @Test fun ambiguousOrFailedFileProofShowsHonestRecovery() {
        assertFalse(p.canCommit(false, "song-1", setOf("song-2")))
        assertTrue(p.NOT_VERIFIED.contains("Check Offline Library"))
        assertEquals("Saved audio could not be verified. Check Offline Library before retrying.",
            p.NOT_VERIFIED)
    }
}
