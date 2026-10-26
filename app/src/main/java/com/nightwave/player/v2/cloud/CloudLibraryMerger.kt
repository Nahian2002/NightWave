package com.nightwave.player.v2.cloud

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryBackupCodec
import com.nightwave.player.v2.data.PlaylistNameIdentity

/**
 * Manual cross-device collection merge. Always preserves local profile, recent
 * history, existing playlist order and existing playlist content.
 *
 * A remote playlist with a matching name but different song order/content is
 * imported as a separately named "(Drive)" copy, never silently concatenated.
 */
data class CloudMergePlan(
    val expectedLocalBackup: ByteArray,
    val mergedArchive: LibraryArchive,
    val addedFavoriteCount: Int,
    val addedPlaylistCount: Int,
    val conflictingPlaylistNames: List<String>,
    val newTrackReferences: Int,
    val hasChanges: Boolean
)

object CloudLibraryMerger {
    fun preview(localBackup: ByteArray, remote: LibraryArchive): CloudMergePlan {
        // Validate BOTH sides using the existing strict, bounded v1 codec.
        val local = LibraryBackupCodec.decode(localBackup)
        val cleanRemote = LibraryBackupCodec.decode(LibraryBackupCodec.encode(remote))

        val localFavorites = local.favorites.map { it.url }.toSet()
        val additions = cleanRemote.favorites.filter { it.url !in localFavorites }
        val mergedFavorites = local.favorites + additions
        require(mergedFavorites.size <= 500) {
            "Too many favorites to merge safely (500 maximum). Nothing changed."
        }

        val names = local.playlists.map { PlaylistNameIdentity.key(it.first) }.toMutableSet()
        val mergedPlaylists = local.playlists.toMutableList()
        val conflicts = mutableListOf<String>()
        var appendedReferences = 0

        for ((remoteName, remoteTracks) in cleanRemote.playlists) {
            val existing = mergedPlaylists.firstOrNull {
                PlaylistNameIdentity.same(it.first, remoteName)
            }
            val remoteUrls = remoteTracks.map { it.url }
            if (existing != null && existing.second.map { it.url } == remoteUrls) {
                continue // Already identical in content and order. Keep local metadata.
            }
            if (existing != null) {
                // Idempotent: repeat manual merge of the SAME remote snapshot
                // must not create "Drive 2", "Drive 3" copies forever.
                val alreadyImported = mergedPlaylists.any { (storedName, storedTracks) ->
                    (1..51).any { suffix ->
                        PlaylistNameIdentity.same(storedName, importedName(remoteName, suffix))
                    } && storedTracks.map { it.url } == remoteUrls
                }
                if (alreadyImported) continue
            }
            val importedName = if (existing == null) remoteName
                else {
                    conflicts.add(remoteName)
                    availableImportedName(remoteName, names)
                }
            // Every imported playlist is an intact copy; never change existing order.
            require(names.add(PlaylistNameIdentity.key(importedName))) {
                "Duplicate playlist during merge"
            }
            mergedPlaylists.add(importedName to remoteTracks)
            appendedReferences += remoteTracks.size
            require(mergedPlaylists.size <= 50) {
                "Too many playlists to merge safely (50 maximum). Nothing changed."
            }
        }

        // Keep this device's profile and local recent history. Cloud history
        // requires the separate 5U opt-in restore path and is never merged here.
        val merged = local.copy(
            favorites = mergedFavorites,
            playlists = mergedPlaylists
        )
        // Fail closed if JSON archive size, video metadata or any other v1
        // contract constraint is exceeded. This happens BEFORE showing Apply.
        LibraryBackupCodec.decode(LibraryBackupCodec.encode(merged))
        val changed = additions.isNotEmpty() || mergedPlaylists.size != local.playlists.size
        return CloudMergePlan(
            expectedLocalBackup = localBackup.copyOf(),
            mergedArchive = merged,
            addedFavoriteCount = additions.size,
            addedPlaylistCount = mergedPlaylists.size - local.playlists.size,
            conflictingPlaylistNames = conflicts.toList(),
            newTrackReferences = appendedReferences,
            hasChanges = changed
        )
    }

    private fun importedName(name: String, suffix: Int): String {
        val marker = if (suffix == 1) " (Drive)" else " (Drive $suffix)"
        return name.take(60 - marker.length).trimEnd() + marker
    }

    private fun availableImportedName(name: String, used: Set<String>): String {
        for (suffix in 1..51) {
            val candidate = importedName(name, suffix)
            if (PlaylistNameIdentity.key(candidate) !in used) return candidate
        }
        error("Could not allocate a safe imported playlist name")
    }
}
