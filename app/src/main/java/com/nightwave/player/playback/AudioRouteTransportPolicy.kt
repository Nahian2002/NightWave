package com.nightwave.player.playback

/**
 * Media3 owns Android audio focus and unplug/noisy handling. This policy only
 * interprets a headset's explicit transport key while a saved public queue
 * has no loaded MediaItem (e.g. during public stream resolution).
 *
 * DELEGATE leaves normal loaded-media and offline transport control with
 * Media3, preserving native focus and route-change behavior.
 */
internal object AudioRouteTransportPolicy {
    enum class Key { PLAY, PAUSE, TOGGLE }
    enum class Action { DELEGATE, RESUME_SAVED, PAUSE_PENDING }

    fun decide(
        key: Key,
        selectedCanonicalPublicSong: Boolean,
        mediaItemLoaded: Boolean,
        offlinePlaying: Boolean,
        playWhenReady: Boolean
    ): Action {
        if (offlinePlaying || mediaItemLoaded || !selectedCanonicalPublicSong) {
            return Action.DELEGATE
        }
        return when (key) {
            Key.PLAY -> Action.RESUME_SAVED
            Key.PAUSE -> if (playWhenReady) Action.PAUSE_PENDING else Action.DELEGATE
            Key.TOGGLE -> if (playWhenReady) Action.PAUSE_PENDING else Action.RESUME_SAVED
        }
    }
}
