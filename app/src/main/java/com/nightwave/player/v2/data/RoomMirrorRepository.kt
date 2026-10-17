package com.nightwave.player.v2.data

import android.content.Context
import androidx.room.withTransaction
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.backup.MusicCollection

data class LibraryMirrorStatus(
    val collectionCount: Int,
    val trackCount: Int,
    val checkpointMs: Long
)

/**
 * Stages a validated copy of current library preferences in Room. Read-after-
 * write comparison runs INSIDE the transaction, rolling back on mismatch.
 *
 * Important: this is not yet the live repository. Safe final cutover requires
 * all legacy writers to migrate together in a later verified milestone.
 */
class RoomMirrorRepository(context: Context) {
    private val database = NightWaveDatabase.get(context.applicationContext)
    private val dao = database.mirrorDao()

    suspend fun stage(snapshot: MusicCollection): LibraryMirrorStatus = database.withTransaction {
        val headings = mutableListOf(
            MirrorCollection("__favorites", "Liked Songs", 0),
            MirrorCollection("__recent", "Recently Played", 1)
        )
        snapshot.playlists.entries.forEachIndexed { index, entry ->
            headings.add(MirrorCollection("playlist:" + entry.key, entry.key, index + 2))
        }
        val entries = buildList {
            fun append(key: String, songs: List<VideoPreview>) {
                songs.forEachIndexed { position, song ->
                    add(MirrorTrack(key, position, song.url, song.title,
                        song.author, song.thumbnail, song.durationSeconds))
                }
            }
            append("__favorites", snapshot.favorites)
            append("__recent", snapshot.recent)
            snapshot.playlists.forEach { (name, tracks) -> append("playlist:" + name, tracks) }
        }
        dao.clearTracks()
        dao.clearCollections()
        dao.addCollections(headings)
        dao.addTracks(entries)
        val actual = readFromDao()
        if (actual != snapshot) error("Room staged library differs from the original. Transaction rolled back.")
        val state = MirrorState(
            migrationFormat = 1, lastSyncedMs = System.currentTimeMillis(),
            collectionCount = headings.size, trackCount = entries.size
        )
        dao.checkpoint(state)
        LibraryMirrorStatus(state.collectionCount, state.trackCount, state.lastSyncedMs)
    }

    suspend fun readSnapshot(): MusicCollection = database.withTransaction { readFromDao() }

    suspend fun status(): LibraryMirrorStatus? = dao.checkpoint()?.let {
        LibraryMirrorStatus(it.collectionCount, it.trackCount, it.lastSyncedMs)
    }

    private suspend fun readFromDao(): MusicCollection {
        val headings = dao.collections()
        val tracks = dao.tracks().groupBy { it.collectionKey }
        fun collect(key: String) = tracks[key].orEmpty().sortedBy { it.position }.map {
            VideoPreview(it.title, it.artist, it.url, it.artwork, it.durationSeconds)
        }
        val lists = linkedMapOf<String, List<VideoPreview>>()
        headings.filter { it.collectionKey.startsWith("playlist:") }.forEach {
            lists[it.displayName] = collect(it.collectionKey)
        }
        return MusicCollection(collect("__favorites"), collect("__recent"), LinkedHashMap(lists))
    }
}
