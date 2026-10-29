package com.nightwave.player.v2.cloud

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.v2.data.GuestProfileStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Isolated preference namespaces only; no Google requests or listener data. */
@RunWith(AndroidJUnit4::class)
class NW139RecoveryReceiptInstrumentedTest {
    private class Sandbox(base: Context) : ContextWrapper(base) {
        private val suffix = "_nw139_" + UUID.randomUUID()
        private val names = mutableSetOf<String>()
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            names += name
            return super.getSharedPreferences(name + suffix, mode)
        }
        fun clear() { names.forEach { super.getSharedPreferences(it + suffix, MODE_PRIVATE).edit().clear().commit() } }
    }
    private fun scenario(block: (CloudBackupState, Sandbox) -> Unit) {
        val context = Sandbox(ApplicationProvider.getApplicationContext())
        try { block(CloudBackupState(context), context) } finally { context.clear() }
    }
    @Test fun newerSameTimestampProofRejectsOldSuccessAndStatusWithoutErasingBackup() = scenario { store, _ ->
        assertTrue(store.connect("fixture@example.test"))
        val before = store.read()
        val old = store.recordAccessIfCurrent(before.consentSnapshot(), CloudAccessState.CHECKED, 555L)!!
        val newer = store.recordAccessIfCurrent(before.consentSnapshot(), CloudAccessState.CHECKED, 555L)!!
        assertNotEquals(old, newer)
        assertFalse(store.successIfCurrent(before.consentSnapshot(), 999L, true, old))
        assertFalse(store.statusIfCurrent(before.consentSnapshot(), "stale failure", old))
        assertEquals(newer, store.read().accessRevision)
        assertTrue(store.successIfCurrent(before.consentSnapshot(), 999L, true, newer))
        val verified = store.read()
        assertEquals(999L, verified.lastSuccessMs)
        assertNull(store.recordAccessIfCurrent(verified.consentSnapshot(), CloudAccessState.NEEDS_REVIEW,
            555L, old))
        assertEquals(verified, store.read())
    }
    @Test fun localCommitRejectsChangedAccessBeforeInvokingAnyWriter() = scenario { store, _ ->
        assertTrue(store.connect("fixture@example.test"))
        val state = store.read()
        val receipt = CloudRecoverySession(state.consentSnapshot(), state.accessRevision)
        var writes = 0
        assertEquals("ok", store.withRecoverySession(receipt) { writes++; "ok" })
        assertTrue(store.accessIfCurrent(state.consentSnapshot(), CloudAccessState.CHECKED, 123L))
        assertNull(store.withRecoverySession(receipt) { writes++; "stale" })
        assertEquals(1, writes)
    }
    @Test fun privacyOrReconnectCannotReuseAnyEarlierCommitReceipt() = scenario { store, _ ->
        assertTrue(store.connect("fixture@example.test"))
        val state = store.read()
        val receipt = CloudRecoverySession(state.consentSnapshot(), state.accessRevision)
        assertTrue(store.setLyrics(true))
        assertNull(store.withRecoverySession(receipt) { error("Must not run") })
        assertTrue(store.disconnect()); assertTrue(store.connect("fixture@example.test"))
        assertNull(store.withRecoverySession(receipt) { error("Must not run") })
    }
    @Test fun cancellingPermissionKeepsProofWhileScopedStatusChanges() = scenario { store, _ ->
        assertTrue(store.connect("fixture@example.test"))
        val old = store.read()
        assertTrue(store.statusIfCurrent(old.consentSnapshot(), "Google permission not completed", old.accessRevision))
        assertEquals(old.accessRevision, store.read().accessRevision)
        assertEquals(old.accessState, store.read().accessState)
        assertEquals(old.lastSuccessMs, store.read().lastSuccessMs)
    }
    @Test fun optionalPreferencesRejectNewerLocalEditsAndKeepIdentity() = scenario { _, context ->
        val profiles = GuestProfileStore(context)
        val baseline = profiles.load()
        assertTrue(profiles.save(baseline.copy(displayName = "New local edit")))
        assertFalse(profiles.saveIfUnchanged(baseline, baseline.copy(crossfadeSeconds = 4)))
        val current = profiles.load()
        assertEquals("New local edit", current.displayName)
        assertEquals(baseline.localId, current.localId)
        assertTrue(profiles.saveIfUnchanged(current, current.copy(crossfadeSeconds = 4)))
        assertEquals("New local edit", profiles.load().displayName)
    }
    @Test fun firstConnectAllowsBlankBaselineButLateConnectCannotReplaceNewerProof() = scenario { store, _ ->
        val empty = store.read()
        assertTrue(store.connect("fixture@example.test", expected = empty))
        val started = store.read()
        assertTrue(store.accessIfCurrent(started.consentSnapshot(), CloudAccessState.CHECKED, 123L))
        val newer = store.read()
        assertFalse(store.connect("different@example.test", expected = started))
        assertEquals(newer, store.read())
        assertTrue(store.connect("different@example.test", expected = newer))
        assertEquals("different@example.test", store.read().account)
    }
}
