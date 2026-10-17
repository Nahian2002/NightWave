package com.nightwave.player.v2.playback

/**
 * Human-readable phase derived only from Media3/controller state.
 * Does not perform a stream lookup, collect telemetry or read any files.
 */
enum class PlaybackPhase { IDLE, DISCONNECTED, ERROR, LOOKING_UP, BUFFERING, PLAYING, PAUSED, READY }

data class PlaybackHealth(
    val phase: PlaybackPhase,
    val title: String,
    val explanation: String,
    val needsUserAction: Boolean
)

object PlaybackHealthPolicy {
    fun classify(
        hasTrack: Boolean,
        connected: Boolean,
        offline: Boolean,
        playing: Boolean,
        buffering: Boolean,
        resolving: Boolean,
        error: String?
    ): PlaybackHealth {
        if (!connected) return PlaybackHealth(
            PlaybackPhase.DISCONNECTED, "Player disconnected",
            "Reconnect the music controls. Your saved tracks and queue remain available.", true)
        if (!error.isNullOrBlank()) return PlaybackHealth(
            PlaybackPhase.ERROR, "Playback needs attention",
            "The current track couldn't continue. Retry or select another song.", true)
        if (!hasTrack) return PlaybackHealth(
            PlaybackPhase.IDLE, "Nothing playing", "Choose a song to begin.", false)
        if (resolving && !offline) return PlaybackHealth(
            PlaybackPhase.LOOKING_UP, "Finding audio",
            "Checking an available public stream. You can still change songs or pause.", false)
        if (buffering) return PlaybackHealth(
            PlaybackPhase.BUFFERING, "Buffering audio",
            if (offline) "Preparing the selected local audio file."
            else "Waiting for the selected stream to become playable.", false)
        if (playing) return PlaybackHealth(
            PlaybackPhase.PLAYING, "Playing", "Audio is currently playing.", false)
        return PlaybackHealth(
            PlaybackPhase.PAUSED, "Paused or ready",
            "Use Play to continue listening when you're ready.", false)
    }
}
