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
        val presetNames: List<String>,
        val levelsMb: List<Int> = emptyList()
    ) {
        val commandToken: String? get() = if (sessionReady)
            EqualizerCommandPolicy.token(audioSessionId, centersHz, minimumMb, maximumMb, presetNames)
            else null
    }

    fun read(): Snapshot {
        val count = preferences.getInt("count", 0).coerceIn(0, 32)
        val presets = preferences.getInt("presets", 0).coerceIn(0, 32)
        val snapshot = Snapshot(
            preferences.getBoolean("ready", false),
            preferences.getInt("session", -1),
            (0 until count).map { preferences.getInt("frequency_$it", 0) },
            preferences.getInt("min", 0),
            preferences.getInt("max", 0),
            (0 until presets).map { preferences.getString("preset_$it", "Preset ${it + 1}") ?: "Preset ${it + 1}" },
            (0 until count).map { preferences.getInt("level_$it", 0) }
        )
        return if (snapshot.commandToken == null) snapshot.copy(sessionReady = false) else snapshot
    }

    fun clear() {
        preferences.edit().clear().commit()
    }

    fun publish(effect: Equalizer, sessionId: Int): Boolean = try {
        val bands = effect.numberOfBands.toInt()
        val presets = effect.numberOfPresets.toInt().coerceIn(0, 20)
        val range = effect.bandLevelRange
        if (range.size < 2 || !EqualizerSessionPolicy.canPublish(
                sessionId, runCatching { effect.hasControl() }.getOrDefault(false),
                bands, range[0].toInt(), range[1].toInt())) {
            clear() // Never leave an old "ready" receipt after control loss.
            false
        } else {
            // Capture all readback before replacing readiness; a throwing band
            // query must not leave an old or partially populated snapshot.
            val centers = (0 until bands).map { effect.getCenterFreq(it.toShort()) / 1000 }
            val levels = (0 until bands).map { effect.getBandLevel(it.toShort()).toInt() }
            val names = (0 until presets).map { effect.getPresetName(it.toShort()).take(60) }
            val valid = EqualizerCommandPolicy.token(sessionId, centers,
                range[0].toInt(), range[1].toInt(), names) != null &&
                levels.all { it in range[0]..range[1] } && effect.hasControl()
            if (!valid) {
                clear()
                false
            } else {
                val editor = preferences.edit().clear()
                    .putBoolean("ready", true).putInt("session", sessionId).putInt("count", bands)
                    .putInt("presets", presets)
                    .putInt("min", range[0].toInt()).putInt("max", range[1].toInt())
                for (i in 0 until bands) {
                    editor.putInt("frequency_$i", centers[i])
                    editor.putInt("level_$i", levels[i])
                }
                for (i in 0 until presets) editor.putString("preset_$i", names[i])
                val saved = editor.commit()
                if (!saved) clear()
                saved
            }
        }
    } catch (_: Exception) {
        clear()
        false
    }
}
