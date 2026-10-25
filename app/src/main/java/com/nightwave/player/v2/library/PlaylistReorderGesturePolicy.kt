package com.nightwave.player.v2.library

import kotlin.math.roundToInt

/**
 * Bounded gesture translation only. The database operation still validates the
 * exact playlist snapshot and rejects stale or invalid reorder attempts.
 */
object PlaylistReorderGesturePolicy {
    fun offsetToSteps(offsetPx: Float, rowHeightPx: Int): Int {
        if (!offsetPx.isFinite() || rowHeightPx <= 0) return 0
        return (offsetPx / rowHeightPx).coerceIn(-299f, 299f).roundToInt()
    }

    fun target(fromIndex: Int, steps: Int, count: Int): Int? {
        if (count !in 2..300 || fromIndex !in 0 until count) return null
        val target = (fromIndex + steps).coerceIn(0, count - 1)
        return target.takeIf { it != fromIndex }
    }
}
