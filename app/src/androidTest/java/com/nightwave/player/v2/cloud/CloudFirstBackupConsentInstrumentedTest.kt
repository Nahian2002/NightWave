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

    @Test fun invalidTimestampsCannotClaimACompletedBackup() = scenario { store ->
        assertTrue(store.connect("temporary@example.com"))
        val consent = store.read().consentSnapshot()
        assertFalse(store.successIfCurrent(consent, 0L, created = true))
        assertFalse(store.successIfCurrent(consent, -1L, created = false))
        assertFalse(store.success("temporary@example.com", 0L))
        assertFalse(store.success("temporary@example.com", -10L))
        val after = store.read()
        assertFalse(after.enabled)
        assertEquals(0L, after.lastSuccessMs)
        assertEquals("Account authorized; initial backup pending", after.lastResult)
    }
}
