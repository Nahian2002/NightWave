package com.nightwave.player.v2.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupFreshnessPolicyTest {
    private val p = CloudBackupFreshnessPolicy
    private val F = CloudBackupFreshnessPolicy.Freshness

    @Test fun previouslyVerifiedBackupIsNotCurrentAfterConsentChanges() {
        assertEquals(F.CURRENT_VERIFIED, p.classify(true, true, 1000L, 12, 12))
        assertEquals(F.PREVIOUSLY_VERIFIED, p.classify(true, true, 1000L, 13, 12))
        assertEquals(F.PREVIOUSLY_VERIFIED, p.classify(true, true, 1000L, 13, -1))
    }

    @Test fun unconnectedAndBeforeFirstVerifiedUploadAreNotReportedAsReady() {
        assertEquals(F.LOCAL_ONLY, p.classify(false, true, 1000L, 1, 1))
        assertEquals(F.CONNECTED_PENDING, p.classify(true, false, 1000L, 1, 1))
        assertEquals(F.CONNECTED_PENDING, p.classify(true, true, 0L, 1, 1))
    }

    @Test fun repeatedSettingsChoiceDoesNotArtificiallyInvalidateBackup() {
        assertFalse(p.changed(false, false))
        assertFalse(p.changed(true, true))
        assertTrue(p.changed(false, true))
        assertTrue(p.changed(true, false))
    }

    @Test fun verifiedRevisionOnlyBecomesCurrentAfterReadback() {
        assertEquals(F.PREVIOUSLY_VERIFIED, p.classify(true, true, 800L, 99, 98))
        assertEquals(F.CURRENT_VERIFIED, p.classify(true, true, 900L, 99, 99))
    }
}
