package com.nightwave.player.v2.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nightwave.player.v2.model.ProfileSettings
import kotlinx.coroutines.flow.first

/** Shadow preferences only; legacy guest store is still the active source. */
private val Context.guestSettingsShadow by preferencesDataStore(name = "nightwave_v2_settings_shadow")

class GuestSettingsMirror(context: Context) {
    private val store = context.applicationContext.guestSettingsShadow

    private val displayName = stringPreferencesKey("display_name")
    private val avatar = stringPreferencesKey("avatar_style")
    private val theme = stringPreferencesKey("theme")
    private val motion = stringPreferencesKey("motion")
    private val audioQuality = stringPreferencesKey("audio_quality")
    private val historyPrivacy = stringPreferencesKey("history_privacy")
    private val crossfade = intPreferencesKey("crossfade_seconds")
    private val lockscreenArtwork = booleanPreferencesKey("lockscreen_artwork")
    private val stayAwake = booleanPreferencesKey("stay_awake")

    suspend fun stage(profile: ProfileSettings) {
        val safe = profile.normalized()
        store.edit { settings ->
            settings[displayName] = safe.displayName
            settings[avatar] = safe.avatarStyle
            settings[theme] = safe.theme.name
            settings[motion] = safe.motion.name
            settings[audioQuality] = safe.quality.name
            settings[historyPrivacy] = safe.historyPrivacy.name
            settings[crossfade] = safe.crossfadeSeconds
            settings[lockscreenArtwork] = safe.showArtworkOnLockscreen
            settings[stayAwake] = safe.keepScreenAwakeWhileBrowsing
        }
    }

    /** Used by migration QA. No stable device ID is copied into the mirror. */
    suspend fun matches(profile: ProfileSettings): Boolean {
        val current = store.data.first()
        val safe = profile.normalized()
        return current[displayName] == safe.displayName &&
            current[avatar] == safe.avatarStyle &&
            current[theme] == safe.theme.name &&
            current[motion] == safe.motion.name &&
            current[audioQuality] == safe.quality.name &&
            current[historyPrivacy] == safe.historyPrivacy.name &&
            current[crossfade] == safe.crossfadeSeconds &&
            current[lockscreenArtwork] == safe.showArtworkOnLockscreen &&
            current[stayAwake] == safe.keepScreenAwakeWhileBrowsing
    }
}
