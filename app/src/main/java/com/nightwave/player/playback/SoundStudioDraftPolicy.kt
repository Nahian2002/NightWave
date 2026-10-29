package com.nightwave.player.playback

/** UI-only decisions. Never writes preferences or sends audio commands. */
internal object SoundStudioDraftPolicy {
    fun adoptManualReadback(
        requestedToken: String?, currentToken: String?, selected: Int, active: Boolean,
        requested: List<Int>, actual: List<Int>, committedRequested: Boolean
    ): Boolean = EqualizerCommandPolicy.accepts(requestedToken, currentToken) &&
        committedRequested && active && selected == 21 && EqualizerReadbackPolicy.inspect(requested, actual) in setOf(
            EqualizerReadbackPolicy.Result.VERIFIED,
            EqualizerReadbackPolicy.Result.DEVICE_QUANTIZED)

    fun activeLabel(fresh: Boolean, ready: Boolean, active: Boolean,
        selected: Int, presets: List<String>): String = when {
        !fresh -> "Checking current equalizer"
        !ready -> if (selected == 0) "Equalizer off · no active session" else "Equalizer unavailable"
        !active -> if (selected == 0) "Equalizer off" else "Saved choice · not active"
        selected == 21 -> "Active · Manual EQ"
        selected in 1..presets.size -> "Active · ${presets[selected - 1]}"
        else -> "Current equalizer needs refresh"
    }

    fun adjust(level: Int, delta: Int, minimum: Int, maximum: Int): Int? {
        val range = SoundStudioPolicy.safeManualRange(minimum, maximum) ?: return null
        if (level !in range || delta !in setOf(-100, 100)) return null
        return (level + delta).coerceIn(range)
    }
}
