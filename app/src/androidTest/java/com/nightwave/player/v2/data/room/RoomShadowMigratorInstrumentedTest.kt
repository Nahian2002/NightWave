package com.nightwave.player.v2.data.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.VideoPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomShadowMigratorInstrumentedTest {
    private lateinit var app: Context
    private lateinit var db: NightWaveDatabase
    private val first = VideoPreview(
        "First", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 180
    )
    private val second = VideoPreview(
        "Second", "Artist", "https://www.youtube.com/watch?v=J---aiyznGQ", "", 90
    )

    @Before fun start() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(app, NightWaveDatabase::class.java).build()
    }

    @After fun stop() {
        db.close()
        app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun importsAndOrdersCollectionWithoutChangingLegacy() = runBlocking {
        val legacy = MusicLibrary(app)
        assertTrue(legacy.createPlaylist("Late night"))
        assertTrue(legacy.addToPlaylist("Late night", second))
        assertTrue(legacy.addToPlaylist("Late night", first))
        assertTrue(legacy.toggleFavorite(first))
        legacy.addRecent(second)
        val oldFavorites = legacy.favorites()
        val oldOrder = legacy.playlist("Late night")

        val report = RoomShadowMigrator(app, db).reconcile()
        assertTrue(report.changed)
        assertEquals(1, report.favorites)
        assertEquals(1, report.playlists)
        assertEquals(2, report.songs)
        assertEquals(listOf("Late night"), db.libraryDao().observePlaylists().first().map { it.name })
        assertEquals(listOf(second.url, first.url),
            db.libraryDao().observePlaylistSongs("Late night").first().map { it.url })
        assertEquals(listOf(first.url), db.libraryDao().observeFavorites().first().map { it.url })
        assertEquals(oldFavorites, legacy.favorites())
        assertEquals(oldOrder, legacy.playlist("Late night"))
    }

    @Test fun unchangedSnapshotIsNotRewritten() = runBlocking {
        val legacy = MusicLibrary(app)
        legacy.toggleFavorite(first)
        val migrator = RoomShadowMigrator(app, db)
        assertTrue(migrator.reconcile().changed)
        val before = db.libraryDao().checkpoint(RoomShadowMigrator.CHECKPOINT)
        assertFalse(migrator.reconcile().changed)
        assertEquals(before, db.libraryDao().checkpoint(RoomShadowMigrator.CHECKPOINT))
    }

    @Test fun invalidLegacyDataNeverDestroysPreviouslyImportedMirror() = runBlocking {
        val legacy = MusicLibrary(app)
        legacy.toggleFavorite(first)
        val migrator = RoomShadowMigrator(app, db)
        migrator.reconcile()
        val before = db.libraryDao().observeFavorites().first()
        val checkpoint = db.libraryDao().checkpoint(RoomShadowMigrator.CHECKPOINT)
        val pref = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        pref.edit().putString("favorites", """[{"url":"https://bad.example/video"}]""").commit()

        try {
            migrator.reconcile()
            fail("Expected malformed URL rejection")
        } catch (_: IllegalArgumentException) {
            assertEquals(before, db.libraryDao().observeFavorites().first())
            assertEquals(checkpoint, db.libraryDao().checkpoint(RoomShadowMigrator.CHECKPOINT))
            assertTrue(pref.getString("favorites", "").orEmpty().contains("bad.example"))
        }
    }

    @Test fun changedLegacyPreferencesRefreshMirrorWithoutOldDataLoss() = runBlocking {
        val legacy = MusicLibrary(app)
        legacy.toggleFavorite(first)
        val migrator = RoomShadowMigrator(app, db)
        migrator.reconcile()
        legacy.toggleFavorite(second)
        assertTrue(migrator.reconcile().changed)
        assertEquals(listOf(second.url, first.url),
            db.libraryDao().observeFavorites().first().map { it.url })
        assertEquals(listOf(second.url, first.url), legacy.favorites().map { it.url })
    }
}
