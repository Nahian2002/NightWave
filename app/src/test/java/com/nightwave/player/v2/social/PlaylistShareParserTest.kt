package com.nightwave.player.v2.social

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistShareParserTest {
    private fun song(id: String, title: String = "Track", artist: String = "Artist") =
        VideoPreview(title, artist, "https://www.youtube.com/watch?v=" + id, "", 180L)
    private val first = song("dQw4w9WgXcQ")
    private val second = song("9bZkp7q19f0")

    @Test fun roundTripUsesExistingSixMOneShareFormat() {
        val shared = PlaylistShareFormatter.format("Road drive", listOf(first, second))!!
        val imported = PlaylistShareParser.parse(shared)!!
        assertEquals("Road drive", imported.name)
        assertEquals(listOf(first.url, second.url), imported.tracks.map { it.url })
        assertEquals("Track", imported.tracks.first().title)
        assertEquals("Artist", imported.tracks.first().author)
        assertEquals(0L, imported.tracks.first().durationSeconds)
    }

    @Test fun windowsLineEndingsAreAccepted() {
        val raw = PlaylistShareFormatter.format("Road", listOf(first))!!
        assertNotNull(PlaylistShareParser.parse(raw.replace("\n", "\r\n")))
    }

    @Test fun unknownTextIsNotAPlaylist() {
        assertNull(PlaylistShareParser.parse("watch this song"))
        assertNull(PlaylistShareParser.parse(first.url))
    }

    @Test fun rejectsIncompleteAndTruncatedShares() {
        val text = PlaylistShareFormatter.format("Saved", listOf(first))!!
        assertNull(PlaylistShareParser.parse(text.dropLast(15)))
        assertNull(PlaylistShareParser.parse(text.substringBefore(first.url)))
    }

    @Test fun rejectsCountMismatchAndWrongTrackOrder() {
        val text = PlaylistShareFormatter.format("Saved", listOf(first, second))!!
        assertNull(PlaylistShareParser.parse(text.replace("(2 shared)", "(3 shared)")))
        assertNull(PlaylistShareParser.parse(text.replace("1. Track", "2. Track")))
    }

    @Test fun rejectsRepeatedUrls() {
        val text = PlaylistShareFormatter.format("Saved", listOf(first, second))!!
        assertNull(PlaylistShareParser.parse(text.replace(second.url, first.url)))
    }

    @Test fun rejectsSecretParametersAndLocalFiles() {
        val text = PlaylistShareFormatter.format("Saved", listOf(first))!!
        assertNull(PlaylistShareParser.parse(text.replace(first.url,
            first.url + "&access_token=secret")))
        assertNull(PlaylistShareParser.parse(text.replace(first.url,
            "file:///storage/emulated/0/Music/song.mp3")))
    }

    @Test fun rejectsOversizedAndOverlongText() {
        val text = PlaylistShareFormatter.format("Saved", listOf(first))!!
        assertNull(PlaylistShareParser.parse(text + "x".repeat(PlaylistShareParser.MAX_TEXT_CHARS)))
        assertNull(PlaylistShareParser.parse(text.replace("1. Track",
            "1. " + "x".repeat(154))))
    }

    @Test fun rejectsZeroSongsAndUnexpectedTrailers() {
        val text = PlaylistShareFormatter.format("Saved", listOf(first))!!
        assertNull(PlaylistShareParser.parse(text.replace("(1 shared)", "(0 shared)")))
        assertNull(PlaylistShareParser.parse(text + "\nSigned stream: private"))
    }

    @Test fun acceptsOfficialThirtySongOmissionNotice() {
        val songs = (1..40).map {
            song("abCD1234" + it.toString().padStart(3, '0'))
        }
        val text = PlaylistShareFormatter.format("Big", songs)!!
        assertTrue(text.contains("Only the first 30 eligible"))
        assertEquals(30, PlaylistShareParser.parse(text)!!.tracks.size)
    }

    @Test fun neverImportsOriginalOfflineFilesOrArtwork() {
        val private = VideoPreview("Private", "Me", "offline:secret.mp3", "file:///secret", 100L)
        val text = PlaylistShareFormatter.format("Safe", listOf(private, first))!!
        val imported = PlaylistShareParser.parse(text)!!
        assertEquals(1, imported.tracks.size)
        assertFalse(imported.tracks.any {
            it.url.startsWith("offline:") || it.thumbnail.isNotEmpty()
        })
    }

    @Test fun preservesSharedOrder() {
        val text = PlaylistShareFormatter.format("Order", listOf(second, first))!!
        assertEquals(listOf(second.url, first.url),
            PlaylistShareParser.parse(text)!!.tracks.map { it.url })
    }
}
