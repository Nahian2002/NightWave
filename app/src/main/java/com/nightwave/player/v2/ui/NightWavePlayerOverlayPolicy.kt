package com.nightwave.player.v2.ui

/**
 * NW58: keep the Now Playing backdrop opaque from the first requested
 * frame until the last frame of the exit animation. Normal screen content
 * must never become visible through a partially transparent player.
 */
internal object NightWavePlayerOverlayPolicy {
    fun retainBackdrop(
        requestedExpanded: Boolean,
        transitionCurrent: Boolean,
        transitionTarget: Boolean,
        transitionRunning: Boolean = false
    ): Boolean = requestedExpanded || transitionCurrent || transitionTarget || transitionRunning
}
