package com.nightwave.player.v2.ui

import kotlin.math.roundToInt

/** Decode geometry only; never changes the media URL, thumbnail identity or storage. */
internal object NightWaveArtworkRequestPolicy {
    const val MAX_EDGE = 1280
    data class Pixels(val width: Int, val height: Int) {
        val valid: Boolean get() = width > 0 && height > 0
    }
    fun pixels(width: Int, height: Int): Pixels {
        if (width <= 0 || height <= 0) return Pixels(0, 0)
        val factor = minOf(1.0, MAX_EDGE.toDouble() / maxOf(width, height))
        return Pixels((width * factor).roundToInt().coerceIn(1, MAX_EDGE),
            (height * factor).roundToInt().coerceIn(1, MAX_EDGE))
    }
}

/** Reference identity distinguishes even A→B→A with the same thumbnail address. */
internal class ArtworkRequestSession {
    private var active = true
    fun close() { active = false }
    fun accepts(expected: ArtworkRequestAttempt, current: ArtworkRequestAttempt): Boolean =
        active && expected.session === this && current.session === this && expected == current
}
internal data class ArtworkRequestAttempt(val session: ArtworkRequestSession, val candidate: Int,
    val size: NightWaveArtworkRequestPolicy.Pixels)

/** Numeric/enum-only debug evidence: no media URL, title, account or persistent file. */
internal data class ArtworkDecodeReport(val large: Boolean, val candidate: Int,
    val viewportWidth: Int, val viewportHeight: Int, val requestWidth: Int, val requestHeight: Int,
    val decodedWidth: Int, val decodedHeight: Int, val dataSource: String, val sampled: Boolean)
