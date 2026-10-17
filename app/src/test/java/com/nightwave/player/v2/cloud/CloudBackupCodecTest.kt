package com.nightwave.player.v2.cloud

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryBackupCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure local tests: no Google login, Drive calls, or user data mutation. */
class CloudBackupCodecTest {
    private fun song(id: String) = VideoPreview(
        "Song $id", "Artist", "https://www.youtube.com/watch?v=$id", "", 60L
    )
    private val a = song("dQw4w9WgXcQ")
    private val b = song("9bZkp7q19f0")
    private fun library() = LibraryBackupCodec.encode(
        LibraryArchive("NightWave Listener", "MIDNIGHT", "orbit",
            listOf(a), listOf("Mix" to listOf(a, b)))
    )

    @Test fun roundtripKeepsLibraryAndExplicitHistory() {
        val decoded = CloudBackupCodec.decode(CloudBackupCodec.encode(library(), listOf(b), true))
        assertEquals(1, decoded.archive.favorites.size)
        assertEquals(1, decoded.archive.playlists.size)
        assertEquals("Mix", decoded.archive.playlists.first().first)
        assertTrue(decoded.includesHistory)
        assertEquals(b.url, decoded.history.single().url)
    }

    @Test fun historyNotExportedWithoutOptIn() {
        val output = CloudBackupCodec.encode(library(), listOf(b), false)
        val decoded = CloudBackupCodec.decode(output)
        assertFalse(decoded.includesHistory)
        assertTrue(decoded.history.isEmpty())
        assertFalse(output.toString(Charsets.UTF_8).contains(b.url))
    }

    @Test fun corruptedEnvelopeFailsWithoutRestore() {
        val bytes = CloudBackupCodec.encode(library(), emptyList(), false)
        val modified = bytes.copyOf().also { it[0] = 0 }
        assertThrows(Exception::class.java) { CloudBackupCodec.decode(modified) }
    }

    @Test fun duplicatedRecentVideoIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.encode(library(), listOf(a, a), true)
        }
    }

    @Test fun oversizedHistoryIsRejectedAtValidation() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.validatedHistory(List(101) { a })
        }
    }
}
