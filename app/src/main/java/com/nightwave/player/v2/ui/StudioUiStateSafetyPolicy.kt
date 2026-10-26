package com.nightwave.player.v2.ui

/**
 * UI-only guards for state restored after Android recreated an Activity, or
 * callbacks retained by native dialogs while their displayed data changed.
 * Never mutate playlists, audio files, cloud backups or downloaded songs.
 */
internal object StudioUiStateSafetyPolicy {
    fun resolveCollection(
        selected: String?, playlistNames: Collection<String>,
        smartCollectionIds: Collection<String>, likedId: String,
        historyId: String
    ): String? = selected?.takeIf { it == likedId || it == historyId ||
        it in playlistNames || it in smartCollectionIds }

    /** Native AlertDialog choice indexes are untrusted after list refreshes. */
    fun <T> itemAt(options: List<T>, index: Int): T? = options.getOrNull(index)

    /**
     * Shared-playlist import can succeed while the snapshot's final entry is
     * not the created playlist. Prefer the newly observed name; never call
     * last() on an empty result or guess an unrelated existing playlist.
     */
    fun createdPlaylistName(before: Collection<String>,
                            after: List<String>): String? {
        if (after.isEmpty()) return null
        val existing = before.toHashSet()
        return after.lastOrNull { it !in existing }
    }
}
