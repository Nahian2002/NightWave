package com.nightwave.player.v2.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Transient touch ownership only; disposing any shelf releases its own token. */
internal class NightWaveShelfGestureState {
    private val owners = mutableStateMapOf<Any, Unit>()
    val active: Boolean get() = owners.isNotEmpty()
    fun begin(owner: Any) { owners[owner] = Unit }
    fun end(owner: Any) { owners.remove(owner) }
}
internal val LocalNightWaveShelfGestures = staticCompositionLocalOf<NightWaveShelfGestureState?> { null }
