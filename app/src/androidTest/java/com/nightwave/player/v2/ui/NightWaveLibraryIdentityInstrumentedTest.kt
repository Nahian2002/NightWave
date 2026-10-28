package com.nightwave.player.v2.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.library.*
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.library.*
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Library screen; disposable in-memory policies only, no real app data/player/picker/account. */
@RunWith(AndroidJUnit4::class)
class NightWaveLibraryIdentityInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private var data by mutableStateOf(LibraryCollections(emptyList(), emptyList(), emptyList()))
    private var revision by mutableIntStateOf(0)
    private var selection by mutableStateOf<String?>(null)
    private var undo by mutableStateOf<PlaylistUndoState?>(null)
    private var action = ""
    private var played = false
    private fun song(n: Int) = VideoPreview("Song $n", "Fixture uploader",
        "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'), "", 180L)
    private fun screen() {
        compose.setContent {
            NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                StudioLibrary(Modifier, data.favorites, data.recent, data.names(), revision, false,
                    SmartLibraryPolicy.generate(data.favorites, data.recent, data.playlists.map { it.second }),
                    PlaylistFolderSnapshot(), emptyList(), onTogglePinPlaylist = { action = "pin:$it" },
                    playingUrl = "", selectedPlaylist = selection, onSelectPlaylist = { selection = it },
                    getPlaylist = { data.songs(it) }, onPlay = { _, _ -> played = true }, onQuickPlay = { played = true },
                    onManage = { action = "manage" }, onSearch = { action = "search" },
                    onCreatePlaylist = { action = "create" }, onManageFolders = { action = "folders" },
                    onMovePlaylist = { action = "move:$it" }, onOffline = { action = "offline" },
                    offlineResumeTitle = null, offlineResumePositionMs = 0L, onResumeOffline = {},
                    onSongOptions = {}, onRemoveRecent = {}, onClearRecent = {}, onPlaylistSongOptions = { _, _ -> },
                    onSharePlaylist = { name, _ -> action = "share:$name" },
                    onExportPlaylist = { name, songs -> assertEquals(data.songs(name), songs); action = "export:$name" },
                    onBulkShareLinks = { _, _ -> }, onBulkRemove = { name, songs ->
                        val before = data.songs(name)
                        val result = LibraryMutationPlan.apply(data, LibraryChange.BulkRemoveFromPlaylist(
                            name, songs.map { it.url }, before.map { it.url }))
                        assertTrue(result.accepted)
                        data = result.updated
                        undo = PlaylistUndoState(name, before, data.songs(name), "Fixture removal")
                        revision++
                    }, onBulkCopy = { _, _ -> }, onMovePlaylistSong = { _, _, _, _ -> },
                    playlistUndo = undo, onUndoPlaylistEdit = {
                        val pending = requireNotNull(undo)
                        val result = LibraryMutationPlan.apply(data, LibraryChange.RestorePlaylistIfUnchanged(
                            pending.playlist, pending.after, pending.before))
                        assertTrue(result.accepted)
                        data = result.updated; undo = null; revision++
                    }, onDismissPlaylistUndo = { undo = null }, onImportSharedPlaylist = { action = "import" })
            }
        }
    }
    private fun scroll(matcher: SemanticsMatcher) = compose.onNodeWithTag("nw132-library-list").performScrollToNode(matcher)

    @Test fun historyOnlyOverviewHasCreateAndHistoryWithoutEmptyLikedDashboard() {
        data = data.copy(recent = listOf(song(1), song(1), song(2)))
        screen()
        compose.onNodeWithTag("nw47-library-new-playlist").performClick()
        compose.runOnIdle { assertEquals("create", action); assertFalse(played) }
        compose.onNodeWithText("Recently liked").assertDoesNotExist()
        compose.onNodeWithText("Nothing here yet").assertDoesNotExist()
        scroll(hasText("2 songs · 3 history entries"))
        compose.onNodeWithText("2 songs · 3 history entries").assertExists()
    }
    @Test fun automaticHistoryCollectionNeverUsesSavedTracksHeading() {
        data = data.copy(recent = listOf(song(1), song(2)))
        selection = SmartLibraryPolicy.PREFIX + "recent"
        screen()
        scroll(hasText("Collection songs"))
        compose.onNodeWithText("Collection songs").assertExists()
        compose.onNodeWithText("Saved tracks").assertDoesNotExist()
        compose.onNodeWithTag("nw132-playlist-options").assertDoesNotExist()
    }
    @Test fun libraryOptionsDispatchImportWithoutPlayingOrEditing() {
        screen()
        compose.onNodeWithTag("nw132-library-options").performClick()
        compose.onNodeWithText("Import playlist (.txt or pasted links)").performClick()
        compose.runOnIdle { assertEquals("import", action); assertTrue(data.playlists.isEmpty()); assertFalse(played) }
    }
    @Test fun playlistOptionsRetainExportOfExactCurrentCollection() {
        data = data.copy(playlists = listOf("Night" to listOf(song(1), song(2))))
        selection = "Night"
        screen()
        scroll(hasTestTag("nw132-playlist-options"))
        compose.onNodeWithTag("nw132-playlist-options").performClick()
        compose.onNodeWithText("Export playlist .txt").performClick()
        compose.runOnIdle { assertEquals("export:Night", action); assertFalse(played) }
    }
    @Test fun filterHiddenSelectionRemovesBothChosenSongsAndUndoRestoresExactPlaylist() {
        val before = data.copy(favorites = listOf(song(1)), recent = listOf(song(2)),
            playlists = listOf("Night" to (1..3).map(::song), "Other" to listOf(song(9))))
        data = before; selection = "Night"
        screen()
        scroll(hasText("Select songs")); compose.onNodeWithText("Select songs").performClick()
        scroll(hasContentDescription("Select Song 1")); compose.onNodeWithContentDescription("Select Song 1").performClick()
        scroll(hasTestTag("nw132-track-filter")); compose.onNodeWithTag("nw132-track-filter").performTextInput("Song 3")
        scroll(hasContentDescription("Select Song 3")); compose.onNodeWithContentDescription("Select Song 3").performClick()
        scroll(hasText("Remove")); compose.onNodeWithText("Remove").performClick()
        compose.runOnIdle { assertEquals(listOf(song(2)), data.songs("Night")); assertFalse(played) }
        scroll(hasText("Undo")); compose.onNodeWithText("Undo").performClick()
        compose.runOnIdle { assertEquals(before, data) }
    }
}
