package com.nightwave.player.playback

import java.util.Locale

/** Pure UI-to-engine policy. Engine limits still come from the current Android audio session. */
object SoundStudioPolicy {
    fun flatLevels(count: Int, minimumMb: Int, maximumMb: Int): List<Int> =
        List(count.coerceIn(0, 32)) { 0.coerceIn(minimumMb, maximumMb) }

    fun updateLevel(
        original: List<Int>, index: Int, requestedMb: Int,
        minimumMb: Int, maximumMb: Int
    ): List<Int> {
        if (index !in original.indices || minimumMb > maximumMb) return original
        return original.toMutableList().apply {
            this[index] = requestedMb.coerceIn(minimumMb, maximumMb)
        }
    }

    fun validLevels(
        values: List<Int>, count: Int, minimumMb: Int, maximumMb: Int
    ): Boolean = count in 1..32 && values.size == count &&
        minimumMb <= maximumMb && values.all { it in minimumMb..maximumMb }

    fun validPreset(selection: Int, reportedPresets: Int): Boolean =
        selection == 0 || selection in 1..reportedPresets.coerceIn(0, 20)

    fun frequencyLabel(hz: Int): String =
        if (hz >= 1000 && hz % 1000 == 0) "${hz / 1000} kHz"
        else if (hz >= 1000) String.format(Locale.US, "%.1f kHz", hz / 1000.0)
        else "${hz.coerceAtLeast(0)} Hz"

    fun gainLabel(millibels: Int): String =
        String.format(Locale.US, "%+.1f dB", millibels / 100.0)
}
