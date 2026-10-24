package com.nightwave.player.v2.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupConsentPolicyTest {
    private val initial = CloudBackupConsent(
        "demo@example.com", true, false, true, false, true
    )

    @Test fun unchangedAccountAndConsentMayConfirmUpload() {
        assertTrue(CloudBackupConsentPolicy.same(initial, initial.copy()))
        assertTrue(CloudBackupConsentPolicy.same(initial,
            initial.copy(account = "DEMO@EXAMPLE.COM")))
    }

    @Test fun accountDisconnectAndPrivacyChangesInvalidateStaleBackup() {
        assertFalse(CloudBackupConsentPolicy.same(initial, initial.copy(account = "")))
        assertFalse(CloudBackupConsentPolicy.same(initial,
            initial.copy(account = "other@example.com")))
        assertFalse(CloudBackupConsentPolicy.same(initial, initial.copy(enabled = false)))
        assertFalse(CloudBackupConsentPolicy.same(initial, initial.copy(history = true)))
        assertFalse(CloudBackupConsentPolicy.same(initial, initial.copy(lyrics = false)))
        assertFalse(CloudBackupConsentPolicy.same(initial, initial.copy(preferences = true)))
        assertFalse(CloudBackupConsentPolicy.same(initial, initial.copy(wifiOnly = false)))
    }
}
