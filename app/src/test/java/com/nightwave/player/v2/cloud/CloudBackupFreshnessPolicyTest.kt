package com.nightwave.player.v2.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupFreshnessPolicyTest {
    private val p = CloudBackupFreshnessPolicy

    @Test fun previouslyVerifiedBackupIsNotCurrentAfterConsentChanges() {
        assertEquals(CloudBackupFreshnessPolicy.Freshness.CURRENT_VERIFIED, p.classify(true, true, 1000L, 12, 12))
        assertEquals(CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED, p.classify(true, true, 1000L, 13, 12))
        assertEquals(CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED, p.classify(true, true, 1000L, 13, -1))
    }

    @Test fun unconnectedAndBeforeFirstVerifiedUploadAreNotReportedAsReady() {
        assertEquals(CloudBackupFreshnessPolicy.Freshness.LOCAL_ONLY, p.classify(false, true, 1000L, 1, 1))
        assertEquals(CloudBackupFreshnessPolicy.Freshness.CONNECTED_PENDING, p.classify(true, false, 1000L, 1, 1))
        assertEquals(CloudBackupFreshnessPolicy.Freshness.CONNECTED_PENDING, p.classify(true, true, 0L, 1, 1))
    }

    @Test fun repeatedSettingsChoiceDoesNotArtificiallyInvalidateBackup() {
        assertFalse(p.changed(false, false))
        assertFalse(p.changed(true, true))
        assertTrue(p.changed(false, true))
        assertTrue(p.changed(true, false))
    }

    @Test fun verifiedRevisionOnlyBecomesCurrentAfterReadback() {
        assertEquals(CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED, p.classify(true, true, 800L, 99, 98))
        assertEquals(CloudBackupFreshnessPolicy.Freshness.CURRENT_VERIFIED, p.classify(true, true, 900L, 99, 99))
    }
}
