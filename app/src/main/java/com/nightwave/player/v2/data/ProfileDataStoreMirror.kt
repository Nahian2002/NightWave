package com.nightwave.player.v2.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nightwave.player.v2.model.AudioQualityPreference
import com.nightwave.player.v2.model.ListeningPrivacy
import com.nightwave.player.v2.model.MotionPreference
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.model.ProfileSettings
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Mirrors guest settings into Preferences DataStore. Does not invoke Android's
 * destructive SharedPreferencesMigration: Phase 1/2 UIs still rely on that
 * original SharedPreferences file. No new setting changes are authored here yet.
 */
private val Context.nightwaveProfileShadow by preferencesDataStore(
    name = "nightwave_v2_guest_datastore"
)

data class ProfileMirrorReport(val changed: Boolean, val displayName: String, val theme: NightWaveTheme)

class ProfileDataStoreMirror(context: Context) {
    private val app = context.applicationContext
    private val guest = GuestProfileStore(app)

    private object Key {
        val digest = stringPreferencesKey("legacy_digest")
        val localId = stringPreferencesKey("local_id")
        val name = stringPreferencesKey("display_name")
        val avatar = stringPreferencesKey("avatar_style")
        val theme = stringPreferencesKey("theme")
        val motion = stringPreferencesKey("motion")
        val quality = stringPreferencesKey("quality")
        val privacy = stringPreferencesKey("history_privacy")
        val crossfade = intPreferencesKey("crossfade_seconds")
        val artwork = booleanPreferencesKey("lockscreen_artwork")
        val awake = booleanPreferencesKey("stay_awake")
    }

    val observe: Flow<ProfileSettings?> = app.nightwaveProfileShadow.data.map { preferences ->
        val id = preferences[Key.localId] ?: return@map null
        fun <T : Enum<T>> parse(input: String?, options: Array<T>, fallback: T): T =
            options.firstOrNull { it.name == input } ?: fallback
        ProfileSettings(
            localId = id,
            displayName = preferences[Key.name] ?: "NightWave Listener",
            avatarStyle = preferences[Key.avatar] ?: "orbit",
            theme = parse(preferences[Key.theme], NightWaveTheme.entries.toTypedArray(), NightWaveTheme.MIDNIGHT),
            motion = parse(preferences[Key.motion], MotionPreference.entries.toTypedArray(), MotionPreference.SYSTEM),
            quality = parse(preferences[Key.quality], AudioQualityPreference.entries.toTypedArray(), AudioQualityPreference.AUTO),
            historyPrivacy = parse(preferences[Key.privacy], ListeningPrivacy.entries.toTypedArray(), ListeningPrivacy.DEVICE_ONLY),
            crossfadeSeconds = preferences[Key.crossfade] ?: 0,
            showArtworkOnLockscreen = preferences[Key.artwork] ?: true,
            keepScreenAwakeWhileBrowsing = preferences[Key.awake] ?: false
        ).normalized()
    }

    suspend fun reconcile(): ProfileMirrorReport = withContext(Dispatchers.IO) {
        val source = guest.load().normalized()
        val canonical = listOf(
            source.localId, source.displayName, source.avatarStyle, source.theme.name,
            source.motion.name, source.quality.name, source.historyPrivacy.name,
            source.crossfadeSeconds.toString(),
            source.showArtworkOnLockscreen.toString(),
            source.keepScreenAwakeWhileBrowsing.toString()
        ).joinToString("\u0000")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        var changed = false
        app.nightwaveProfileShadow.edit { state ->
            if (state[Key.digest] != digest) {
                state[Key.localId] = source.localId
                state[Key.name] = source.displayName
                state[Key.avatar] = source.avatarStyle
                state[Key.theme] = source.theme.name
                state[Key.motion] = source.motion.name
                state[Key.quality] = source.quality.name
                state[Key.privacy] = source.historyPrivacy.name
                state[Key.crossfade] = source.crossfadeSeconds
                state[Key.artwork] = source.showArtworkOnLockscreen
                state[Key.awake] = source.keepScreenAwakeWhileBrowsing
                state[Key.digest] = digest // same atomic DataStore edit
                changed = true
            }
        }
        ProfileMirrorReport(changed, source.displayName, source.theme)
    }
}
