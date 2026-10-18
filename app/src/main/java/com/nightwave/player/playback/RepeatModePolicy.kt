package com.nightwave.player.playback

/** Pure repeat control rules: rapid taps must not depend on asynchronous UI refresh. */
object RepeatModePolicy {
    fun next(current: PlaybackQueue.Repeat): PlaybackQueue.Repeat = when (current) {
        PlaybackQueue.Repeat.OFF -> PlaybackQueue.Repeat.ALL
        PlaybackQueue.Repeat.ALL -> PlaybackQueue.Repeat.ONE
        PlaybackQueue.Repeat.ONE -> PlaybackQueue.Repeat.OFF
    }

    /** Explicit modes are idempotent; unrecognized requests never toggle the queue. */
    fun resolve(
        current: PlaybackQueue.Repeat, explicit: Boolean, requested: String?
    ): PlaybackQueue.Repeat? =
        if (!explicit) next(current)
        else PlaybackQueue.Repeat.values().firstOrNull { it.name == requested }
}
