package com.nightwave.player.v2.social

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistShareFormatterTest {
    private fun song(id: String, title: String = "Real song",
        artist: String = "Artist") =
        VideoPreview(title, artist, "https://www.youtube.com/watch?v=" + id, "", 150L)
    private val valid = song("dQw4w9WgXcQ")

    @Test fun emptyPlaylistDoesNotShare() {
        assertNull(PlaylistShareFormatter.format("Empty", emptyList()))
    }
    @Test fun publicLinkAndTitleAreIncluded() {
        val text = PlaylistShareFormatter.format("Road trip", listOf(valid))
        assertNotNull(text)
        assertTrue(text!!.contains("NightWave playlist: Road trip"))
        assertTrue(text.contains(valid.url))
        assertTrue(text.contains("Real song — Artist"))
    }
    @Test fun localAudioAndPathsNeverAppear() {
        val offline = VideoPreview("Private", "Me", "offline:secret.mp3", "file:///secret", 100L)
        val text = PlaylistShareFormatter.format("Safe", listOf(offline, valid))!!
        assertFalse(text.contains("offline:"))
        assertFalse(text.contains("file:///"))
        assertFalse(text.contains("Private"))
    }
    @Test fun noEligiblePublicLinksReturnsNull() {
        val local = VideoPreview("Local", "Me", "content://media/external/audio/1", "", 10L)
        assertNull(PlaylistShareFormatter.format("Local", listOf(local)))
    }
    @Test fun unknownPublicQueryParametersAreRejected() {
        val suspicious = valid.copy(url = valid.url + "&access_token=secret")
        val text = PlaylistShareFormatter.format("Safe", listOf(suspicious, valid))!!
        assertFalse(text.contains("access_token"))
        assertEquals(1, text.split(valid.url).size - 1)
    }
    @Test fun repeatedSongAppearsOnce() {
        val text = PlaylistShareFormatter.format("Repeats", listOf(valid, valid))!!
        assertEquals(1, text.split(valid.url).size - 1)
    }
    @Test fun capPreventsUnboundedShares() {
        val many = (0..99).map { song("abCD1234" + it.toString().padStart(3, '0')) }
        val text = PlaylistShareFormatter.format("Big", many)!!
        assertEquals(30, Regex("https://www\\.youtube\\.com/watch\\?v=").findAll(text).count())
        assertTrue(text.contains("first 30 eligible"))
    }
    @Test fun removesControlCharactersFromNames() {
        val text = PlaylistShareFormatter.format("First\nSecond\tName",
            listOf(song("dQw4w9WgXcQ", "Hello\nWorld", "A\rB")))!!
        assertFalse(text.contains("First\nSecond"))
        assertFalse(text.contains("Hello\nWorld"))
        assertTrue(text.contains("FirstSecondName"))
    }
    @Test fun keepsSavedTrackOrder() {
        val first = song("9bZkp7q19f0")
        val second = song("dQw4w9WgXcQ")
        val text = PlaylistShareFormatter.format("Ordered", listOf(first, second))!!
        assertTrue(text.indexOf(first.url) < text.indexOf(second.url))
    }
    @Test fun sameInputSameShareText() {
        val inputs = listOf(valid, song("9bZkp7q19f0"))
        assertEquals(PlaylistShareFormatter.format("Same", inputs),
            PlaylistShareFormatter.format("Same", inputs))
    }
}
