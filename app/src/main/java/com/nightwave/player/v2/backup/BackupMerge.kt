package com.nightwave.player.v2.backup

import com.nightwave.player.net.VideoPreview

enum class ImportStrategy { KEEP_EXISTING, REPLACE_LIBRARY, MERGE_LIBRARY }

data class ImportPreview(
    val incomingFavorites: Int,
    val incomingPlaylists: Int,
    val incomingTracks: Int,
    val incomingHistory: Int,
    val createdAtMs: Long
)

object BackupMerge {
    fun preview(backup: PortableBackup) = ImportPreview(
        backup.library.favorites.size, backup.library.playlists.size,
        backup.library.playlists.values.sumOf { it.size },
        backup.library.recent.size, backup.createdAtMs
    )

    /**
     * Pure merge; no preferences are changed. Local ordering wins. Duplicate
     * playlists are merged by case-insensitive name, with no silent truncation.
     * A capacity conflict fails the import before any user data is modified.
     */
    fun prepare(
        local: MusicCollection,
        incoming: PortableBackup,
        strategy: ImportStrategy
    ): MusicCollection {
        if (strategy == ImportStrategy.KEEP_EXISTING) return local
        if (strategy == ImportStrategy.REPLACE_LIBRARY) {
            return incoming.library.copy(
                recent = if (incoming.includesHistory) incoming.library.recent else local.recent
            )
        }
        fun union(old: List<VideoPreview>, added: List<VideoPreview>, limit: Int): List<VideoPreview> {
            val result = (old + added).distinctBy { it.url }
            if (result.size > limit) throw InvalidBackupException(
                "Collections exceed NightWave limits. Remove some songs before merging."
            )
            return result
        }
        val playlists = LinkedHashMap(local.playlists)
        for ((name, tracks) in incoming.library.playlists) {
            val existingName = playlists.keys.firstOrNull { it.equals(name, ignoreCase = true) }
            if (existingName != null) {
                playlists[existingName] = union(playlists.getValue(existingName), tracks, BackupFormat.MAX_TRACKS_PER_PLAYLIST)
            } else {
                if (playlists.size >= BackupFormat.MAX_PLAYLISTS) throw InvalidBackupException(
                    "There are too many playlists to merge."
                )
                playlists[name] = tracks
            }
        }
        return MusicCollection(
            favorites = union(local.favorites, incoming.library.favorites, 500),
            recent = if (incoming.includesHistory) union(local.recent, incoming.library.recent, 100) else local.recent,
            playlists = playlists
        )
    }
}
