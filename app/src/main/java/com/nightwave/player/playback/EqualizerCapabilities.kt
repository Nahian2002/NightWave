package com.nightwave.player.playback

import android.content.Context
import android.media.audiofx.Equalizer

/** Device/session-specific values. Never assume five bands or a fixed dB range. */
class EqualizerCapabilities(context: Context) {
    private val preferences = context.getSharedPreferences("nightwave_eq_capabilities", Context.MODE_PRIVATE)

    data class Snapshot(
        val sessionReady: Boolean,
        val audioSessionId: Int,
        val centersHz: List<Int>,
        val minimumMb: Int,
        val maximumMb: Int,
        val presetNames: List<String>
    )

    fun read(): Snapshot {
        val count = preferences.getInt("count", 0).coerceIn(0, 32)
        val presets = preferences.getInt("presets", 0).coerceIn(0, 32)
        return Snapshot(
            preferences.getBoolean("ready", false),
            preferences.getInt("session", -1),
            (0 until count).map { preferences.getInt("frequency_$it", 0) },
            preferences.getInt("min", 0),
            preferences.getInt("max", 0),
            (0 until presets).map { preferences.getString("preset_$it", "Preset ${it + 1}") ?: "Preset ${it + 1}" }
        )
    }

    fun clear() {
        preferences.edit().clear().commit()
    }

    fun publish(effect: Equalizer, sessionId: Int) {
        val bands = effect.numberOfBands.toInt().coerceIn(0, 32)
        val presets = effect.numberOfPresets.toInt().coerceIn(0, 20)
        val range = effect.bandLevelRange
        if (range.size < 2 || range[0] > range[1]) return
        val editor = preferences.edit().clear()
            .putBoolean("ready", true).putInt("session", sessionId).putInt("count", bands)
            .putInt("presets", presets)
            .putInt("min", range[0].toInt()).putInt("max", range[1].toInt())
        for (i in 0 until bands) {
            editor.putInt("frequency_$i", effect.getCenterFreq(i.toShort()) / 1000)
        }
        for (i in 0 until presets) {
            editor.putString("preset_$i", effect.getPresetName(i.toShort()).take(60))
        }
        editor.commit()
    }
}
