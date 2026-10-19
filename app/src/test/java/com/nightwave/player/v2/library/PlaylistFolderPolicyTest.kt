package com.nightwave.player.v2.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistFolderPolicyTest {
    @Test fun rejectsInvalidOrDuplicateNamesWithoutMutation() {
        val start = PlaylistFolderSnapshot()
        val first = PlaylistFolderPolicy.add(start, "Late Night")!!
        assertNull(PlaylistFolderPolicy.add(first, "late night"))
        assertNull(PlaylistFolderPolicy.add(first, "../unsafe"))
        assertNull(PlaylistFolderPolicy.add(first, " "))
        assertNull(PlaylistFolderPolicy.add(first, "a".repeat(61)))
        assertTrue(start.folders.isEmpty())
    }

    @Test fun createRenameAndDeleteAreOnlyMetadataEdits() {
        val start = PlaylistFolderPolicy.add(PlaylistFolderSnapshot(), "Favorites")!!
        val moved = PlaylistFolderPolicy.assign(start, "Indie",
            "Favorites", listOf("Indie", "Workout"))!!
        assertEquals("Favorites", moved.folderOf("Indie"))
        val renamed = PlaylistFolderPolicy.rename(moved, "Favorites", "Home")!!
        assertEquals("Home", renamed.folderOf("Indie"))
        val deleted = PlaylistFolderPolicy.delete(renamed, "Home")!!
        assertNull(deleted.folderOf("Indie"))
        assertTrue(deleted.folders.isEmpty())
        assertEquals("Favorites", moved.folderOf("Indie"))
    }

    @Test fun moveRejectsUnknownPlaylistsAndFolders() {
        val snap = PlaylistFolderPolicy.add(PlaylistFolderSnapshot(), "Home")!!
        assertNull(PlaylistFolderPolicy.assign(snap, "unknown", "Home", listOf("Real")))
        assertNull(PlaylistFolderPolicy.assign(snap, "Real", "Other", listOf("Real")))
        assertTrue(snap.placements.isEmpty())
    }

    @Test fun renamePlaylistKeepsItsPlacement() {
        val folder = PlaylistFolderPolicy.add(PlaylistFolderSnapshot(), "Home")!!
        val moved = PlaylistFolderPolicy.assign(folder,
            "Old playlist", "Home", listOf("Old playlist"))!!
        val renamed = PlaylistFolderPolicy.playlistRenamed(moved,
            "Old playlist", "New playlist")
        assertNull(renamed.folderOf("Old playlist"))
        assertEquals("Home", renamed.folderOf("New playlist"))
        val deleted = PlaylistFolderPolicy.playlistDeleted(renamed, "New playlist")
        assertTrue(deleted.placements.isEmpty())
    }

    @Test fun reassignAndUnfileAreReversible() {
        val a = PlaylistFolderPolicy.add(PlaylistFolderSnapshot(), "A")!!
        val b = PlaylistFolderPolicy.add(a, "B")!!
        val x = PlaylistFolderPolicy.assign(b, "Playlist", "A", listOf("Playlist"))!!
        val y = PlaylistFolderPolicy.assign(x, "Playlist", "B", listOf("Playlist"))!!
        val z = PlaylistFolderPolicy.assign(y, "Playlist", null, listOf("Playlist"))!!
        assertEquals("A", x.folderOf("Playlist"))
        assertEquals("B", y.folderOf("Playlist"))
        assertNull(z.folderOf("Playlist"))
        assertEquals(listOf("Playlist"), y.playlists("B", listOf("Playlist", "Other")))
    }

    @Test fun folderLimitIsEnforcedWithoutDroppingAnyFolder() {
        var layout = PlaylistFolderSnapshot()
        repeat(PlaylistFolderPolicy.MAX_FOLDERS) {
            layout = PlaylistFolderPolicy.add(layout, "Folder $it")!!
        }
        assertNull(PlaylistFolderPolicy.add(layout, "One more"))
        assertEquals(PlaylistFolderPolicy.MAX_FOLDERS, layout.folders.size)
        assertFalse(PlaylistFolderPolicy.validName(" A "))
    }
}
