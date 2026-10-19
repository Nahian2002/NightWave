package com.nightwave.player.v2.ui

/** Converts artwork-screen touch coordinates into a safe playback fraction. */
internal object NightWaveSeekGesturePolicy {
    fun touchFraction(x: Float, width: Float, edge: Float): Float {
        if (!x.isFinite() || !width.isFinite() || !edge.isFinite() ||
            width <= 0f || edge < 0f) return 0f
        val usable = (width - 2f * edge).coerceAtLeast(1f)
        return ((x - edge) / usable).coerceIn(0f, 1f)
    }

    fun thumbX(fraction: Float, width: Float, edge: Float): Float {
        if (!width.isFinite() || !edge.isFinite() || width <= 0f || edge < 0f)
            return 0f
        val actualEdge = edge.coerceAtMost(width / 2f)
        val usable = (width - 2f * actualEdge).coerceAtLeast(0f)
        val safe = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
        return actualEdge + usable * safe
    }
}
