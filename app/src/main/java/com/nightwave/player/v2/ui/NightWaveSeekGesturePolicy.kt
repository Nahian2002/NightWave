package com.nightwave.player.v2.ui

/** Converts artwork-screen touch coordinates into a safe playback fraction. */
internal object NightWaveSeekGesturePolicy {
    /**
     * A collapsed or non-finite timeline has no meaningful seek coordinate.
     * Never translate a tap on a 0px/narrow player into "seek to beginning".
     * The mapping uses the exact start and end of the drawn track.
     */
    fun touchFractionOrNull(x: Float, width: Float, edge: Float): Float? {
        if (!x.isFinite() || !width.isFinite() || !edge.isFinite() ||
            width <= 0f || edge < 0f) return null
        val usable = width - 2f * edge
        if (!usable.isFinite() || usable <= 0f) return null
        val mapped = (x - edge) / usable
        return if (mapped.isFinite()) mapped.coerceIn(0f, 1f) else null
    }

    /** Legacy presentation fallback; UI actions must use the nullable mapping. */
    fun touchFraction(x: Float, width: Float, edge: Float): Float =
        touchFractionOrNull(x, width, edge) ?: 0f

    /** Accessibility progress must never acknowledge NaN or infinity. */
    fun accessibilityFractionOrNull(requested: Float): Float? =
        if (requested.isFinite()) requested.coerceIn(0f, 1f) else null

    fun thumbX(fraction: Float, width: Float, edge: Float): Float {
        if (!width.isFinite() || !edge.isFinite() || width <= 0f || edge < 0f)
            return 0f
        val actualEdge = edge.coerceAtMost(width / 2f)
        val usable = (width - 2f * actualEdge).coerceAtLeast(0f)
        val safe = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
        return actualEdge + usable * safe
    }
}
