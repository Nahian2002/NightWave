package com.nightwave.player.library

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

/** Immutable input to the single-writer coordinator. Preserve per-playlist metadata. */
data class LibraryCollections(
    val favorites: List<VideoPreview>,
    val recent: List<VideoPreview>,
    val playlists: List<Pair<String, List<VideoPreview>>>
) {
    fun songs(name: String): List<VideoPreview> = playlists.firstOrNull { it.first == name }?.second.orEmpty()
    fun names(): List<String> = playlists.map { it.first }
}

sealed interface LibraryChange {
    data class ToggleFavorite(val song: VideoPreview) : LibraryChange
    data class RecordRecent(val song: VideoPreview) : LibraryChange
    data class CreatePlaylist(val name: String) : LibraryChange
    data class DeletePlaylist(val name: String) : LibraryChange
    data class RenamePlaylist(val oldName: String, val newName: String) : LibraryChange
    data class AddToPlaylist(val name: String, val song: VideoPreview) : LibraryChange
    data class RemoveFromPlaylist(val name: String, val url: String) : LibraryChange
}

/** 'accepted' reports command success; 'added' is for toggleFavorite semantics. */
data class LibraryChangeResult(
    val updated: LibraryCollections,
    val accepted: Boolean,
    val changed: Boolean,
    val added: Boolean = false
)

object LibraryMutationPlan {
    private fun video(raw: VideoPreview): VideoPreview? {
        val url = VideoLink.normalize(raw.url) ?: return null
        // Match the legacy metadata limits instead of rejecting an otherwise
        // playable track because YouTube returned a long title or unknown length.
        return raw.copy(
            url = url,
            title = raw.title.take(250),
            author = raw.author.take(150),
            thumbnail = raw.thumbnail.take(2000),
            durationSeconds = raw.durationSeconds.coerceIn(0L, 86400L)
        )
    }

    fun apply(old: LibraryCollections, operation: LibraryChange): LibraryChangeResult {
        fun rejected() = LibraryChangeResult(old, false, false)
        fun accepted(updated: LibraryCollections, added: Boolean = false) =
            LibraryChangeResult(updated, true, updated != old, added)

        return when (operation) {
            is LibraryChange.ToggleFavorite -> {
                val song = video(operation.song) ?: return rejected()
                val present = old.favorites.any { it.url == song.url }
                val updated = if (present) old.favorites.filterNot { it.url == song.url }
                    else listOf(song) + old.favorites.filterNot { it.url == song.url }.take(499)
                accepted(old.copy(favorites = updated), added = !present)
            }
            is LibraryChange.RecordRecent -> {
                val song = video(operation.song) ?: return rejected()
                // Keep the newest song plus up to 99 older distinct songs.
                // Previously take(99) applied to the combined list, capping
                // all listening history at 99 instead of the intended 100.
                accepted(old.copy(recent = (listOf(song) +
                    old.recent.filterNot { it.url == song.url }).take(100)))
            }
            is LibraryChange.CreatePlaylist -> {
                val name = operation.name.trim().take(60)
                if (name.isBlank() || old.playlists.size >= 50 ||
                    old.names().any { it.equals(name, true) }) return rejected()
                accepted(old.copy(playlists = old.playlists + (name to emptyList())))
            }
            is LibraryChange.DeletePlaylist -> {
                if (operation.name !in old.names()) return rejected()
                accepted(old.copy(playlists = old.playlists.filterNot { it.first == operation.name }))
            }
            is LibraryChange.RenamePlaylist -> {
                val newName = operation.newName.trim().take(60)
                if (newName.isBlank() || operation.oldName !in old.names() ||
                    old.names().any { it.equals(newName, true) && it != operation.oldName }) return rejected()
                accepted(old.copy(playlists = old.playlists.map { (name, songs) ->
                    if (name == operation.oldName) newName to songs else name to songs
                }))
            }
            is LibraryChange.AddToPlaylist -> {
                val song = video(operation.song) ?: return rejected()
                if (operation.name !in old.names()) return rejected()
                val songs = old.songs(operation.name)
                if (songs.size >= 300 || songs.any { it.url == song.url }) return rejected()
                accepted(old.copy(playlists = old.playlists.map { (name, entries) ->
                    name to if (name == operation.name) entries + song else entries
                }))
            }
            is LibraryChange.RemoveFromPlaylist -> {
                if (operation.name !in old.names()) return rejected()
                val url = VideoLink.normalize(operation.url) ?: return rejected()
                val tracks = old.songs(operation.name)
                if (tracks.none { it.url == url }) return rejected()
                accepted(old.copy(playlists = old.playlists.map { (name, songs) ->
                    name to if (name == operation.name) songs.filterNot { it.url == url } else songs
                }))
            }
        }
    }
}
