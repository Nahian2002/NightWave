package com.nightwave.player.v2.data

import com.nightwave.player.net.VideoPreview
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LibraryBackupCodecTest {
    private val song = VideoPreview(
        "Night music", "Test Creator",
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", 203L
    )
    private fun archive() = LibraryArchive(
        "Listener", "AMOLED", "vinyl", listOf(song),
        listOf("Evening" to listOf(song), "Focus" to emptyList())
    )

    @Test fun backupRoundTripPreservesMusicAndSettings() {
        val bytes = LibraryBackupCodec.encode(archive())
        val imported = LibraryBackupCodec.decode(bytes)
        assertEquals("Listener", imported.displayName)
        assertEquals("AMOLED", imported.theme)
        assertEquals("vinyl", imported.avatar)
        assertEquals(archive().favorites, imported.favorites)
        assertEquals(archive().playlists, imported.playlists)
        assertTrue(imported.recent.isEmpty())
    }

    @Test fun archiveIncludesNoSignedAudioOrHistory() {
        val raw = String(LibraryBackupCodec.encode(archive()))
        assertFalse(raw.contains("stream_url"))
        assertFalse(raw.contains("access_token"))
        assertFalse(raw.contains("audio_url"))
        assertFalse(raw.contains("recent"))
    }

    @Test fun tamperedPayloadDoesNotImport() {
        val doc = JSONObject(String(LibraryBackupCodec.encode(archive())))
        doc.put("sha256", "0".repeat(64))
        assertThrows(IllegalArgumentException::class.java) {
            LibraryBackupCodec.decode(doc.toString().toByteArray())
        }
    }

    @Test fun rejectsWrongFormat() {
        val doc = JSONObject(String(LibraryBackupCodec.encode(archive())))
        doc.put("payload_base64", "e30=")
        assertThrows(IllegalArgumentException::class.java) {
            LibraryBackupCodec.decode(doc.toString().toByteArray())
        }
    }

    @Test fun rejectsTooManyPlaylists() {
        val archive = archive().copy(
            playlists = (0..50).map { "Playlist $it" to emptyList<VideoPreview>() }
        )
        assertThrows(IllegalArgumentException::class.java) { LibraryBackupCodec.encode(archive) }
    }

    @Test fun rejectsUntrustedVideoLinks() {
        val unsafe = song.copy(url = "https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ")
        assertThrows(IllegalArgumentException::class.java) {
            LibraryBackupCodec.encode(archive().copy(favorites = listOf(unsafe)))
        }
    }

    @Test fun rejectsDuplicateNamesWithoutOverwritingCollections() {
        assertThrows(IllegalArgumentException::class.java) {
            LibraryBackupCodec.encode(archive().copy(playlists =
                listOf("Focus" to emptyList(), "focus" to listOf(song))))
        }
    }

    @Test fun rejectsOversizedBackupsBeforeJsonParsing() {
        assertThrows(IllegalArgumentException::class.java) {
            LibraryBackupCodec.decode(ByteArray(LibraryBackupCodec.MAX_BYTES + 1))
        }
    }
}
