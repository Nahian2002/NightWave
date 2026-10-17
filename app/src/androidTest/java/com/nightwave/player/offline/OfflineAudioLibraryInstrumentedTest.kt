package com.nightwave.player.offline

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OfflineAudioLibraryInstrumentedTest {
    private val app: Context = ApplicationProvider.getApplicationContext()
    private val library = OfflineAudioLibrary(app)

    @Test fun malformedOfflineIndexReturnsMissingFileWithoutCrashingService() {
        val prefs = app.getSharedPreferences("nightwave_offline_v1", Context.MODE_PRIVATE)
        val previous = prefs.getString("items", null)
        try {
            prefs.edit().putString("items", "invalid json").commit()
            val missing = OfflineSong(
                "00000000-0000-0000-0000-000000000000.mp3", "Test", "audio/mpeg", 50L
            )
            assertNull(library.fileFor(missing))
            assertEquals("invalid json", prefs.getString("items", null))
        } finally {
            val editor = prefs.edit()
            if (previous == null) editor.remove("items") else editor.putString("items", previous)
            editor.commit()
        }
    }

    @Test fun importsAndRetainsPrivateAudioAfterReaderRecreation() {
        val source = File.createTempFile("nightwave-offline", ".mp3", app.cacheDir)
        source.writeBytes(ByteArray(128) { 0x42 })
        var id: String? = null
        try {
            val song = library.import(Uri.fromFile(source))
            id = song.id
            assertTrue(song.title.isNotBlank())
            assertEquals(128L, song.bytes)
            assertTrue(library.fileFor(song)?.exists() == true)
            val restored = OfflineAudioLibrary(app).find(song.id)
            assertEquals(song, restored)
            assertEquals(128L, OfflineAudioLibrary(app).fileFor(song)?.length())
        } finally {
            id?.let { library.find(it)?.let(library::delete) }
            source.delete()
        }
    }

    @Test fun rejectsFileWithoutAudioExtensionWithoutTouchingLibrary() {
        val before = library.songs()
        val source = File.createTempFile("nightwave-invalid", ".exe", app.cacheDir)
        source.writeBytes(ByteArray(100) { 0x42 })
        try {
            assertThrows(IllegalArgumentException::class.java) {
                library.import(Uri.fromFile(source))
            }
            assertEquals(before, library.songs())
        } finally {
            source.delete()
        }
    }

    @Test fun deletingImportedFileRemovesIndexAndPrivateCopyOnly() {
        val source = File.createTempFile("nightwave-delete", ".ogg", app.cacheDir)
        source.writeBytes(ByteArray(64) { 0x10 })
        try {
            val song = library.import(Uri.fromFile(source))
            val saved = library.fileFor(song)
            assertNotNull(saved)
            assertTrue(library.delete(song))
            assertNull(library.find(song.id))
            assertFalse(saved!!.exists())
            assertTrue(source.exists()) // original user file is never deleted
        } finally { source.delete() }
    }
}
