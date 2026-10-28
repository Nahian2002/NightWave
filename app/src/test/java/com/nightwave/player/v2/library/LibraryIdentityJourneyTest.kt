package com.nightwave.player.v2.library

import com.nightwave.player.library.*
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.social.PlaylistDocumentPolicy
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

/** Synthetic metadata only; runs the real policies/commands without preferences, providers or playback. */
class LibraryIdentityJourneyTest {
    private fun song(n: Int) = VideoPreview("Song $n", "Uploader $n",
        "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'), "", 180L)
    private val empty = LibraryCollections(emptyList(), emptyList(), emptyList())
    private fun apply(before: LibraryCollections, change: LibraryChange): LibraryCollections {
        val result = LibraryMutationPlan.apply(before, change)
        assertTrue(result.accepted)
        return result.updated
    }
    @Test fun newLibraryDoesNotRenderAnEmptyLikedDashboard() {
        val state = LibraryCollectionPresentationPolicy.overview(emptyList(), emptyList(), emptyList())
        assertFalse(state.showRecentlyLiked)
        assertTrue(state.showSaveHint)
        assertEquals(0, state.savedSongs)
    }
    @Test fun historyOnlyCountsEntriesAndSongsSeparatelyWithoutSavedMembership() {
        val history = listOf(song(1), song(1), song(2))
        val state = LibraryCollectionPresentationPolicy.overview(emptyList(), history, emptyList())
        assertEquals(3, state.historyEntries)
        assertEquals(2, state.historySongs)
        assertEquals(0, state.savedSongs)
        assertEquals("0 liked songs · 0 playlists", state.summary)
        assertTrue(state.showSaveHint)
        assertFalse(state.showRecentlyLiked)
    }
    @Test fun invalidAndDuplicateLikedRowsCannotInflateDisplayCountsOrRewriteMetadata() {
        val first = song(1)
        val source = listOf(first, first.copy(title = "Older metadata"), song(2).copy(url = "offline:not-a-public-reference"))
        val state = LibraryCollectionPresentationPolicy.overview(source, emptyList(), emptyList())
        val shown = LibraryCollectionPresentationPolicy.displaySongs(source)
        assertEquals(1, state.likedCount)
        assertEquals(listOf(first), shown)
        assertSame(first, shown.single())
        assertEquals(3, source.size)
    }
    @Test fun savedMembershipIsUniqueAcrossLikesAndPlaylistsAndExcludesHistoryOnlySongs() {
        val state = LibraryCollectionPresentationPolicy.overview(listOf(song(1)), listOf(song(4)), listOf(
            LibraryPlaylistPreview("First", listOf(song(1), song(2))),
            LibraryPlaylistPreview("Second", listOf(song(2), song(3))), LibraryPlaylistPreview("Empty", emptyList())))
        assertEquals(3, state.savedSongs)
        assertEquals(3, state.playlistCount)
        assertFalse(state.showSaveHint)
    }
    @Test fun automaticCollectionsExplainOverlappingRealMembershipWithoutClaimingSaved() {
        val tracks = listOf(song(1), song(2), song(2), song(3))
        val detail = LibraryCollectionPresentationPolicy.automaticDetail(tracks, listOf(song(1)),
            listOf(song(1), song(3)), listOf(LibraryPlaylistPreview("Night", listOf(song(2)))))
        assertEquals("3 songs · Automatic collection · 1 liked · 1 in playlists · 2 in history", detail)
        val historyOnly = LibraryCollectionPresentationPolicy.automaticDetail(tracks, emptyList(), tracks, emptyList())
        assertEquals("3 songs · Automatic collection · 3 in history", historyOnly)
        assertFalse(historyOnly.contains("saved", ignoreCase = true))
    }
    @Test fun eachCollectionSectionStatesItsActualOrigin() {
        assertEquals("Recently liked", LibraryCollectionPresentationPolicy.songSection(true, false, false, false))
        assertEquals("Listening history", LibraryCollectionPresentationPolicy.songSection(false, true, false, false))
        assertEquals("Collection songs", LibraryCollectionPresentationPolicy.songSection(false, false, true, false))
        assertEquals("Liked songs", LibraryCollectionPresentationPolicy.songSection(false, false, false, true))
        assertEquals("Playlist songs", LibraryCollectionPresentationPolicy.songSection(false, false, false, false))
    }
    @Test fun preparedAutomaticDetailsMatchIndividualEvidenceAndRefreshWithMembership() {
        val collections = listOf(SmartLibraryCollection("fixture", "Fixture", "", listOf(song(1), song(2))))
        val lists = listOf(LibraryPlaylistPreview("Night", listOf(song(2))))
        val prepared = LibraryCollectionPresentationPolicy.automaticDetails(collections, listOf(song(1)), emptyList(), lists)
        assertEquals(LibraryCollectionPresentationPolicy.automaticDetail(collections.single().songs,
            listOf(song(1)), emptyList(), lists), prepared["fixture"])
        val changed = LibraryCollectionPresentationPolicy.automaticDetails(collections, emptyList(), listOf(song(1)), lists)
        assertEquals("2 songs · Automatic collection · 1 in playlists · 1 in history", changed["fixture"])
        assertTrue(prepared.getValue("fixture").contains("1 liked"))
    }
    @Test fun invalidSourceRowsNeverJoinBulkCommandsEvenWhenRestoredOrVisible() {
        val valid = song(1)
        val invalid = song(2).copy(url = "https://www.youtube.com/watch?v=00000000002&token=fixture")
        val source = listOf(valid, invalid)
        val restored = source.map { it.url }.toSet()
        assertEquals(setOf(valid.url), LibraryBulkSelectionPolicy.sanitize(restored, source))
        assertEquals(setOf(valid.url), LibraryBulkSelectionPolicy.selectVisible(source))
        assertEquals(listOf(valid), LibraryBulkSelectionPolicy.selected(source, restored))
        assertTrue(LibraryBulkSelectionPolicy.toggle(emptySet(), invalid.url, source).isEmpty())
        assertEquals(setOf(valid.url), LibraryBulkSelectionPolicy.toggleVisible(emptySet(), source, source))
        assertEquals("Deselect visible", LibraryBulkSelectionPolicy.visibleActionLabel(setOf(valid.url), source))
        assertTrue(LibraryBulkSelectionPolicy.toggleVisible(setOf(valid.url), source, source).isEmpty())
    }
    @Test fun playlistContentsFilterFindsTheLastSupportedSongWithoutChangingItsOrder() {
        val songs = (1..300).map(::song).toMutableList()
        songs[299] = songs.last().copy(title = "Boundary moonlight")
        val playlist = LibraryPlaylistPreview("Evening", songs)
        assertEquals(listOf(playlist), LibraryBrowsePolicy.playlists(listOf(playlist), "boundary moonlight",
            LibraryBrowsePolicy.SORT_SAVED, false, matchContainedSongs = true))
        assertEquals(300, songs.size)
        assertEquals("Song 1", songs.first().title)
        assertSame(songs, playlist.songs)
    }
    @Test fun containedSongFilterRemainsBoundedAtTheActualWriterCapacity() {
        val unsupported = LibraryPlaylistPreview("Oversized fixture", (1..301).map(::song).mapIndexed { i, s ->
            if (i == 300) s.copy(title = "Outside capacity needle") else s
        })
        assertTrue(LibraryBrowsePolicy.playlists(listOf(unsupported), "capacity needle", "saved", false, true).isEmpty())
        assertEquals(300, LibraryBrowsePolicy.MAX_PLAYLIST_SONGS)
    }
    @Test fun createRenameFolderPinSortAndDocumentImportPreserveOriginalCollections() {
        val a = song(1).copy(title = "রাতের গান", author = "Fixture uploader")
        val baseline = empty.copy(favorites = listOf(a), recent = listOf(song(2)), playlists = listOf("Other" to listOf(song(3))))
        var current = apply(baseline, LibraryChange.CreatePlaylist("Evening"))
        current = apply(current, LibraryChange.AddToPlaylist("Evening", a))
        current = apply(current, LibraryChange.RenamePlaylist("Evening", "Night"))
        val folder = PlaylistFolderPolicy.assign(PlaylistFolderSnapshot(listOf("Focus")), "Night", "Focus", current.names())!!
        assertEquals("Focus", folder.folderOf("Night"))
        val previews = current.playlists.map { LibraryPlaylistPreview(it.first, it.second) }
        assertEquals("Night", PlaylistPinPolicy.pinnedFirst(previews, listOf("Night")) { it.name }.first().name)
        assertEquals(listOf("Night", "Other"), LibraryBrowsePolicy.playlists(previews, "", "name", false).map { it.name })
        val exported = PlaylistDocumentPolicy.prepare("Night", current.songs("Night"))!!
        val parsed = PlaylistDocumentPolicy.read(ByteArrayInputStream(exported.bytes))!!
        current = apply(current, LibraryChange.ImportSharedPlaylist(parsed.name, parsed.tracks))
        assertEquals(listOf("Other", "Night"), current.names()) // Repeated exact import is a no-op.
        assertEquals(baseline.favorites, current.favorites)
        assertEquals(baseline.recent, current.recent)
        assertEquals(baseline.songs("Other"), current.songs("Other"))
        assertEquals(listOf("Other"), baseline.names())
    }
    @Test fun filterHiddenSelectionRemovesAndUndoesOnlyTheExactTargetPlaylist() {
        val songs = (1..4).map(::song)
        val before = empty.copy(favorites = listOf(songs[0]), recent = listOf(songs[1]),
            playlists = listOf("Night" to songs, "Other" to listOf(song(9))))
        var selected = LibraryBulkSelectionPolicy.toggleVisible(emptySet(), listOf(songs[0]), songs)
        selected = LibraryBulkSelectionPolicy.toggleVisible(selected, listOf(songs[2]), songs)
        assertEquals(1, LibraryBulkSelectionPolicy.hiddenSelectionCount(selected, listOf(songs[2])))
        val selectedSongs = LibraryBulkSelectionPolicy.selected(songs, selected)
        val after = apply(before, LibraryChange.BulkRemoveFromPlaylist("Night", selectedSongs.map { it.url }, songs.map { it.url }))
        assertEquals(listOf(songs[1], songs[3]), after.songs("Night"))
        assertEquals(before, apply(after, LibraryChange.RestorePlaylistIfUnchanged("Night", after.songs("Night"), songs)))
        assertEquals(before.favorites, after.favorites)
        assertEquals(before.recent, after.recent)
        assertEquals(before.songs("Other"), after.songs("Other"))
    }
    @Test fun staleRemoveReorderAndUndoFailClosedAfterAnotherEdit() {
        val songs = (1..3).map(::song)
        val before = empty.copy(playlists = listOf("Night" to songs))
        val after = apply(before, LibraryChange.RemoveFromPlaylist("Night", songs[0].url))
        for (change in listOf(
            LibraryChange.BulkRemoveFromPlaylist("Night", listOf(songs[1].url), songs.map { it.url }),
            LibraryChange.ReorderPlaylist("Night", songs.map { it.url }, songs.reversed().map { it.url }),
            LibraryChange.RestorePlaylistIfUnchanged("Night", songs, songs.reversed()))) {
            val rejected = LibraryMutationPlan.apply(after, change)
            assertFalse(rejected.accepted)
            assertEquals(after, rejected.updated)
        }
    }
    @Test fun conflictingDocumentNameCreatesANewPlaylistAndMalformedTextNeverMutates() {
        val before = empty.copy(playlists = listOf("Night" to listOf(song(1))))
        val prepared = PlaylistDocumentPolicy.prepare("Night", listOf(song(2)))!!
        val incoming = PlaylistDocumentPolicy.read(ByteArrayInputStream(prepared.bytes))!!
        val after = apply(before, LibraryChange.ImportSharedPlaylist(incoming.name, incoming.tracks))
        assertEquals(listOf("Night", "Night (Shared)"), after.names())
        assertEquals(before.songs("Night"), after.songs("Night"))
        assertNull(PlaylistDocumentPolicy.read(ByteArrayInputStream("untrusted incomplete text".toByteArray())))
        assertEquals(listOf("Night"), before.names())
    }
}
