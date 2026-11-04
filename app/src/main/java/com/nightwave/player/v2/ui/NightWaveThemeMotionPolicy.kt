package com.nightwave.player.v2.ui

import com.nightwave.player.v2.model.Palette

/** Presentation roles only; stored themes and motion preferences are unchanged. */
internal object NightWaveThemeMotionPolicy {
    fun inverseAction(palette: Palette): Long = palette.accentSoft

    fun reduceMotion(userReduced: Boolean, followSystem: Boolean, systemScale: Float?): Boolean =
        userReduced || (followSystem &&
            (systemScale == null || !systemScale.isFinite() || systemScale <= 0f))
}

/** One foreground subscription. Stopped/disposed callbacks cannot publish stale settings. */
internal class NightWaveMotionObservation(
    private val read: () -> Float?,
    private val subscribe: () -> Unit,
    private val unsubscribe: () -> Unit,
    private val publish: (Float?) -> Unit
) {
    private var active = false
    fun start() {
        if (active) return
        active = true
        try {
            subscribe()
            refresh()
        } catch (_: Exception) {
            active = false
            runCatching { unsubscribe() }
            publish(null)
        }
    }
    fun refresh() {
        if (active) publish(runCatching { read() }.getOrNull())
    }
    fun stop() {
        if (!active) return
        active = false
        runCatching { unsubscribe() }
    }
}
