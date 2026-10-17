package com.nightwave.player.playback

/**
 * Bounded user-selected attenuation for NightWave output.
 * 100% is unity gain; no preset boosts or modifies Android's system volume.
 */
object ListeningLevelPolicy {
    const val DEFAULT_PERCENT = 100
    val presetsPercent = listOf(100, 85, 70)

    fun accepts(percent: Int): Boolean = percent in presetsPercent

    fun gain(percent: Int): Float =
        (if (accepts(percent)) percent else DEFAULT_PERCENT) / 100f

    fun label(percent: Int): String = when (percent) {
        100 -> "Full · 100%"
        85 -> "Comfortable · 85%"
        70 -> "Quiet · 70%"
        else -> "Full · 100%"
    }
}
