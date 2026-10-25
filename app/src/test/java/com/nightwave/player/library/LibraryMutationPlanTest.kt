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

    @Test fun recentHistoryBoundaryHoldsExactlyOneHundredEntries() {
        var state = zero
        for (i in 1..99) {
            state = LibraryMutationPlan.apply(state, LibraryChange.RecordRecent(song(i))).updated
        }
        assertEquals(99, state.recent.size)

        state = LibraryMutationPlan.apply(state, LibraryChange.RecordRecent(song(100))).updated
        assertEquals(100, state.recent.size)
        assertEquals(song(1).url, state.recent.last().url)

        state = LibraryMutationPlan.apply(state, LibraryChange.RecordRecent(song(101))).updated
        assertEquals(100, state.recent.size)
        assertEquals(song(2).url, state.recent.last().url)
    }

    @Test fun replayingRecentTrackMovesItToFrontWithoutDuplication() {
        var state = zero
        for (i in 1..100) {
            state = LibraryMutationPlan.apply(state, LibraryChange.RecordRecent(song(i))).updated
        }
        state = LibraryMutationPlan.apply(state, LibraryChange.RecordRecent(song(50))).updated
        assertEquals(100, state.recent.size)
        assertEquals(song(50).url, state.recent.first().url)
        assertEquals(100, state.recent.map { it.url }.distinct().size)
        assertEquals(song(1).url, state.recent.last().url)
    }

    @Test fun removeRecentlyPlayedIsLimitedToHistoryAndPreservesEverythingElse() {
        val a = song(1); val b = song(2)
        val before = zero.copy(favorites = listOf(a),
            recent = listOf(b, a), playlists = listOf("Focus" to listOf(a)))
        val result = LibraryMutationPlan.apply(before, LibraryChange.RemoveRecent(a.url))
        assertTrue(result.accepted && result.changed)
        assertEquals(listOf(b), result.updated.recent)
        assertEquals(listOf(a), result.updated.favorites)
        assertEquals(listOf(a), result.updated.songs("Focus"))
        assertEquals(listOf(b, a), before.recent)
    }

    @Test fun clearRecentIsHistoryOnlyAndRejectsEmptyDoubleClear() {
        val a = song(3)
        val before = zero.copy(favorites = listOf(a),
            recent = listOf(a), playlists = listOf("A" to listOf(a)))
        val first = LibraryMutationPlan.apply(before, LibraryChange.ClearRecent)
        assertTrue(first.accepted && first.changed)
        assertTrue(first.updated.recent.isEmpty())
        assertEquals(before.favorites, first.updated.favorites)
        assertEquals(before.playlists, first.updated.playlists)
        val again = LibraryMutationPlan.apply(first.updated, LibraryChange.ClearRecent)
        assertFalse(again.accepted)
        assertFalse(again.changed)
    }

    @Test fun invalidOrMissingRecentTrackNeverModifiesHistory() {
        val a = song(1)
        val before = zero.copy(recent = listOf(a))
        assertFalse(LibraryMutationPlan.apply(before,
            LibraryChange.RemoveRecent("https://evil.example/song")).accepted)
        assertFalse(LibraryMutationPlan.apply(before,
            LibraryChange.RemoveRecent(song(4).url)).accepted)
        assertEquals(listOf(a), before.recent)
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

    @Test fun bulkRemovalIsAtomicAndKeepsAllOtherCollections() {
        val a = song(1); val b = song(2); val c = song(3)
        val before = zero.copy(favorites = listOf(b), recent = listOf(a),
            playlists = listOf("Focus" to listOf(a, b, c), "Other" to listOf(c)))
        val good = LibraryMutationPlan.apply(before, LibraryChange.BulkRemoveFromPlaylist(
            "Focus", listOf(a.url, c.url), listOf(a.url, b.url, c.url)))
        assertTrue(good.accepted && good.changed)
        assertEquals(listOf(b), good.updated.songs("Focus"))
        assertEquals(listOf(c), good.updated.songs("Other"))
        assertEquals(before.favorites, good.updated.favorites)
        assertEquals(before.recent, good.updated.recent)
        assertEquals(listOf(a,b,c), before.songs("Focus"))
        val stale = LibraryMutationPlan.apply(good.updated, LibraryChange.BulkRemoveFromPlaylist(
            "Focus", listOf(b.url), listOf(a.url,b.url,c.url)))
        assertFalse(stale.accepted)
        assertEquals(good.updated, stale.updated)
    }

    @Test fun bulkInvalidOrOversizedSelectionsChangeNothing() {
        val a = song(1); val b = song(2)
        val before = zero.copy(playlists = listOf("Focus" to listOf(a,b)))
        for (invalid in listOf(emptyList(), listOf(a.url,a.url),
            listOf(a.url, song(4).url), (1..21).map { song(it).url })) {
            val result = LibraryMutationPlan.apply(before,
                LibraryChange.BulkRemoveFromPlaylist("Focus", invalid, listOf(a.url,b.url)))
            assertFalse(result.accepted)
            assertEquals(before, result.updated)
        }
    }

    @Test fun bulkCopySkipsExistingTracksButRejectsUnsafeAndOverflow() {
        val a = song(1); val b = song(2); val c = song(3)
        val before = zero.copy(playlists = listOf("Source" to listOf(a,b,c),
            "Target" to listOf(a)))
        val good = LibraryMutationPlan.apply(before,
            LibraryChange.BulkAddToPlaylist("Target", listOf(a,b,c), listOf(a.url)))
        assertTrue(good.accepted)
        assertEquals(listOf(a,b,c), good.updated.songs("Target"))
        assertEquals(listOf(a,b,c), good.updated.songs("Source"))
        val bad = LibraryMutationPlan.apply(before,
            LibraryChange.BulkAddToPlaylist("Target", listOf(b,
                b.copy(url = "file:///private/music.mp3")), listOf(a.url)))
        assertFalse(bad.accepted)
        val stale = LibraryMutationPlan.apply(good.updated,
            LibraryChange.BulkAddToPlaylist("Target", listOf(song(4)), listOf(a.url)))
        assertFalse(stale.accepted)
        val full = zero.copy(playlists = listOf("Target" to (1..300).map(::song)))
        assertFalse(LibraryMutationPlan.apply(full, LibraryChange.BulkAddToPlaylist(
            "Target", listOf(song(301)), full.songs("Target").map { it.url })).accepted)
    }

    @Test fun reorderOnlyAllowsExactPermutationAndStaleSourceIsRejected() {
        val a = song(1).copy(title="Custom title"); val b = song(2); val c = song(3)
        val before = zero.copy(playlists = listOf("Focus" to listOf(a,b,c)))
        val accepted = LibraryMutationPlan.apply(before, LibraryChange.ReorderPlaylist(
            "Focus", listOf(a.url,b.url,c.url), listOf(c.url,a.url,b.url)))
        assertTrue(accepted.accepted)
        assertEquals(listOf(c,a,b), accepted.updated.songs("Focus"))
        for (invalid in listOf(listOf(a.url,a.url,b.url),
            listOf(c.url,b.url), listOf(c.url, song(8).url, a.url))) {
            assertFalse(LibraryMutationPlan.apply(before, LibraryChange.ReorderPlaylist(
                "Focus", listOf(a.url,b.url,c.url), invalid)).accepted)
        }
        assertFalse(LibraryMutationPlan.apply(accepted.updated, LibraryChange.ReorderPlaylist(
            "Focus", listOf(a.url,b.url,c.url), listOf(b.url,a.url,c.url))).accepted)
    }

    @Test fun conditionalUndoRestoresOnlyUnchangedPlaylistAndMetadata() {
        val a = song(1).copy(title="Old title"); val b = song(2)
        val before = zero.copy(favorites = listOf(a),
            playlists = listOf("Focus" to listOf(a,b), "Other" to listOf(b)))
        val after = LibraryMutationPlan.apply(before, LibraryChange.BulkRemoveFromPlaylist(
            "Focus", listOf(a.url), listOf(a.url,b.url))).updated
        val undo = LibraryMutationPlan.apply(after, LibraryChange.RestorePlaylistIfUnchanged(
            "Focus", listOf(b), listOf(a,b)))
        assertTrue(undo.accepted)
        assertEquals(before, undo.updated)
        val edited = LibraryMutationPlan.apply(after, LibraryChange.AddToPlaylist("Focus", song(3))).updated
        val blocked = LibraryMutationPlan.apply(edited,
            LibraryChange.RestorePlaylistIfUnchanged("Focus", listOf(b), listOf(a,b)))
        assertFalse(blocked.accepted)
        assertEquals(edited, blocked.updated)
        assertFalse(LibraryMutationPlan.apply(after,
            LibraryChange.RestorePlaylistIfUnchanged("Focus", listOf(b),
                listOf(a.copy(url="https://youtube.com.evil.test/watch?v=123"),b))).accepted)
    }
}
