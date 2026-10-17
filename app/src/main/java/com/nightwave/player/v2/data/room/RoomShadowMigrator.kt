package com.nightwave.player.v2.data.room

import android.content.Context
import androidx.room.withTransaction
import com.nightwave.player.net.VideoPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ShadowMigrationReport(
    val changed: Boolean,
    val favorites: Int,
    val playlists: Int,
    val songs: Int,
    val recent: Int,
    val sourceDigest: String
)

/**
 * Phase 3B: legacy SharedPreferences are STILL authoritative. Room is an
 * inspectable mirror and never writes/deletes the old user collection.
 *
 * Every replacement, including checkpoint, is ONE Room transaction. A failed
 * import cannot leave a half-written Room mirror. Retry after a later legacy
 * change or launch; do not automatically restore/export from Room yet.
 */
class RoomShadowMigrator(context: Context, private val database: NightWaveDatabase) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
    private val dao = database.libraryDao()

    suspend fun reconcile(): ShadowMigrationReport = withContext(Dispatchers.IO) {
        // Ignore unrelated future preference fields, but fail closed if a
        // field we are actually importing has the wrong type.
        val strings = prefs.all.filterKeys { key ->
            key in setOf("favorites", "recent", "playlist_names") || key.startsWith("list_")
        }.mapValues { (key, value) ->
            require(value is String) { "Unexpected legacy value type for $key" }
            value
        }
        val snapshot = LegacyLibrarySnapshotParser.parse(strings)
        val songsByUrl = linkedMapOf<String, SavedTrack>()
        fun collect(track: VideoPreview) {
            songsByUrl[track.url] = SavedTrack(
                track.url, track.title, track.author,
                track.thumbnail, track.durationSeconds
            )
        }
        // Prefer a favorite's title/artwork when the same track appears multiple times.
        snapshot.playlists.forEach { (_, songs) -> songs.forEach(::collect) }
        snapshot.recent.forEach(::collect)
        snapshot.favorites.forEach(::collect)

        val didChange = database.withTransaction {
            if (dao.checkpoint(CHECKPOINT)?.legacyDigest == snapshot.digest) return@withTransaction false
            // Foreign keys: dependent rows first; the old source is never edited.
            dao.clearPlaylistEntries()
            dao.clearFavorites()
            dao.clearRecent()
            dao.clearPlaylists()
            dao.clearTracks()
            dao.saveTracks(songsByUrl.values.toList())
            dao.savePlaylists(snapshot.playlists.mapIndexed { i, (name, _) ->
                SavedPlaylist(name, i)
            })
            dao.savePlaylistEntries(snapshot.playlists.flatMap { (name, tracks) ->
                tracks.mapIndexed { i, video -> PlaylistEntry(name, video.url, i) }
            })
            dao.saveFavorites(snapshot.favorites.mapIndexed { i, video ->
                SavedFavorite(video.url, i)
            })
            dao.saveRecent(snapshot.recent.mapIndexed { i, video ->
                SavedRecent(video.url, i)
            })
            dao.saveCheckpoint(MigrationCheckpoint(
                CHECKPOINT, snapshot.digest, System.currentTimeMillis()
            ))
            true
        }
        ShadowMigrationReport(
            changed = didChange, favorites = snapshot.favorites.size,
            playlists = snapshot.playlists.size, songs = songsByUrl.size,
            recent = snapshot.recent.size, sourceDigest = snapshot.digest
        )
    }

    companion object {
        const val CHECKPOINT = "legacy_library_v1_mirror"
    }
}
