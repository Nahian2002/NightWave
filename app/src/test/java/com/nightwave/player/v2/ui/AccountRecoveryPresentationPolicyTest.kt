package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class AccountRecoveryPresentationPolicyTest {
    @Test fun compactAndLargeFontScreensReserveMoreSpaceForText() {
        for (width in listOf(204f, 320f, 345f)) {
            val layout = AccountRecoveryPresentationPolicy.layout(width, 1f)
            assertTrue(layout.compact)
            assertEquals(14, layout.paddingDp)
            assertEquals(44, layout.avatarDp)
        }
        assertTrue(AccountRecoveryPresentationPolicy.layout(560f, 2f).compact)
        assertFalse(AccountRecoveryPresentationPolicy.layout(412f, 1f).compact)
    }

    @Test fun invalidGeometryCannotProduceUnboundedSpacing() {
        for (width in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            val layout = AccountRecoveryPresentationPolicy.layout(width, Float.NaN)
            assertTrue(layout.compact)
            assertTrue(layout.paddingDp in 14..20)
        }
    }

    @Test fun thePrimaryActionFollowsAccountAndRecoveryTruth() {
        assertEquals("Connect Google Drive", AccountRecoveryPresentationPolicy.primaryAction(false, true, true))
        assertEquals("Reconnect Google Drive", AccountRecoveryPresentationPolicy.primaryAction(true, true, true))
        assertEquals("Create first backup", AccountRecoveryPresentationPolicy.primaryAction(true, false, false))
        assertEquals("Back up library now", AccountRecoveryPresentationPolicy.primaryAction(true, false, true))
    }

    @Test fun historicalBackupIsNotPresentedAsContinuousSyncOrCurrentAccess() {
        assertEquals("Drive needs attention", AccountRecoveryPresentationPolicy.protectionTitle(true, true, true, false))
        assertEquals("Last backup verified", AccountRecoveryPresentationPolicy.protectionTitle(true, false, true, false))
        assertEquals("Refresh your backup", AccountRecoveryPresentationPolicy.protectionTitle(true, false, true, true))
        assertEquals("Create your recovery copy", AccountRecoveryPresentationPolicy.protectionTitle(true, false, false, false))
    }
}
