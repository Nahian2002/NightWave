package com.nightwave.player.auto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBrowsePolicyTest {
    @Test fun folderIdsRoundTripWithoutExposingNamesInPaths() {
        val name = "Evening 🎵 & Lo-fi"
        val id = AutoBrowsePolicy.playlistId(name)
        assertTrue(id.startsWith(AutoBrowsePolicy.PLAYLIST_PREFIX))
        assertFalse(id.contains(name))
        assertEquals(name, AutoBrowsePolicy.playlistName(id))
    }

    @Test fun invalidPlaylistFolderIdsFailClosed() {
        assertNull(AutoBrowsePolicy.playlistName(AutoBrowsePolicy.PLAYLIST_PREFIX + "../etc"))
        assertNull(AutoBrowsePolicy.playlistName(AutoBrowsePolicy.PLAYLIST_PREFIX))
        assertNull(AutoBrowsePolicy.playlistName("random"))
    }

    @Test fun publicCarIdsKeepCollectionContext() {
        val url = "https://www.youtube.com/watch?v=aaaaaaaaaaa"
        val id = AutoBrowsePolicy.publicId(AutoBrowsePolicy.FAVORITES, url)
        assertEquals(AutoBrowsePolicy.FAVORITES to url, AutoBrowsePolicy.publicTarget(id))
        assertNull(AutoBrowsePolicy.publicTarget(url))
    }

    @Test fun publicCarIdsRejectUnknownParentAndMalformedInput() {
        assertNull(AutoBrowsePolicy.publicTarget(AutoBrowsePolicy.publicId("bad", "url")))
        assertNull(AutoBrowsePolicy.publicTarget(AutoBrowsePolicy.PUBLIC_PREFIX + "garbage"))
        assertNull(AutoBrowsePolicy.publicTarget(AutoBrowsePolicy.PUBLIC_PREFIX + ":"))
    }

    @Test fun legacyOfflineSongIdsRetainTheirExtensionsAndArePathSafe() {
        val id = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa.mp3"
        assertEquals(id, AutoBrowsePolicy.offlineTarget(AutoBrowsePolicy.offlineId(id)))
        assertNull(AutoBrowsePolicy.offlineTarget(
            AutoBrowsePolicy.offlineId("../etc/passwd.mp3")))
        assertNull(AutoBrowsePolicy.offlineTarget(AutoBrowsePolicy.offlineId("missing")))
    }

    @Test fun decodeRejectsInvalidUtf8AndOversizedTokens() {
        assertNull(AutoBrowsePolicy.decode("_w"))
        assertNull(AutoBrowsePolicy.decode("a".repeat(1001)))
        assertNull(AutoBrowsePolicy.decode("abc+"))
    }

    @Test fun pageIsZeroBasedBoundedAndDoesNotModifyItsInput() {
        val items = (0 until 225).toList()
        val before = items.toList()
        assertEquals((0 until 100).toList(), AutoBrowsePolicy.page(items, 0, 100))
        assertEquals((100 until 200).toList(), AutoBrowsePolicy.page(items, 1, 100))
        assertEquals((200 until 225).toList(), AutoBrowsePolicy.page(items, 2, 100))
        assertEquals(before, items)
    }

    @Test fun invalidPageSizesAndIntegerOverflowReturnEmpty() {
        val list = listOf(1, 2, 3)
        assertTrue(AutoBrowsePolicy.page(list, -1, 2).isEmpty())
        assertTrue(AutoBrowsePolicy.page(list, 0, 0).isEmpty())
        assertTrue(AutoBrowsePolicy.page(list, 0, 101).isEmpty())
        assertTrue(AutoBrowsePolicy.page(list, Int.MAX_VALUE, Int.MAX_VALUE).isEmpty())
    }

    @Test fun rootCollectionsAreStableAndDistinct() {
        assertEquals(4, listOf(AutoBrowsePolicy.IMPORTED,
            AutoBrowsePolicy.FAVORITES, AutoBrowsePolicy.RECENT,
            AutoBrowsePolicy.PLAYLISTS).distinct().size)
        assertTrue(AutoBrowsePolicy.ROOT != AutoBrowsePolicy.IMPORTED)
    }
}
