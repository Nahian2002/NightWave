package com.nightwave.player.v2.playback

/**
 * Before calling MediaController.play(), match the currently loaded media item
 * to the exact selected song shown in NightWave's UI. It is not sufficient
 * that *some* unrelated MediaItem happens to remain loaded after a queue
 * replacement or switching between public and offline audio.
 *
 * A mismatch must route through PlaybackService's selected-queue resume,
 * which revalidates identity and the saved bookmark.
 */
object PlaybackCommandPolicy {
    fun canResumeLoaded(
        selectedId: String?,
        loadedId: String?,
        playerHasError: Boolean
    ): Boolean = !playerHasError &&
        !selectedId.isNullOrBlank() &&
        selectedId == loadedId

    /** Home Resume is idempotent only for the exact item already playing. */
    fun resumeSatisfied(
        selectedId: String?,
        loadedId: String?,
        playerHasError: Boolean,
        isPlaying: Boolean
    ): Boolean = isPlaying && canResumeLoaded(selectedId, loadedId, playerHasError)

    fun isOfflineSelection(selectedId: String?): Boolean =
        selectedId?.startsWith("offline:") == true

    fun isPublicSelection(selectedId: String?): Boolean =
        selectedId?.startsWith("https://www.youtube.com/watch?v=") == true
}
