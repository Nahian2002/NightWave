package com.nightwave.player.v2.cloud

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Uses isolated preferences, never the listener's real Google account. */
@RunWith(AndroidJUnit4::class)
class CloudFirstBackupConsentInstrumentedTest {
    private class Sandbox(base: Context, private val suffix: String) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences(name + suffix, mode)
    }

    private fun scenario(block: (CloudBackupState) -> Unit) {
        val base: Context = ApplicationProvider.getApplicationContext()
        val sandbox = Sandbox(base, "_nw45_" + UUID.randomUUID())
        val prefs = sandbox.getSharedPreferences("nightwave_cloud_backup_v1",
            Context.MODE_PRIVATE)
        try { block(CloudBackupState(sandbox)) }
        finally { prefs.edit().clear().commit() }
    }

    @Test fun initialManualBackupRequiresIdenticalConsentAndEnablesVerifiedState() = scenario { store ->
        assertTrue(store.connect("temporary@example.com"))
        val consent = store.read().consentSnapshot()
        assertFalse(consent.enabled) // No backup has been verified yet.
        assertTrue(store.successIfCurrent(consent, 12345678L, created = true))
        val after = store.read()
        assertTrue(after.enabled)
        assertEquals(12345678L, after.lastSuccessMs)
        assertEquals("Backup verified", after.lastResult)
    }

    @Test fun privacyChangePreventsStaleUploadFromBeingMarkedVerified() = scenario { store ->
        assertTrue(store.connect("temporary@example.com"))
        val old = store.read().consentSnapshot()
        assertTrue(store.setHistory(true))
        assertFalse(store.successIfCurrent(old, 999999L, created = true))
        assertFalse(store.read().enabled)
        assertEquals(0L, store.read().lastSuccessMs)
    }

    @Test fun disconnectOrReconnectionCannotInheritPendingSuccess() = scenario { store ->
        assertTrue(store.connect("temporary@example.com"))
        val old = store.read().consentSnapshot()
        assertTrue(store.disconnect())
        assertTrue(store.connect("temporary@example.com"))
        assertFalse(store.successIfCurrent(old, 999999L, created = true))
        assertFalse(store.read().enabled)
        assertEquals(0L, store.read().lastSuccessMs)
    }

    @Test fun unchangedSnapshotNeverTurnsIntoASecondRemoteUploadClaim() = scenario { store ->
        assertTrue(store.connect("temporary@example.com"))
        val consent = store.read().consentSnapshot()
        assertTrue(store.successIfCurrent(consent, 98765L, created = false))
        assertEquals("Already backed up; library unchanged", store.read().lastResult)
    }

    @Test fun invalidCompletionTimeCannotPretendBackupWasReadBack() = scenario { store ->
        assertTrue(store.connect("first@example.com"))
        val consent = store.read().consentSnapshot()
        assertFalse(store.successIfCurrent(consent, 0L, created = true))
        assertFalse(store.successIfCurrent(consent, -1L, created = false))
        assertFalse(store.success("first@example.com", 0L))
        assertFalse(store.success("first@example.com", -1L))
        val state = store.read()
        assertFalse(state.enabled)
        assertEquals(0L, state.lastSuccessMs)
        assertEquals(-1L, state.lastVerifiedRevision)
        assertEquals(CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP,
            CloudAccountPresentation.stage(state))
    }

    @Test fun checkedAccountIsNotAFirstBackupAndDisconnectClearsOnlyAccessProof() = scenario { store ->
        assertTrue(store.connect("disposable@example.test"))
        assertEquals(CloudAccessState.CHECKED, store.read().accessState)
        assertTrue(store.read().lastAccessCheckMs > 0L)
        assertFalse(store.read().enabled)
        assertEquals(0L, store.read().lastSuccessMs)
        assertTrue(store.disconnect())
        assertEquals(CloudAccessState.UNKNOWN, store.read().accessState)
        assertEquals(0L, store.read().lastAccessCheckMs)
    }

    @Test fun oldAccountOrPrivacyRevisionCannotOverwriteAccessState() = scenario { store ->
        assertTrue(store.connect("disposable@example.test"))
        val old = store.read().consentSnapshot()
        assertTrue(store.setHistory(true))
        assertFalse(store.accessIfCurrent(old, CloudAccessState.NEEDS_REVIEW))
        assertFalse(store.accessIfCurrent(old, CloudAccessState.CHECKED, 1234L))
        assertEquals(CloudAccessState.CHECKED, store.read().accessState)
    }

    @Test fun accessFailurePreservesTheVerifiedBackupAndPrivacyChoices() = scenario { store ->
        assertTrue(store.connect("disposable@example.test"))
        assertTrue(store.successIfCurrent(store.read().consentSnapshot(), 1234L, true))
        val before = store.read()
        assertTrue(store.accessIfCurrent(before.consentSnapshot(), CloudAccessState.NEEDS_REVIEW))
        val after = store.read()
        assertEquals(CloudAccessState.NEEDS_REVIEW, after.accessState)
        assertEquals(before.consentSnapshot(), after.consentSnapshot())
        assertEquals(before.lastVerifiedRevision, after.lastVerifiedRevision)
        assertEquals(before.lastSuccessMs, after.lastSuccessMs)
    }

    @Test fun olderFailureCannotReplaceNewAccessProofEvenInTheSameMillisecond() = scenario { store ->
        assertTrue(store.connect("disposable@example.test"))
        val consent = store.read().consentSnapshot()
        val older = store.recordAccessIfCurrent(consent, CloudAccessState.CHECKED, 1000L)!!
        val newer = store.recordAccessIfCurrent(consent, CloudAccessState.CHECKED, 1000L)!!
        assertTrue(older != newer)
        assertEquals(null, store.recordAccessIfCurrent(consent, CloudAccessState.NEEDS_REVIEW,
            expectedAccessRevision = older))
        assertEquals(CloudAccessState.CHECKED, store.read().accessState)
        assertEquals(newer, store.read().accessRevision)
        assertTrue(store.recordAccessIfCurrent(consent, CloudAccessState.NEEDS_REVIEW,
            expectedAccessRevision = newer) != null)
    }

    @Test fun differentAccountCannotInheritAnyPreviousVerifiedSnapshot() = scenario { store ->
        assertTrue(store.connect("first@example.com"))
        val old = store.read().consentSnapshot()
        assertTrue(store.successIfCurrent(old, 1000L, created = true))
        assertEquals(CloudAccountPresentation.BackupStage.VERIFIED_BACKUP,
            CloudAccountPresentation.stage(store.read()))
        assertTrue(store.connect("second@example.com"))
        val switched = store.read()
        assertFalse(switched.enabled)
        assertEquals(0L, switched.lastSuccessMs)
        assertEquals(-1L, switched.lastVerifiedRevision)
        assertFalse(store.successIfCurrent(old, 2000L, created = true))
        assertFalse(store.success("first@example.com", 2000L))
        assertEquals(CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP,
            CloudAccountPresentation.stage(store.read()))
    }

    @Test fun reconnectingSameAccountInvalidatesOldWorkerReceipt() = scenario { store ->
        assertTrue(store.connect("first@example.com"))
        val old = store.read().consentSnapshot()
        assertTrue(store.successIfCurrent(old, 1000L, created = true))
        assertTrue(store.disconnect())
        assertTrue(store.connect("first@example.com"))
        assertFalse(store.successIfCurrent(old, 2000L, created = true))
        assertEquals(0L, store.read().lastSuccessMs)
        assertEquals(CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP,
            CloudAccountPresentation.stage(store.read()))
    }

    @Test fun delayedFailureCannotOverwriteStatusAfterAccountSwitch() = scenario { store ->
        assertTrue(store.connect("first@example.com"))
        val oldConsent = store.read().consentSnapshot()
        assertTrue(store.connect("second@example.com"))
        val newMessage = store.read().lastResult
        assertFalse(store.statusIfCurrent(oldConsent, "Stale Drive failure"))
        assertEquals(newMessage, store.read().lastResult)
        assertEquals("second@example.com", store.read().account)
    }

    @Test fun delayedFailureCannotOverwriteStatusAfterPrivacyChange() = scenario { store ->
        assertTrue(store.connect("first@example.com"))
        val consent = store.read().consentSnapshot()
        assertTrue(store.setHistory(true))
        val newerStatus = store.read().lastResult
        assertFalse(store.statusIfCurrent(consent, "Stale Drive failure"))
        assertEquals(newerStatus, store.read().lastResult)
        assertTrue(store.statusIfCurrent(store.read().consentSnapshot(),
            "Drive temporarily unavailable"))
        assertEquals("Drive temporarily unavailable", store.read().lastResult)
    }
}
