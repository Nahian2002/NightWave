package com.nightwave.player.playback

/**
 * Pure end-of-track handoff fence. It does not own or modify ExoPlayer, queue
 * state, signed network media, local files or listening preferences.
 */
internal object PlaybackTransitionPolicy {
    data class Ended(val generation: Int, val mediaId: String?, val offline: Boolean)

    fun shouldAdvance(
        ended: Ended,
        currentGeneration: Int,
        loadedMediaId: String?,
        selectedMediaId: String?,
        currentOffline: Boolean,
        isEnded: Boolean,
        timerPaused: Boolean,
        alreadyHandled: Boolean
    ): Boolean =
        !timerPaused &&
        isEnded &&
        !alreadyHandled &&
        ended.generation == currentGeneration &&
        ended.offline == currentOffline &&
        !ended.mediaId.isNullOrBlank() &&
        ended.mediaId == loadedMediaId &&
        ended.mediaId == selectedMediaId
}
