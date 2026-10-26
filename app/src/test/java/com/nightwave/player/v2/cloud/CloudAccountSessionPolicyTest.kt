package com.nightwave.player.v2.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudAccountSessionPolicyTest {
    private val session = CloudBackupConsent("demo@example.com",
        enabled = false, history = false, lyrics = false,
        preferences = false, wifiOnly = true, revision = 7)

    @Test fun currentSessionAllowsReviewedPreviewAfterSameAccountVerification() {
        assertTrue(CloudAccountSessionPolicy.same(session, session.copy()))
        assertTrue(CloudAccountSessionPolicy.mayApply(session,
            session.copy(account = "DEMO@EXAMPLE.COM"), false))
    }

    @Test fun sameEmailAfterDisconnectReconnectInvalidatesOldPreview() {
        assertFalse(CloudAccountSessionPolicy.same(session,
            session.copy(revision = 9L)))
        assertFalse(CloudAccountSessionPolicy.mayApply(session,
            session.copy(revision = 9L), false))
        assertFalse(CloudAccountSessionPolicy.same(session,
            session.copy(account = "another@example.com")))
        assertFalse(CloudAccountSessionPolicy.same(session,
            session.copy(account = "")))
    }

    @Test fun privacyChangeOrActiveActionCannotApplyAnOldPreview() {
        assertFalse(CloudAccountSessionPolicy.mayApply(
            session, session.copy(revision = 8), false))
        assertFalse(CloudAccountSessionPolicy.mayApply(session, session, true))
        assertTrue(CloudAccountSessionPolicy.same(session,
            session.copy(enabled = true))) // backup status alone does not rotate account
    }

    @Test fun blankAccountsAndSessionWrapCannotAuthorize() {
        assertFalse(CloudAccountSessionPolicy.same(session.copy(account = ""),
            session.copy(account = "")))
        assertFalse(CloudAccountSessionPolicy.same(session,
            session.copy(revision = Long.MAX_VALUE)))
    }
}
