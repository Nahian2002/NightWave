package com.nightwave.player.v2.ui

/** Keep an old track's duration/seek state from generating invalid Media3 commands. */
internal object PlayerProgressRules {
    fun fraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return (positionMs.coerceIn(0L, durationMs).toDouble() /
            durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    fun requestedPosition(durationMs: Long, fraction: Float): Long? {
        if (durationMs <= 0L || !fraction.isFinite()) return null
        return (durationMs.toDouble() * fraction.coerceIn(0f, 1f).toDouble())
            .toLong().coerceIn(0L, durationMs)
    }
}
