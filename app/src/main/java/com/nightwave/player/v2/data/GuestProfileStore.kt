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

    private fun <T : Enum<T>> enumValue(raw: String?, fallback: T, values: Array<T>): T =
        values.firstOrNull { it.name == raw } ?: fallback

    fun load(): ProfileSettings {
        val id = prefs.getString("local_id", null).orEmpty().ifBlank {
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString("local_id", generated).commit()
            generated
        }
        return ProfileSettings(
            localId = id,
            displayName = prefs.getString("display_name", "NightWave Listener") ?: "NightWave Listener",
            avatarStyle = prefs.getString("avatar_style", "orbit") ?: "orbit",
            theme = enumValue(prefs.getString("theme", null), NightWaveTheme.MIDNIGHT, NightWaveTheme.entries.toTypedArray()),
            motion = enumValue(prefs.getString("motion", null), MotionPreference.SYSTEM, MotionPreference.entries.toTypedArray()),
            quality = enumValue(prefs.getString("quality", null), AudioQualityPreference.AUTO, AudioQualityPreference.entries.toTypedArray()),
            historyPrivacy = enumValue(prefs.getString("history_privacy", null), ListeningPrivacy.DEVICE_ONLY, ListeningPrivacy.entries.toTypedArray()),
            crossfadeSeconds = prefs.getInt("crossfade_seconds", 0),
            showArtworkOnLockscreen = prefs.getBoolean("lockscreen_artwork", true),
            keepScreenAwakeWhileBrowsing = prefs.getBoolean("stay_awake", false)
        ).normalized()
    }

    fun save(settings: ProfileSettings): Boolean {
        val clean = settings.normalized()
        if (clean.localId != load().localId) return false // Never overwrite an unrelated profile.
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
