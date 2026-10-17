package com.nightwave.player.v2.data.room

import android.content.Context
import androidx.room.withTransaction
import com.nightwave.player.net.VideoPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VerifiedRoomRead(
    val parity: ReadParity,
    val snapshot: LibraryReadSnapshot?
) {
    val canUseRoom: Boolean get() = parity.eligible && snapshot != null
}

/**
 * Phase 3C opt-in READ-ONLY router.
 *
 * There is deliberately no Room authoring API here. All mutations still go
 * through MusicLibrary and GuestProfileStore; the legacy launcher is unchanged.
 * Reads require BOTH an atomic Room snapshot and an unchanged source digest.
 * A mismatch returns no Room rows to the UI and causes immediate fallback.
 */
class VerifiedRoomReader(
    context: Context,
    private val database: NightWaveDatabase
) {
    private val sourcePrefs = context.applicationContext.getSharedPreferences(
        "nightwave_library_v1", Context.MODE_PRIVATE
    )
    private val dao = database.libraryDao()

    private fun readSource(): LegacyLibrarySnapshot {
        val values = sourcePrefs.all.filterKeys { name ->
            name in setOf("favorites", "recent", "playlist_names") || name.startsWith("list_")
        }.mapValues { (key, value) ->
            require(value is String) { "Invalid legacy library type in $key" }
            value
        }
        return LegacyLibrarySnapshotParser.parse(values)
    }

    private fun toVideo(row: SavedTrack): VideoPreview = VideoPreview(
        title = row.title,
        author = row.creator,
        url = row.url,
        thumbnail = row.thumbnail,
        durationSeconds = row.durationSeconds
    )

    suspend fun inspect(): VerifiedRoomRead = withContext(Dispatchers.IO) {
        val before = readSource()
        val (digest, actual) = database.withTransaction {
            val checkpoint = dao.checkpoint(RoomShadowMigrator.CHECKPOINT)?.legacyDigest
            val names = dao.readPlaylists()
            val favorites = dao.readFavorites().map(::toVideo)
            val recent = dao.readRecent().map(::toVideo)
            val playlists = names.map { it.name to dao.readPlaylistSongs(it.name).map(::toVideo) }
            checkpoint to LibraryReadSnapshot(favorites, recent, playlists)
        }
        // The legacy player may change favorites/queue on another Activity.
        // A source change while taking the DB read invalidates this result.
        val after = readSource()
        if (before.digest != after.digest) {
            return@withContext VerifiedRoomRead(
                ReadParity(false, ReadBlocker.SOURCE_CHANGED), null
            )
        }
        val parity = LibraryReadParity.evaluate(after, actual, digest)
        VerifiedRoomRead(parity, if (parity.eligible) actual else null)
    }
}
