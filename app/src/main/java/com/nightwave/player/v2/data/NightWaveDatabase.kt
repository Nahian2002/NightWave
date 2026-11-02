package com.nightwave.player.v2.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/** V2 staging database. Existing SharedPreferences remain the live music source. */
@Entity(tableName = "mirror_collections")
data class MirrorCollection(
    @PrimaryKey val collectionKey: String,
    val displayName: String,
    val position: Int
)

@Entity(tableName = "mirror_tracks", primaryKeys = ["collectionKey", "position"])
data class MirrorTrack(
    val collectionKey: String,
    val position: Int,
    val url: String,
    val title: String,
    val artist: String,
    val artwork: String,
    val durationSeconds: Long
)

@Entity(tableName = "mirror_state")
data class MirrorState(
    @PrimaryKey val key: String = "local",
    val migrationFormat: Int,
    val lastSyncedMs: Long,
    val collectionCount: Int,
    val trackCount: Int
)

@Dao
interface LibraryMirrorDao {
    @Query("SELECT * FROM mirror_collections ORDER BY position ASC")
    suspend fun collections(): List<MirrorCollection>

    @Query("SELECT * FROM mirror_tracks ORDER BY collectionKey, position")
    suspend fun tracks(): List<MirrorTrack>

    @Query("SELECT * FROM mirror_state WHERE 'local' = key LIMIT 1")
    suspend fun checkpoint(): MirrorState?

    @Query("DELETE FROM mirror_collections")
    suspend fun clearCollections()

    @Query("DELETE FROM mirror_tracks")
    suspend fun clearTracks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCollections(records: List<MirrorCollection>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTracks(records: List<MirrorTrack>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun checkpoint(value: MirrorState)
}

@Database(
    entities = [MirrorCollection::class, MirrorTrack::class, MirrorState::class],
    version = 1,
    exportSchema = true
)
abstract class NightWaveDatabase : RoomDatabase() {
    abstract fun mirrorDao(): LibraryMirrorDao

    companion object {
        @Volatile private var db: NightWaveDatabase? = null

        fun get(context: Context): NightWaveDatabase =
            db ?: synchronized(this) {
                db ?: Room.databaseBuilder(
                    context.applicationContext, NightWaveDatabase::class.java,
                    "nightwave_v2_library.db"
                ).build().also { db = it }
            }
    }
}
