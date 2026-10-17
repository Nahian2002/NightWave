package com.nightwave.player.v2.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs ONLY on a disposable Android test profile: clears test preferences. */
@RunWith(AndroidJUnit4::class)
class GuestProfileStartupInstrumentedTest {
    private val app: Context = ApplicationProvider.getApplicationContext()
    private val prefs = app.getSharedPreferences(
        "nightwave_v2_guest_profile_1", Context.MODE_PRIVATE
    )

    @Before fun setup() { assertTrue(prefs.edit().clear().commit()) }
    @After fun cleanup() { prefs.edit().clear().commit() }

    @Test fun wrongTypedLegacySettingDoesNotCrashProfileLoadOrEraseValue() {
        prefs.edit().putString("local_id", "stable-local-id")
            .putInt("display_name", 42)
            .putInt("theme", 17)
            .putString("crossfade_seconds", "not-an-int")
            .putString("stay_awake", "not-boolean")
            .commit()
        val result = GuestProfileStore(app).load()
        assertEquals("stable-local-id", result.localId)
        assertEquals("NightWave Listener", result.displayName)
        assertEquals(NightWaveTheme.MIDNIGHT, result.theme)
        assertEquals(42, prefs.getInt("display_name", 0))
        assertEquals("not-an-int", prefs.getString("crossfade_seconds", null))
    }

    @Test fun malformedProfileIdentityIsPreservedAndCannotBeOverwritten() {
        prefs.edit().putInt("local_id", 123).commit()
        val store = GuestProfileStore(app)
        val loaded = store.load()
        assertEquals("corrupt-guest", loaded.localId)
        assertFalse(store.save(loaded.copy(displayName = "Try to overwrite")))
        assertEquals(123, prefs.getInt("local_id", 0))
    }

    @Test fun newProfileIdPersistsAndCanBeSafelyEdited() {
        val store = GuestProfileStore(app)
        val initial = store.load()
        assertTrue(initial.localId.isNotBlank())
        assertEquals(initial.localId, GuestProfileStore(app).load().localId)
        assertTrue(store.save(initial.copy(displayName = "Night listener")))
        assertEquals("Night listener", store.load().displayName)
    }
}
