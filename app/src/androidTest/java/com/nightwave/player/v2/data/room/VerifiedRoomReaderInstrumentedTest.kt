package com.nightwave.player.v2.data.room

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.VideoPreview
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VerifiedRoomReaderInstrumentedTest {
    private lateinit var app: Context
    private lateinit var db: NightWaveDatabase
    private lateinit var legacy: MusicLibrary
    private lateinit var migrator: RoomShadowMigrator
    private lateinit var reader: VerifiedRoomReader

    private val a = VideoPreview("First", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 121)
    private val b = VideoPreview("Second", "Artist", "https://www.youtube.com/watch?v=J---aiyznGQ", "", 179)

    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(app, NightWaveDatabase::class.java).build()
        legacy = MusicLibrary(app)
        migrator = RoomShadowMigrator(app, db)
        reader = VerifiedRoomReader(app, db)
    }

    @After fun cleanup() {
        db.close()
        app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun roomIsNotEligibleBeforeMigration() = runBlocking {
        val result = reader.inspect()
        assertFalse(result.canUseRoom)
        assertNull(result.snapshot)
        assertEquals(ReadBlocker.MISSING_CHECKPOINT, result.parity.blocker)
    }

    @Test fun migrationCreatesVerifiedRoomRead() = runBlocking {
        legacy.createPlaylist("Focus")
        legacy.addToPlaylist("Focus", b)
        legacy.addToPlaylist("Focus", a)
        legacy.toggleFavorite(a)
        legacy.addRecent(b)
        migrator.reconcile()
        val result = reader.inspect()
        assertTrue(result.canUseRoom)
        assertEquals(listOf(a), result.snapshot!!.favorites)
        assertEquals(listOf(b, a), result.snapshot!!.playlist("Focus"))
        assertEquals(listOf("Focus"), result.snapshot!!.playlistNames())
    }

    @Test fun newLegacyEditMakesRoomSnapshotIneligibleUntilReconcile() = runBlocking {
        legacy.toggleFavorite(a)
        migrator.reconcile()
        assertTrue(reader.inspect().canUseRoom)
        legacy.toggleFavorite(b)
        val stale = reader.inspect()
        assertFalse(stale.canUseRoom)
        assertNull(stale.snapshot)
        assertEquals(ReadBlocker.SOURCE_CHANGED, stale.parity.blocker)
        migrator.reconcile()
        assertTrue(reader.inspect().canUseRoom)
        assertEquals(listOf(b, a), legacy.favorites())
    }

    @Test fun sameCountsButCorruptedRoomMetadataForcesFallback() = runBlocking {
        legacy.toggleFavorite(a)
        migrator.reconcile()
        db.withTransaction {
            db.libraryDao().saveTracks(listOf(
                SavedTrack(a.url, "Corrupted title", a.author, a.thumbnail, a.durationSeconds)
            ))
        }
        val fail = reader.inspect()
        assertFalse(fail.canUseRoom)
        assertEquals(ReadBlocker.FAVORITES_DIFFER, fail.parity.blocker)
        assertEquals(listOf(a), legacy.favorites())
    }

    @Test fun duplicatedTrackUrlWithDifferentMetadataNeverPassesReadParity() = runBlocking {
        legacy.createPlaylist("Travel")
        legacy.addToPlaylist("Travel", b.copy(title = "Alternate title"))
        legacy.toggleFavorite(b)
        migrator.reconcile()
        val fail = reader.inspect()
        assertFalse(fail.canUseRoom)
        assertNull(fail.snapshot)
        assertEquals(ReadBlocker.PLAYLIST_TRACKS_DIFFER, fail.parity.blocker)
        assertEquals("Alternate title", legacy.playlist("Travel").single().title)
    }

    @Test fun malformedLegacySourceDoesNotExposeRoomMirror() = runBlocking {
        legacy.toggleFavorite(a)
        migrator.reconcile()
        val prefs = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        prefs.edit().putString("favorites", """[{"url":"https://example.invalid/bad"}]""").commit()
        try {
            reader.inspect()
            fail("Should reject malformed source")
        } catch (_: IllegalArgumentException) {
            // The UI catches and falls back to untouched legacy preferences.
            assertNotNull(db.libraryDao().checkpoint(RoomShadowMigrator.CHECKPOINT))
        }
    }
}
