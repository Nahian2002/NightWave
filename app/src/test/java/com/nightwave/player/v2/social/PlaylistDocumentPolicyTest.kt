package com.nightwave.player.v2.social

import com.nightwave.player.net.VideoPreview
import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** No Android document picker or music library required. */
class PlaylistDocumentPolicyTest {
    private val song = VideoPreview("ভালোবাসি ঝুম", "শিল্পী",
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", 0L)

    @Test fun userOwnedFileRoundTripPreservesFullUnicodeNamesAndOrdering() {
        val second = song.copy(title = "Second", url = "https://www.youtube.com/watch?v=9bZkp7q19f0")
        val prepared = PlaylistDocumentPolicy.prepare("বাংলা favourites", listOf(song, second))!!
        assertTrue(prepared.fileName.endsWith(".nightwave.txt"))
        assertTrue(prepared.fileName.startsWith("বাংলা"))
        val parsed = PlaylistDocumentPolicy.read(ByteArrayInputStream(prepared.bytes))!!
        assertEquals("বাংলা favourites", parsed.name)
        assertEquals(listOf(song.url, second.url), parsed.tracks.map { it.url })
        assertEquals(song.title, parsed.tracks.first().title)
    }

    @Test fun filenamesCannotEscapeTheSelectedDocumentPath() {
        val prepared = PlaylistDocumentPolicy.prepare("../ unsafe:/\\ playlist", listOf(song))!!
        assertFalse(prepared.fileName.contains("/"))
        assertFalse(prepared.fileName.contains("\\"))
        assertFalse(prepared.fileName.contains(":"))
        assertTrue(prepared.fileName.endsWith(".nightwave.txt"))
    }

    @Test fun privateAudioAndOtherLinksAreNeverExported() {
        val private = song.copy(url = "offline:private-recording")
        val safe = PlaylistDocumentPolicy.prepare("Demo", listOf(private, song))!!
        assertEquals(listOf(song.url),
            PlaylistDocumentPolicy.read(ByteArrayInputStream(safe.bytes))!!.tracks.map { it.url })
        assertNull(PlaylistDocumentPolicy.prepare("Private only", listOf(private)))
    }

    @Test fun malformedUtf8MustNotBeSilentlyReplaced() {
        assertNull(PlaylistDocumentPolicy.read(ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28))))
    }

    @Test fun oversizedAndTruncatedDocumentsAreRejectedBeforeMutation() {
        val giant = ByteArray(PlaylistDocumentPolicy.MAX_BYTES + 1) { 'x'.code.toByte() }
        assertNull(PlaylistDocumentPolicy.read(ByteArrayInputStream(giant)))
        val valid = PlaylistDocumentPolicy.prepare("Safe", listOf(song))!!.bytes
        assertNull(PlaylistDocumentPolicy.read(
            ByteArrayInputStream(valid.copyOf(valid.size - 20))))
        assertNotNull(PlaylistDocumentPolicy.read(ByteArrayInputStream(valid)))
    }

    @Test fun brokenDocumentProviderZeroByteReadIsBounded() {
        val broken = object : java.io.InputStream() {
            override fun read(): Int = 0
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = 0
        }
        assertNull(PlaylistDocumentPolicy.read(broken))
    }

    @Test fun exportedPayloadMatchesExistingStrictSharedTextFormat() {
        val prepared = PlaylistDocumentPolicy.prepare("Test", listOf(song))!!
        val expected = PlaylistShareFormatter.format("Test", listOf(song))!!
        assertArrayEquals(expected.toByteArray(Charsets.UTF_8), prepared.bytes)
        assertNotNull(PlaylistShareParser.parse(expected))
    }
}
