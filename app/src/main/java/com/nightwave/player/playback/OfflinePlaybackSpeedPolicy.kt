package com.nightwave.player.playback

/** Explicit bounded speed presets for imported local audio, not public streams. */
object OfflinePlaybackSpeedPolicy {
    const val NORMAL_PERCENT = 100
    val presetsPercent = listOf(75, 100, 125, 150, 200)

    fun accepts(percent: Int): Boolean = percent in presetsPercent

    fun speed(percent: Int): Float =
        (if (accepts(percent)) percent else NORMAL_PERCENT) / 100f

    fun label(percent: Int): String = when (percent) {
        75 -> "0.75×"
        100 -> "1×"
        125 -> "1.25×"
        150 -> "1.5×"
        200 -> "2×"
        else -> "1×"
    }
}
