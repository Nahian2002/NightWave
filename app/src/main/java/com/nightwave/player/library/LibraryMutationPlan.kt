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
    data class RemoveRecent(val url: String) : LibraryChange
    data object ClearRecent : LibraryChange
    data class CreatePlaylist(val name: String) : LibraryChange
    data class DeletePlaylist(val name: String) : LibraryChange
    data class RenamePlaylist(val oldName: String, val newName: String) : LibraryChange
    data class AddToPlaylist(val name: String, val song: VideoPreview) : LibraryChange
    data class ImportSharedPlaylist(val name: String, val tracks: List<VideoPreview>) : LibraryChange
    data class RemoveFromPlaylist(val name: String, val url: String) : LibraryChange
    /** Exact playlist snapshot guards prevent stale UI actions from overwriting newer edits. */
    data class BulkRemoveFromPlaylist(
        val name: String, val urls: List<String>, val expectedUrls: List<String>
    ) : LibraryChange
    data class BulkAddToPlaylist(
        val name: String, val tracks: List<VideoPreview>, val expectedUrls: List<String>
    ) : LibraryChange
    data class ReorderPlaylist(
        val name: String, val expectedUrls: List<String>, val orderedUrls: List<String>
    ) : LibraryChange
    /** One-level Undo: only the precisely matching post-edit playlist is restored. */
    data class RestorePlaylistIfUnchanged(
        val name: String, val expected: List<VideoPreview>, val restore: List<VideoPreview>
    ) : LibraryChange
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
            is LibraryChange.RemoveRecent -> {
                val url = VideoLink.normalize(operation.url) ?: return rejected()
                if (old.recent.none { it.url == url }) return rejected()
                accepted(old.copy(recent = old.recent.filterNot { it.url == url }))
            }
            LibraryChange.ClearRecent -> {
                if (old.recent.isEmpty()) return rejected()
                accepted(old.copy(recent = emptyList()))
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
            is LibraryChange.ImportSharedPlaylist -> {
                val name = operation.name.trim()
                if (name.isBlank() || name.length > 60 ||
                    name.any { it.code < 32 || it.code == 127 } ||
                    operation.tracks.size !in 1..30
                ) return rejected()
                val clean = mutableListOf<VideoPreview>()
                for (raw in operation.tracks) {
                    // Defense in depth: strictly canonical, public watch links.
                    // Never convert an offline/path/extra-query URL into one.
                    val normalized = video(raw) ?: return rejected()
                    if (normalized.url != raw.url) return rejected()
                    clean += normalized
                }
                if (clean.map { it.url }.toSet().size != clean.size) return rejected()
                val urls = clean.map { it.url }
                fun importedName(suffix: Int): String {
                    val tag = if (suffix == 1) " (Shared)" else " (Shared $suffix)"
                    return name.take(60 - tag.length).trimEnd() + tag
                }
                // Repeated imports are no-ops; different existing playlists
                // never get overwritten or silently merged.
                val candidates = listOf(name) + (1..50).map(::importedName)
                val alreadyThere = old.playlists.any { (existing, songs) ->
                    candidates.any { it.equals(existing, ignoreCase = true) } &&
                        songs.map { it.url } == urls
                }
                if (alreadyThere) return accepted(old)
                if (old.playlists.size >= 50) return rejected()
                val chosen = candidates.firstOrNull { candidate ->
                    old.names().none { it.equals(candidate, ignoreCase = true) }
                } ?: return rejected()
                accepted(old.copy(playlists = old.playlists + (chosen to clean)))
            }

            is LibraryChange.BulkRemoveFromPlaylist -> {
                if (operation.name !in old.names() ||
                    operation.urls.size !in 1..20 ||
                    operation.urls.distinct().size != operation.urls.size) return rejected()
                val tracks = old.songs(operation.name)
                if (tracks.map { it.url } != operation.expectedUrls) return rejected()
                val selected = operation.urls.map { VideoLink.normalize(it) ?: return rejected() }
                if (selected.toSet().size != selected.size ||
                    tracks.none { it.url in selected } || !selected.all { url ->
                        tracks.any { it.url == url }
                    }) return rejected()
                accepted(old.copy(playlists = old.playlists.map { (name, songs) ->
                    name to if (name == operation.name)
                        songs.filterNot { it.url in selected } else songs
                }))
            }
            is LibraryChange.BulkAddToPlaylist -> {
                if (operation.name !in old.names() ||
                    operation.tracks.size !in 1..20) return rejected()
                val tracks = old.songs(operation.name)
                if (tracks.map { it.url } != operation.expectedUrls) return rejected()
                // Normalize every input first: one unsafe entry cancels the whole batch.
                val proposed = operation.tracks.map { video(it) ?: return rejected() }
                if (proposed.map { it.url }.toSet().size != proposed.size) return rejected()
                val existing = tracks.map { it.url }.toSet()
                val additions = proposed.filterNot { it.url in existing }
                if (additions.isEmpty() || tracks.size + additions.size > 300) return rejected()
                accepted(old.copy(playlists = old.playlists.map { (name, songs) ->
                    name to if (name == operation.name) songs + additions else songs
                }))
            }
            is LibraryChange.ReorderPlaylist -> {
                if (operation.name !in old.names()) return rejected()
                val tracks = old.songs(operation.name)
                val current = tracks.map { it.url }
                if (current.size < 2 || current != operation.expectedUrls ||
                    operation.orderedUrls.size != current.size ||
                    operation.orderedUrls.toSet().size != current.size ||
                    operation.orderedUrls.toSet() != current.toSet() ||
                    operation.orderedUrls == current) return rejected()
                val byUrl = tracks.associateBy { it.url }
                val reordered = operation.orderedUrls.map { byUrl[it] ?: return rejected() }
                accepted(old.copy(playlists = old.playlists.map { (name, songs) ->
                    name to if (name == operation.name) reordered else songs
                }))
            }
            is LibraryChange.RestorePlaylistIfUnchanged -> {
                if (operation.name !in old.names() ||
                    old.songs(operation.name) != operation.expected ||
                    operation.restore.size > 300 ||
                    operation.restore.map { it.url }.toSet().size != operation.restore.size ||
                    operation.restore == operation.expected) return rejected()
                val restored = operation.restore.map { video(it) ?: return rejected() }
                if (restored != operation.restore) return rejected() // Preserve exact original metadata.
                accepted(old.copy(playlists = old.playlists.map { (name, songs) ->
                    name to if (name == operation.name) restored else songs
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
