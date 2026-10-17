package com.nightwave.player.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SingleWriterBasicInstrumentedTest {
    private lateinit var app: Context
    private lateinit var library: MusicLibrary
    private val song = VideoPreview("First", "Artist",
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 121)

    @Before fun start() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        app.getSharedPreferences("nightwave_v2_library_write_recovery", Context.MODE_PRIVATE)
            .edit().clear().commit()
        library = MusicLibrary(app)
    }

    @After fun finish() {
        app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun createRenameAndSongCommitStayConsistent() {
        assertTrue(library.createPlaylist("Night"))
        assertTrue(library.addToPlaylist("Night", song))
        assertTrue(library.renamePlaylist("Night", "After dark"))
        val raw = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        assertEquals("""["After dark"]""", raw.getString("playlist_names", null))
        assertFalse(raw.contains("list_Night"))
        assertTrue(raw.contains("list_After dark"))
        assertEquals(listOf(song), library.playlist("After dark"))
    }

    @Test fun corruptionBlocksEveryNewWriteWithoutErasingSavedSongs() {
        assertTrue(library.createPlaylist("Keep"))
        val raw = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        val corrupt = """[{"url":"https://invalid.example/song"}]"""
        raw.edit().putString("favorites", corrupt).commit()
        assertFalse(library.toggleFavorite(song))
        assertFalse(library.addToPlaylist("Keep", song))
        assertEquals(corrupt, raw.getString("favorites", null))
        assertEquals(listOf("Keep"), library.playlistNames())
    }

    @Test fun orphanedPlaylistSourceIsNeverSweptAway() {
        val raw = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        raw.edit().putString("list_unlisted", "[]").commit()
        assertTrue(library.toggleFavorite(song))
        assertEquals("[]", raw.getString("list_unlisted", null))
    }

    @Test fun rollbackRefusesChangesToUnlistedRecoveryKeys() {
        val raw = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        raw.edit().putString("list_unlisted", "[]").commit()
        assertTrue(library.toggleFavorite(song))
        // Even though favorite and playlist counts did not change,
        // an external edit to an orphan key invalidates recovery.
        raw.edit().putString("list_unlisted", "[ ]").commit()
        assertFalse(LegacyLibraryTransaction.recoverLastWriteIfUnchanged(app))
        assertEquals(listOf(song), library.favorites())
    }

    @Test fun recoveryIsOneShotAndRejectsNewerData() {
        assertTrue(library.toggleFavorite(song))
        assertTrue(library.toggleFavorite(song))
        assertTrue(LegacyLibraryTransaction.recoverLastWriteIfUnchanged(app))
        assertEquals(listOf(song), library.favorites())
        assertFalse(LegacyLibraryTransaction.recoverLastWriteIfUnchanged(app))
    }
}
