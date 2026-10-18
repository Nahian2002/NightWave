package com.nightwave.player.offline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Private test-only fixtures; the previous index is restored after each test. */
@RunWith(AndroidJUnit4::class)
class OfflineLibraryHealthInstrumentedTest {
    private val app: Context = ApplicationProvider.getApplicationContext()
    private val library = OfflineAudioLibrary(app)
    private val prefs = app.getSharedPreferences("nightwave_offline_v1", Context.MODE_PRIVATE)

    private fun indexedSong(song: OfflineSong): String =
        JSONArray().put(JSONObject()
            .put("id", song.id).put("title", song.title)
            .put("mime", song.mimeType).put("size", song.bytes)).toString()

    private fun withIndex(raw: String, block: () -> Unit) {
        val before = prefs.all["items"]
        try {
            assertTrue(prefs.edit().putString("items", raw).commit())
            block()
        } finally {
            val editor = prefs.edit()
            when (before) {
                null -> editor.remove("items")
                is String -> editor.putString("items", before)
                is Int -> editor.putInt("items", before)
                is Long -> editor.putLong("items", before)
                is Boolean -> editor.putBoolean("items", before)
                is Float -> editor.putFloat("items", before)
                is Set<*> -> editor.putStringSet("items", before.filterIsInstance<String>().toSet())
                else -> editor.remove("items")
            }
            assertTrue(editor.commit())
        }
    }

    @Test fun missingAndChangedFilesNeverLoseTheirIndexRows() {
        val id = UUID.randomUUID().toString() + ".mp3"
        val song = OfflineSong(id, "Temporary test", "audio/mpeg", 256L)
        val root = File(app.filesDir, "offline_audio")
        assertTrue(root.isDirectory || root.mkdirs())
        val file = File(root, id)
        try {
            // Never overwrite another file, even in the unlikely case of an ID collision.
            assertTrue(file.createNewFile())
            file.writeBytes(ByteArray(256) { 7 })
            val raw = indexedSong(song)
            withIndex(raw) {
                assertEquals(OfflineFileHealth.AVAILABLE,
                    library.healthReport().entries.single().health)
                assertEquals(256L, library.fileFor(song)?.length())

                file.writeBytes(ByteArray(33) { 7 })
                assertEquals(OfflineFileHealth.SIZE_CHANGED,
                    library.healthReport().entries.single().health)
                assertNull(library.fileFor(song))
                assertFalse(library.delete(song))
                assertTrue(file.exists())
                assertEquals(raw, prefs.getString("items", null))

                assertTrue(file.delete())
                assertEquals(OfflineFileHealth.MISSING,
                    library.healthReport().entries.single().health)
                assertFalse(library.delete(song))
                assertEquals(raw, prefs.getString("items", null))
            }
        } finally {
            file.delete() // only our unique temporary test fixture
        }
    }

    @Test fun unreadableIndexHasAReportButKeepsOriginalBytes() {
        val broken = "{not valid JSON"
        withIndex(broken) {
            val report = library.healthReport()
            assertFalse(report.indexReadable)
            assertTrue(report.entries.isEmpty())
            assertEquals(broken, prefs.getString("items", null))
        }
    }

    @Test fun mismatchedTypeIsReportedWithoutRemovingRecordedEntry() {
        val song = OfflineSong(UUID.randomUUID().toString() + ".mp3",
            "Metadata mismatch", "audio/flac", 256L)
        val raw = indexedSong(song)
        withIndex(raw) {
            val report = library.healthReport()
            assertTrue(report.indexReadable)
            assertEquals(OfflineFileHealth.UNSAFE_METADATA, report.entries.single().health)
            assertNull(library.fileFor(song))
            assertFalse(library.delete(song))
            assertEquals(raw, prefs.getString("items", null))
        }
    }
}
