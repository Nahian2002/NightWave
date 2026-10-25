package com.nightwave.player.v2.ui

/**
 * Central motion contract: restrained, consistent and genuinely optional.
 * Values are free of Compose dependencies so phone/CI smoke tests can run.
 */
object NightWaveMotionPolicy {
    const val NAV_MS = 220
    const val SURFACE_MS = 240
    const val CONTENT_MS = 280
    const val FEEDBACK_MS = 160
    const val MAX_MS = 450

    fun duration(reduceMotion: Boolean, requested: Int): Int =
        if (reduceMotion) 0 else requested.coerceIn(0, MAX_MS)

    fun tabScale(active: Boolean, reduceMotion: Boolean): Float =
        if (reduceMotion) 1f else if (active) 1f else 0.93f

    fun artworkScale(playing: Boolean, reduceMotion: Boolean): Float =
        if (reduceMotion) 1f else if (playing) 1f else .985f

    fun transitionEnabled(reduceMotion: Boolean) = !reduceMotion
}
