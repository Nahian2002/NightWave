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

    @Test fun nw86SelectVisibleAddsToOtherFilterSelectionsWithoutLosingThem() {
        val tracks = listOf(
            song("aaa111bbb22"), song("ccc333ddd44"), song("eee555fff66"),
            song("ggg777hhh88"))
        val firstFilter = listOf(tracks[0], tracks[1])
        val first = LibraryBulkSelectionPolicy.toggleVisible(emptySet(), firstFilter, tracks)
        assertEquals(setOf(tracks[0].url, tracks[1].url), first)
        val second = LibraryBulkSelectionPolicy.toggleVisible(
            first, listOf(tracks[2]), tracks)
        assertEquals(setOf(tracks[0].url, tracks[1].url, tracks[2].url), second)
        assertEquals(2, LibraryBulkSelectionPolicy.hiddenSelectionCount(
            second, listOf(tracks[2])))
        assertEquals("Deselect visible",
            LibraryBulkSelectionPolicy.visibleActionLabel(second, listOf(tracks[2])))
        val clearedVisible = LibraryBulkSelectionPolicy.toggleVisible(
            second, listOf(tracks[2]), tracks)
        assertEquals(first, clearedVisible)
    }

    @Test fun nw86VisibleSelectionCannotExceedLegacyAtomicTwentySongLimit() {
        val tracks = (0..29).map { index ->
            song(index.toString().padStart(11, '0'))
        }
        val original = tracks.take(18).map { it.url }.toSet()
        val selected = LibraryBulkSelectionPolicy.toggleVisible(
            original, tracks.drop(18), tracks)
        assertEquals(20, selected.size)
        assertTrue(selected.containsAll(original))
        assertFalse(selected.contains(tracks.last().url))
        assertEquals(tracks.take(20), LibraryBulkSelectionPolicy.selected(tracks, selected))
        assertEquals(20, LibraryBulkSelectionPolicy.MAX_SELECTED)
    }

    @Test fun nw86UnknownRowsCannotBeSelectedThroughStaleFilteredViews() {
        val known = listOf(song("aaa111bbb22"), song("ccc333ddd44"))
        val unknown = song("xxx999yyy00")
        val result = LibraryBulkSelectionPolicy.toggleVisible(
            emptySet(), listOf(known[0], unknown), known)
        assertEquals(setOf(known[0].url), result)
        val stale = LibraryBulkSelectionPolicy.toggleVisible(
            setOf(unknown.url), listOf(unknown), known)
        assertTrue(stale.isEmpty())
        assertEquals(1, LibraryBulkSelectionPolicy.hiddenSelectionCount(
            result, listOf(known[1])))
    }

    @Test fun nw86SelectingVisibleDoesNotModifyOriginalPlaylistOrOrdering() {
        val tracks = listOf(
            song("aaa111bbb22"), song("ccc333ddd44"), song("eee555fff66"))
        val original = tracks.toList()
        val selected = LibraryBulkSelectionPolicy.toggleVisible(
            setOf(tracks[0].url), listOf(tracks[2]), tracks)
        assertEquals(original, tracks)
        assertEquals(listOf(tracks[0], tracks[2]),
            LibraryBulkSelectionPolicy.selected(tracks, selected))
        assertEquals("Select visible",
            LibraryBulkSelectionPolicy.visibleActionLabel(selected, tracks))
    }
}
