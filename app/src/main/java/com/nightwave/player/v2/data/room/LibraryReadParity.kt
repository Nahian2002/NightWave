package com.nightwave.player.v2.data.room

import com.nightwave.player.net.VideoPreview

/** Exact data read for one library display, with playlist order retained. */
data class LibraryReadSnapshot(
    val favorites: List<VideoPreview>,
    val recent: List<VideoPreview>,
    val playlists: List<Pair<String, List<VideoPreview>>>
) {
    fun playlistNames(): List<String> = playlists.map { it.first }
    fun playlist(name: String): List<VideoPreview> =
        playlists.firstOrNull { it.first == name }?.second ?: emptyList()
}

/** Why a secondary database is not currently safe to use as a read source. */
enum class ReadBlocker {
    NONE,
    MISSING_CHECKPOINT,
    SOURCE_CHANGED,
    FAVORITES_DIFFER,
    HISTORY_DIFFER,
    PLAYLIST_NAMES_DIFFER,
    PLAYLIST_TRACKS_DIFFER
}

data class ReadParity(val eligible: Boolean, val blocker: ReadBlocker) {
    companion object {
        val VERIFIED = ReadParity(true, ReadBlocker.NONE)
    }
}

object LibraryReadParity {
    /**
     * Strict parity rather than checking counts. Exact metadata and ordering
     * must match. If the normalized Room schema collapsed conflicting metadata
     * for the same video in several playlists, deliberately stay on legacy.
     */
    fun evaluate(
        expected: LegacyLibrarySnapshot,
        actual: LibraryReadSnapshot,
        checkpointDigest: String?
    ): ReadParity {
        if (checkpointDigest == null) return ReadParity(false, ReadBlocker.MISSING_CHECKPOINT)
        if (checkpointDigest != expected.digest) return ReadParity(false, ReadBlocker.SOURCE_CHANGED)
        if (expected.favorites != actual.favorites) return ReadParity(false, ReadBlocker.FAVORITES_DIFFER)
        if (expected.recent != actual.recent) return ReadParity(false, ReadBlocker.HISTORY_DIFFER)
        if (expected.playlists.map { it.first } != actual.playlists.map { it.first }) {
            return ReadParity(false, ReadBlocker.PLAYLIST_NAMES_DIFFER)
        }
        if (expected.playlists != actual.playlists) {
            return ReadParity(false, ReadBlocker.PLAYLIST_TRACKS_DIFFER)
        }
        return ReadParity.VERIFIED
    }
}
