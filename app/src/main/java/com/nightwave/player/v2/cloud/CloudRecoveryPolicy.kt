package com.nightwave.player.v2.cloud

import com.nightwave.player.v2.data.LibraryBackupCodec
import java.util.Locale

/**
 * Read-only summary of precisely what a confirmed cloud restore would replace.
 * Includes an exact copy of the local archive reviewed by the user so a stale
 * confirmation can be rejected by the original legacy transaction lock.
 */
data class CloudRecoveryReport(
    val expectedLocalBackup: ByteArray,
    val localLiked: Int,
    val cloudLiked: Int,
    val localLikedNotInCloud: Int,
    val localPlaylistCount: Int,
    val cloudPlaylistCount: Int,
    val localPlaylistsNotInCloud: Int,
    val localPlaylistsWithChangedContent: Int,
    val cloudPlaylistTracks: Int,
    val profileWouldChange: Boolean,
    val optionalHistoryCount: Int
)

object CloudRecoveryPolicy {
    fun inspect(localBytes: ByteArray, remote: CloudBackupPreview): CloudRecoveryReport {
        val local = LibraryBackupCodec.decode(localBytes)
        // Fail closed on unexpected programmatic callers, not just API decoder.
        val cloud = LibraryBackupCodec.decode(LibraryBackupCodec.encode(remote.archive))
        val remoteLikes = cloud.favorites.map { it.url }.toHashSet()
        val remotePlaylists = cloud.playlists.associateBy {
            it.first.lowercase(Locale.ROOT)
        }
        val lostLists = local.playlists.count { (name, _) ->
            name.lowercase(Locale.ROOT) !in remotePlaylists
        }
        val changedLists = local.playlists.count { (name, songs) ->
            val replacement = remotePlaylists[name.lowercase(Locale.ROOT)]
            replacement != null && (
                replacement.first != name ||
                replacement.second.map { it.url } != songs.map { it.url }
            )
        }
        val recent = if (remote.includesHistory)
            CloudBackupCodec.validatedHistory(remote.history).size
            else {
                require(remote.history.isEmpty()) { "History without opt-in is invalid" }
                0
            }
        return CloudRecoveryReport(
            expectedLocalBackup = localBytes.copyOf(),
            localLiked = local.favorites.size,
            cloudLiked = cloud.favorites.size,
            localLikedNotInCloud = local.favorites.count { it.url !in remoteLikes },
            localPlaylistCount = local.playlists.size,
            cloudPlaylistCount = cloud.playlists.size,
            localPlaylistsNotInCloud = lostLists,
            localPlaylistsWithChangedContent = changedLists,
            cloudPlaylistTracks = cloud.playlists.sumOf { it.second.size },
            profileWouldChange = local.displayName != cloud.displayName ||
                local.theme != cloud.theme || local.avatar != cloud.avatar,
            optionalHistoryCount = recent
        )
    }
}
