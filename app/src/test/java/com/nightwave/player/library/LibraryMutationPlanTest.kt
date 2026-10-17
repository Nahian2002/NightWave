package com.nightwave.player.library

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.*
import org.junit.Test

class LibraryMutationPlanTest {
    private fun song(i: Int): VideoPreview = VideoPreview(
        "Track $i", "Artist $i",
        "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
        "", 180
    )
    private val zero = LibraryCollections(emptyList(), emptyList(), emptyList())

    @Test fun favoritesToggleAddsThenRemovesWithoutChangingOrderOfOthers() {
        val a = song(1); val b = song(2)
        val first = LibraryMutationPlan.apply(zero, LibraryChange.ToggleFavorite(a))
        assertTrue(first.accepted && first.added)
        val second = LibraryMutationPlan.apply(first.updated, LibraryChange.ToggleFavorite(b))
        assertEquals(listOf(b, a), second.updated.favorites)
        val third = LibraryMutationPlan.apply(second.updated, LibraryChange.ToggleFavorite(a))
        assertTrue(third.accepted)
        assertFalse(third.added)
        assertEquals(listOf(b), third.updated.favorites)
    }

    @Test fun renamingPlaylistIsOneLogicalChangeAndKeepsTrackMetadata() {
        val a = song(1).copy(title = "Special per-playlist title")
        val before = zero.copy(playlists = listOf("Focus" to listOf(a), "Travel" to listOf(song(2))))
        val result = LibraryMutationPlan.apply(before, LibraryChange.RenamePlaylist("Focus", "Late hours"))
        assertTrue(result.accepted)
        assertEquals(listOf("Late hours", "Travel"), result.updated.names())
        assertEquals(a, result.updated.songs("Late hours").single())
        assertEquals(listOf(a), before.songs("Focus")) // immutable old state
    }

    @Test fun caseInsensitiveDuplicateNameDoesNotMutateState() {
        val old = zero.copy(playlists = listOf("Focus" to emptyList()))
        val result = LibraryMutationPlan.apply(old, LibraryChange.CreatePlaylist("focus"))
        assertFalse(result.accepted)
        assertEquals(old, result.updated)
    }

    @Test fun removingFromPlaylistDoesNotRemoveAFavorite() {
        val a = song(1)
        val old = zero.copy(favorites = listOf(a), playlists = listOf("Focus" to listOf(a)))
        val result = LibraryMutationPlan.apply(old, LibraryChange.RemoveFromPlaylist("Focus", a.url))
        assertTrue(result.accepted)
        assertTrue(result.updated.songs("Focus").isEmpty())
        assertEquals(listOf(a), result.updated.favorites)
    }

    @Test fun duplicatesCannotBeAddedTwice() {
        val a = song(1)
        val old = zero.copy(playlists = listOf("Focus" to listOf(a)))
        val result = LibraryMutationPlan.apply(old, LibraryChange.AddToPlaylist("Focus", a))
        assertFalse(result.accepted)
        assertEquals(old, result.updated)
    }

    @Test fun playlistCapacityRejectsOverflowWithoutPartialEdit() {
        val old = zero.copy(playlists = listOf("All" to (1..300).map(::song)))
        val result = LibraryMutationPlan.apply(old, LibraryChange.AddToPlaylist("All", song(301)))
        assertFalse(result.accepted)
        assertEquals(old, result.updated)
    }

    @Test fun twentyFirstCenturyQueueHistoryIsBoundedToOneHundred() {
        var state = zero
        for (i in 1..125) {
            val change = LibraryMutationPlan.apply(state, LibraryChange.RecordRecent(song(i)))
            assertTrue(change.accepted)
            state = change.updated
        }
        assertEquals(100, state.recent.size)
        assertEquals(song(125).url, state.recent.first().url)
        assertEquals(song(26).url, state.recent.last().url)
    }

    @Test fun invalidHostNeverGetsIntoFavoritesOrPlaylist() {
        val unsafe = song(3).copy(url = "https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ")
        val first = LibraryMutationPlan.apply(zero, LibraryChange.ToggleFavorite(unsafe))
        assertFalse(first.accepted)
        val second = LibraryMutationPlan.apply(zero.copy(playlists = listOf("X" to emptyList())),
            LibraryChange.AddToPlaylist("X", unsafe))
        assertFalse(second.accepted)
    }

    @Test fun renamingToSameNameIsAcceptedNoOp() {
        val state = zero.copy(playlists = listOf("Study" to listOf(song(1))))
        val result = LibraryMutationPlan.apply(state, LibraryChange.RenamePlaylist("Study", "Study"))
        assertTrue(result.accepted)
        assertFalse(result.changed)
    }

    @Test fun deleteOnlyRemovesSelectedPlaylist() {
        val before = zero.copy(playlists = listOf("A" to listOf(song(1)), "B" to listOf(song(2))))
        val result = LibraryMutationPlan.apply(before, LibraryChange.DeletePlaylist("A"))
        assertEquals(listOf("B"), result.updated.names())
        assertEquals(listOf(song(2)), result.updated.songs("B"))
    }
}
