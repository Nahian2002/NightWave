package com.nightwave.player.offline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Only private UUID test files are created/deleted. Existing library rows are
 * never cleared, and the tests do not contact any network service.
 */
@RunWith(AndroidJUnit4::class)
class AuthorizedAudioImportIntegrityInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val library = OfflineAudioLibrary(context)

    private fun stage(content: ByteArray): File {
        val folder = File(context.filesDir, "permitted_download_staging")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val staged = File(folder, UUID.randomUUID().toString() + ".pending")
        assertTrue(staged.createNewFile())
        staged.writeBytes(content)
        return staged
    }

    @Test fun identicalDownloadedBytesReuseExistingSong() {
        val bytes = ("NW19 integrity " + UUID.randomUUID()).toByteArray(Charsets.UTF_8)
        val first = stage(bytes)
        val second = stage(bytes)
        var added: OfflineSong? = null
        try {
            added = library.importPermittedStagedAudio(first, "Song.mp3")
            assertArrayEquals(bytes, requireNotNull(library.fileFor(added)).readBytes())
            val duplicate = library.importPermittedStagedAudio(second, "Renamed.mp3")
            assertEquals(added.id, duplicate.id)
            assertEquals(1, library.songs().count { it.id == added.id })
            assertArrayEquals(bytes, requireNotNull(library.fileFor(duplicate)).readBytes())
        } finally {
            if (added != null) library.delete(added)
            first.delete()
            second.delete()
        }
    }

    @Test fun sameSizeButDifferentBytesAreNotDeduplicated() {
        val prefix = ("NW19 " + UUID.randomUUID()).toByteArray(Charsets.UTF_8)
        val a = stage(prefix + byteArrayOf(1))
        val b = stage(prefix + byteArrayOf(2))
        var first: OfflineSong? = null
        var second: OfflineSong? = null
        try {
            first = library.importPermittedStagedAudio(a, "Different1.mp3")
            second = library.importPermittedStagedAudio(b, "Different2.mp3")
            assertNotEquals(first.id, second.id)
            assertArrayEquals(prefix + byteArrayOf(1),
                requireNotNull(library.fileFor(first)).readBytes())
            assertArrayEquals(prefix + byteArrayOf(2),
                requireNotNull(library.fileFor(second)).readBytes())
        } finally {
            if (first != null) library.delete(first)
            if (second != null) library.delete(second)
            a.delete()
            b.delete()
        }
    }
}
