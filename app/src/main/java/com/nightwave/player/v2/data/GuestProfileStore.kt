package com.nightwave.player.v2.data

import android.content.Context
import com.nightwave.player.v2.model.AudioQualityPreference
import com.nightwave.player.v2.model.ListeningPrivacy
import com.nightwave.player.v2.model.MotionPreference
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.model.ProfileSettings
import java.util.UUID

/**
 * Local guest preferences for the first NightWave 2.0 milestone.
 *
 * An independent namespace means older NightWave favorites and playlists remain
 * untouched. Google authentication is deliberately NOT simulated here.
 * Later migration into DataStore can read these keys and migrate atomically.
 */
class GuestProfileStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nightwave_v2_guest_profile_1", Context.MODE_PRIVATE)

    companion object { private val guard = Any() }

    private fun <T : Enum<T>> enumValue(raw: String?, fallback: T, values: Array<T>): T =
        values.firstOrNull { it.name == raw } ?: fallback

    /** Wrong-typed historical preferences must not crash app startup. */
    fun load(): ProfileSettings = synchronized(guard) { loadLocked() }

    private fun loadLocked(): ProfileSettings {
        val values = prefs.all
        fun string(key: String, fallback: String) = values[key] as? String ?: fallback
        fun boolean(key: String, fallback: Boolean) = values[key] as? Boolean ?: fallback
        fun int(key: String, fallback: Int) = values[key] as? Int ?: fallback

        val storedId = values["local_id"] as? String
        val id = if (!storedId.isNullOrBlank()) storedId
            else if (!values.containsKey("local_id")) {
                val created = UUID.randomUUID().toString()
                if (prefs.edit().putString("local_id", created).commit()) created else "unsaved-guest"
            } else "corrupt-guest"

        return ProfileSettings(
            localId = id,
            displayName = string("display_name", "NightWave Listener"),
            avatarStyle = string("avatar_style", "orbit"),
            theme = enumValue(string("theme", ""), NightWaveTheme.MIDNIGHT,
                NightWaveTheme.entries.toTypedArray()),
            motion = enumValue(string("motion", ""), MotionPreference.SYSTEM,
                MotionPreference.entries.toTypedArray()),
            quality = enumValue(string("quality", ""), AudioQualityPreference.AUTO,
                AudioQualityPreference.entries.toTypedArray()),
            historyPrivacy = enumValue(string("history_privacy", ""), ListeningPrivacy.DEVICE_ONLY,
                ListeningPrivacy.entries.toTypedArray()),
            crossfadeSeconds = int("crossfade_seconds", 0),
            showArtworkOnLockscreen = boolean("lockscreen_artwork", true),
            keepScreenAwakeWhileBrowsing = boolean("stay_awake", false)
        ).normalized()
    }

    fun save(settings: ProfileSettings): Boolean = synchronized(guard) { saveLocked(settings) }

    /** Optional recovery cannot overwrite a profile edited after its preview. */
    fun saveIfUnchanged(expected: ProfileSettings, updated: ProfileSettings): Boolean = synchronized(guard) {
        if (loadLocked() != expected) return@synchronized false
        saveLocked(updated)
    }

    private fun saveLocked(settings: ProfileSettings): Boolean {
        val clean = settings.normalized()
        // Never overwrite an unexpected identity or corrupted source key.
        val existingId = prefs.all["local_id"] as? String ?: return false
        if (existingId.isBlank() || existingId != clean.localId) return false
        return prefs.edit()
            .putString("display_name", clean.displayName)
            .putString("avatar_style", clean.avatarStyle)
            .putString("theme", clean.theme.name)
            .putString("motion", clean.motion.name)
            .putString("quality", clean.quality.name)
            .putString("history_privacy", clean.historyPrivacy.name)
            .putInt("crossfade_seconds", clean.crossfadeSeconds)
            .putBoolean("lockscreen_artwork", clean.showArtworkOnLockscreen)
            .putBoolean("stay_awake", clean.keepScreenAwakeWhileBrowsing)
            .commit()
    }
}
