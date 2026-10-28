package com.nightwave.player.v2.ui

import kotlin.math.abs

internal object NightWavePagerRecoveryPolicy {
    const val CENTER_EPSILON = .001f
    fun repairPage(page: Int, offset: Float, scrolling: Boolean, dragging: Boolean, count: Int): Int? =
        if (count <= 0 || scrolling || dragging || !offset.isFinite() || abs(offset) <= CENTER_EPSILON) null
        else page.coerceIn(0, count - 1)

    fun needsNavigation(page: Int, offset: Float, scrolling: Boolean, destination: Int): Boolean =
        page != destination || scrolling || !offset.isFinite() || abs(offset) > CENTER_EPSILON
}
