package com.nightwave.player.v2.library

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBulkSelectionPolicyTest {
    private fun song(id: String) = VideoPreview(id, "Artist",
        "https://www.youtube.com/watch?v=$id", "", 140L)

    @Test fun selectedTracksRetainPlaylistDisplayOrder() {
        val songs = listOf(song("aaa111bbb22"), song("ccc333ddd44"), song("eee555fff66"))
        val urls = setOf(songs[2].url, songs[0].url)
        assertEquals(listOf(songs[0], songs[2]),
            LibraryBulkSelectionPolicy.selected(songs, urls))
    }

    @Test fun cannotSelectUnknownTracksAndCanUndoASelection() {
        val tracks = listOf(song("aaa111bbb22"))
        val first = LibraryBulkSelectionPolicy.toggle(emptySet(), tracks[0].url, tracks)
        assertEquals(setOf(tracks[0].url), first)
        assertTrue(LibraryBulkSelectionPolicy.toggle(first, tracks[0].url, tracks).isEmpty())
        assertEquals(first, LibraryBulkSelectionPolicy.toggle(first, "bogus", tracks))
    }

    @Test fun selectVisibleCapsAtTwentyUniqueTracks() {
        val many = (0..40).map { song("track$it") }
        val selected = LibraryBulkSelectionPolicy.selectVisible(many + many[0])
        assertEquals(20, selected.size)
        val atCap = LibraryBulkSelectionPolicy.toggle(selected, many[25].url, many)
        assertEquals(selected, atCap)
        assertFalse(atCap.contains(many[25].url))
    }
}
