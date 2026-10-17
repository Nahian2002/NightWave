package com.nightwave.player.v2.data.room

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Only public, normalized video links and metadata are persisted. */
@Entity(tableName = "saved_track")
data class SavedTrack(
    @PrimaryKey val url: String,
    val title: String,
    val creator: String,
    val thumbnail: String,
    val durationSeconds: Long
)

@Entity(tableName = "playlist", indices = [Index("position", unique = true)])
data class SavedPlaylist(
    @PrimaryKey val name: String,
    val position: Int
)

@Entity(
    tableName = "playlist_entry",
    primaryKeys = ["playlistName", "url"],
    foreignKeys = [
        ForeignKey(entity = SavedPlaylist::class, parentColumns = ["name"], childColumns = ["playlistName"],
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SavedTrack::class, parentColumns = ["url"], childColumns = ["url"],
            onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("url"), Index(value = ["playlistName", "position"], unique = true)]
)
data class PlaylistEntry(val playlistName: String, val url: String, val position: Int)

@Entity(
    tableName = "favorite",
    foreignKeys = [ForeignKey(entity = SavedTrack::class, parentColumns = ["url"],
        childColumns = ["url"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("position", unique = true)]
)
data class SavedFavorite(@PrimaryKey val url: String, val position: Int)

@Entity(
    tableName = "recent",
    foreignKeys = [ForeignKey(entity = SavedTrack::class, parentColumns = ["url"],
        childColumns = ["url"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("position", unique = true)]
)
data class SavedRecent(@PrimaryKey val url: String, val position: Int)

/** Written in the SAME Room transaction as the imported rows. */
@Entity(tableName = "migration_checkpoint")
data class MigrationCheckpoint(
    @PrimaryKey val name: String,
    val legacyDigest: String,
    val committedAtMs: Long
)

@Dao
interface NightWaveLibraryDao {
    @Query("SELECT * FROM migration_checkpoint WHERE name = :name LIMIT 1")
    suspend fun checkpoint(name: String): MigrationCheckpoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCheckpoint(value: MigrationCheckpoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTracks(tracks: List<SavedTrack>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun savePlaylists(playlists: List<SavedPlaylist>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun savePlaylistEntries(entries: List<PlaylistEntry>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun saveFavorites(favorites: List<SavedFavorite>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun saveRecent(items: List<SavedRecent>)

    @Query("DELETE FROM playlist_entry")
    suspend fun clearPlaylistEntries()

    @Query("DELETE FROM favorite")
    suspend fun clearFavorites()

    @Query("DELETE FROM recent")
    suspend fun clearRecent()

    @Query("DELETE FROM playlist")
    suspend fun clearPlaylists()

    @Query("DELETE FROM saved_track")
    suspend fun clearTracks()

    @Query("SELECT * FROM playlist ORDER BY position ASC")
    fun observePlaylists(): Flow<List<SavedPlaylist>>

    @Query("SELECT saved_track.* FROM favorite INNER JOIN saved_track ON favorite.url = saved_track.url ORDER BY favorite.position ASC")
    fun observeFavorites(): Flow<List<SavedTrack>>

    @Query("SELECT saved_track.* FROM recent INNER JOIN saved_track ON recent.url = saved_track.url ORDER BY recent.position ASC")
    fun observeRecent(): Flow<List<SavedTrack>>

    @Query("SELECT saved_track.* FROM playlist_entry INNER JOIN saved_track ON playlist_entry.url = saved_track.url WHERE playlist_entry.playlistName = :name ORDER BY playlist_entry.position ASC")
    fun observePlaylistSongs(name: String): Flow<List<SavedTrack>>

    @Query("SELECT COUNT(*) FROM playlist")
    suspend fun playlistCount(): Int

    @Query("SELECT COUNT(*) FROM favorite")
    suspend fun favoriteCount(): Int

    @Query("SELECT COUNT(*) FROM saved_track")
    suspend fun trackCount(): Int

    @Query("SELECT COUNT(*) FROM recent")
    suspend fun recentCount(): Int
}

@Database(
    entities = [SavedTrack::class, SavedPlaylist::class, PlaylistEntry::class,
        SavedFavorite::class, SavedRecent::class, MigrationCheckpoint::class],
    version = 1,
    exportSchema = true
)
abstract class NightWaveDatabase : RoomDatabase() {
    abstract fun libraryDao(): NightWaveLibraryDao

    companion object {
        @Volatile private var instance: NightWaveDatabase? = null

        fun get(context: Context): NightWaveDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NightWaveDatabase::class.java,
                "nightwave-v2-library.db"
            ).build().also { instance = it }
        }
        // Never use fallbackToDestructiveMigration. A later schema upgrade requires
        // an explicit Room Migration + upgrade/rollback tests.
    }
}
