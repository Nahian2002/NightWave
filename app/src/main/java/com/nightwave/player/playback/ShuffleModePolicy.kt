package com.nightwave.player.playback

/** Explicit shuffle requests are idempotent; legacy toggles still invert. */
object ShuffleModePolicy {
    fun resolve(current: Boolean, explicit: Boolean, enabled: Boolean): Boolean =
        if (explicit) enabled else !current
}
