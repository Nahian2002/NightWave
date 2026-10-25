package com.nightwave.player.v2.ui

/**
 * NW51: a result from a cancelled/replaced UI-only read can never overwrite
 * newer state after a song change, rotation, Stop or Activity destruction.
 * This policy does not own any persisted music or background playback.
 */
internal object LibraryUiRefreshPolicy {
    fun accepts(
        submittedGeneration: Int,
        currentGeneration: Int,
        isFinishing: Boolean,
        isDestroyed: Boolean
    ): Boolean = submittedGeneration >= 0 &&
        submittedGeneration == currentGeneration && !isFinishing && !isDestroyed
}
