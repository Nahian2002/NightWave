package com.nightwave.player.playback

/**
 * NW80: pressing Previous while already at the start of the first song
 * should seek that SAME loaded song to zero. Re-running a network extractor
 * for the identical cached item adds unnecessary lag and can unexpectedly
 * fail during an otherwise healthy listening session.
 */
internal object PlaybackPreviousPolicy {
    fun canRestartLoaded(
        selectedUrl: String?, previousUrl: String?,
        loadedMediaId: String?, playerHasError: Boolean
    ): Boolean = !playerHasError && !selectedUrl.isNullOrBlank() &&
        selectedUrl == previousUrl && selectedUrl == loadedMediaId
}
