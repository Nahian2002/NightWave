package com.nightwave.player.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryArchiveManager
import com.nightwave.player.v2.data.GuestProfileStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SingleWriterConcurrencyInstrumentedTest {
    private lateinit var app: Context
    private val song = VideoPreview(
        "Song", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 120
    )

    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun concurrentPlaylistCreatesKeepEveryAcceptedChange() {
        val threads = Executors.newFixedThreadPool(4)
        val started = CountDownLatch(1)
        try {
            val tasks = (1..12).map { index ->
                threads.submit<Boolean> {
                    started.await(10, TimeUnit.SECONDS)
                    MusicLibrary(app).createPlaylist("Playlist $index")
                }
            }
            started.countDown()
            assertTrue(tasks.all { it.get(30, TimeUnit.SECONDS) })
            assertEquals(12, MusicLibrary(app).playlistNames().size)
        } finally {
            threads.shutdownNow()
        }
    }

    @Test fun manualArchiveImportAndLibraryEditsShareSameLock() {
        val preferences = app.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        preferences.edit().putString("list_unlisted", "[]").commit()
        val profile = GuestProfileStore(app).load()
        val archive = LibraryArchive(
            profile.displayName, profile.theme.name, profile.avatarStyle,
            listOf(song), listOf("Imported" to listOf(song))
        )
        assertTrue(LibraryArchiveManager(app).restoreWithConfirmation(archive))
        assertEquals(listOf(song), MusicLibrary(app).favorites())
        assertEquals(listOf(song), MusicLibrary(app).playlist("Imported"))
        assertEquals("[]", preferences.getString("list_unlisted", null))
    }
}
