package com.nightwave.player.playback

/**
 * NW49: conservative, device-band-aware manual EQ drafts.
 *
 * These are real gain requests in millibels, not a simulated EQ graph.
 * The screen must require explicit "Apply" before sending them to Media3's
 * active Android audio session. No sound is changed by calling this policy.
 * Frequencies are *reported by the device*, not hardcoded five-band indices.
 */
object SoundStudioTuningPolicy {
    enum class Shape { FLAT, WARM, VOCALS, AIRY }

    fun label(shape: Shape): String = when (shape) {
        Shape.FLAT -> "Flat"
        Shape.WARM -> "Warm bass"
        Shape.VOCALS -> "Clear vocals"
        Shape.AIRY -> "Airy detail"
    }

    fun hint(shape: Shape): String = when (shape) {
        Shape.FLAT -> "Reset to device-supported zero gain."
        Shape.WARM -> "Gentle low-end lift, without maximum boost."
        Shape.VOCALS -> "Subtle vocal-presence lift and bass restraint."
        Shape.AIRY -> "Light high-frequency lift for quieter details."
    }

    /** The generated curve never exceeds +2.5 dB or drops below -1 dB. */
    fun targetMb(shape: Shape, hz: Int): Int {
        if (hz <= 0) return 0
        return when (shape) {
            Shape.FLAT -> 0
            Shape.WARM -> when {
                hz < 120 -> 250
                hz < 280 -> 180
                hz < 700 -> 50
                hz < 2_500 -> -50
                else -> 0
            }
            Shape.VOCALS -> when {
                hz < 180 -> -100
                hz < 500 -> 0
                hz < 1_200 -> 100
                hz < 3_500 -> 200
                hz < 6_500 -> 120
                else -> 0
            }
            Shape.AIRY -> when {
                hz < 180 -> -50
                hz < 1_100 -> 0
                hz < 4_500 -> 50
                hz < 10_000 -> 175
                else -> 200
            }
        }
    }

    /** Reject inconsistent or stale capabilities before changing any draft. */
    fun draft(
        shape: Shape,
        centersHz: List<Int>,
        deviceMinimumMb: Int,
        deviceMaximumMb: Int
    ): List<Int>? {
        val range = SoundStudioPolicy.safeManualRange(deviceMinimumMb, deviceMaximumMb)
            ?: return null
        if (centersHz.size !in 1..32 || centersHz.any { it <= 0 }) return null
        return centersHz.map { hz -> targetMb(shape, hz).coerceIn(range.first, range.last) }
    }

    fun hasChanged(draft: List<Int>, actual: List<Int>): Boolean =
        draft.isNotEmpty() && (draft.size != actual.size || draft != actual)

    /** Portable indication only; an output device may still bypass Android FX. */
    fun statusLabel(active: Boolean, selected: Int, edited: Boolean): String =
        when {
            edited -> "Not applied"
            active && selected == 21 -> "Manual EQ active"
            active -> "Device preset active"
            else -> "Effect off"
        }
}
