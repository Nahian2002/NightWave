package com.nightwave.player.v2.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.v2.model.NightWaveTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDataStoreMirrorInstrumentedTest {
    @Test fun mirrorsGuestSettingsWithoutDeletingOriginalPreferences() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val legacy = GuestProfileStore(app)
        val original = legacy.load()
        val edited = original.copy(
            displayName = "DataStore transition test",
            avatarStyle = "vinyl",
            theme = NightWaveTheme.AURORA
        )
        assertTrue(legacy.save(edited))
        try {
            val mirror = ProfileDataStoreMirror(app)
            val result = mirror.reconcile()
            assertEquals("DataStore transition test", result.displayName)
            val persisted = mirror.observe.first()
            assertNotNull(persisted)
            assertEquals(edited.localId, persisted!!.localId)
            assertEquals("vinyl", persisted.avatarStyle)
            assertEquals(NightWaveTheme.AURORA, persisted.theme)
            assertEquals("DataStore transition test", legacy.load().displayName)
            assertFalse(mirror.reconcile().changed)
        } finally {
            // Legacy is authoritative while Phase 3B remains a preview.
            legacy.save(original)
            ProfileDataStoreMirror(app).reconcile()
        }
    }
}
