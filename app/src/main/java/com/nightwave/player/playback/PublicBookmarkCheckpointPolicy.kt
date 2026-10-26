package com.nightwave.player.playback

/**
 * Current-song identity fence for public seek/bookmark checkpoints.
 *
 * A paused/outgoing Media3 item can briefly differ from the service's new
 * queue selection. In that case never persist its old playhead as the new
 * selected song's bookmark. Offline file positions use their own store.
 */
internal object PublicBookmarkCheckpointPolicy {
    fun shouldPersist(
        loadedMediaId: String?,
        selectedPublicUrl: String?,
        offlinePlaying: Boolean
    ): Boolean =
        !offlinePlaying &&
            loadedMediaId != null &&
            loadedMediaId == selectedPublicUrl &&
            QueueRestorePolicy.isCanonicalPublicWatchUrl(loadedMediaId)
}
