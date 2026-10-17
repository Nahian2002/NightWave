package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedCreatorBrowserTest {
    private fun video(title: String, author: String, id: String) =
        VideoPreview(title, author, "https://www.youtube.com/watch?v=" + id, "", 180L)

    @Test fun emptyLibraryHasNoInventedArtistProfiles() {
        assertTrue(SavedCreatorBrowser.groups(emptyList()).isEmpty())
    }

    @Test fun videoUploaderIsShownAsCreatorNotVerifiedArtist() {
        val original = video("A song", "Community Publisher", "abc12345678")
        val group = SavedCreatorBrowser.groups(listOf(original)).single()
        assertEquals("Community Publisher", group.name)
        assertEquals(listOf(original), group.tracks)
    }

    @Test fun conservativeDisplayIdentityJoinsCreditedVevoUploads() {
        val first = video("Ellie Goulding - Lights (Official Video)",
            "EllieGouldingVEVO", "abc12345678")
        val second = video("Ellie Goulding - Starry Eyed (Official Audio)",
            "Ellie Goulding - Topic", "abc12345679")
        val groups = SavedCreatorBrowser.groups(listOf(first, second))
        assertEquals(1, groups.size)
        assertEquals(2, groups.single().trackCount)
        assertEquals(listOf(first, second), groups.single().tracks)
    }

    @Test fun uncorroboratedTitleDoesNotCreateFakeArtist() {
        val original = video("Famous Artist - Song", "Third Party Channel", "abc12345678")
        assertEquals("Third Party Channel",
            SavedCreatorBrowser.groups(listOf(original)).single().name)
    }

    @Test fun duplicatesAreCountedOnceWithoutChangingIdentity() {
        val original = video("Track", "Creator One", "abc12345678")
        val copy = original.copy(title = "Changed metadata")
        val result = SavedCreatorBrowser.groups(listOf(original, copy)).single()
        assertEquals(1, result.trackCount)
        assertEquals(listOf(original), result.tracks)
    }

    @Test fun genericUnverifiedCreatorsAreNotPromoted() {
        val videos = listOf(
            video("A", "YouTube creator", "abc12345678"),
            video("B", "Unknown creator", "abc12345679"),
            video("C", "Various artists", "abc12345670"),
            video("D", "Unknown artist", "abc12345671"))
        assertTrue(SavedCreatorBrowser.groups(videos).isEmpty())
    }

    @Test fun offlineFilesAndUnsupportedLinksCannotBecomeCreatorEntries() {
        val local = VideoPreview("Private MP3", "Creator", "offline:abc", "", 200)
        val other = VideoPreview("Bad source", "Creator", "https://example.com/song", "", 200)
        assertTrue(SavedCreatorBrowser.groups(listOf(local, other)).isEmpty())
    }

    @Test fun nonCanonicalWatchUrlsAreExcludedRatherThanRewritten() {
        val original = video("Saved", "Creator", "abc12345678")
        val modified = original.copy(url = original.url + "&t=16")
        assertTrue(SavedCreatorBrowser.groups(listOf(modified)).isEmpty())
    }

    @Test fun fakeYouTubeHostNeverEntersCreatorQueue() {
        val spoofed = VideoPreview("Saved", "Creator",
            "https://www.youtube.com.example.net/watch?v=abc12345678", "", 200)
        assertTrue(SavedCreatorBrowser.groups(listOf(spoofed)).isEmpty())
    }

    @Test fun accentAndSpacingNormalizeCreatorKeys() {
        val a = video("First", "Beyoncé", "abc12345678")
        val b = video("Second", "Beyonce", "abc12345679")
        assertEquals(2, SavedCreatorBrowser.groups(listOf(a, b)).single().trackCount)
    }

    @Test fun countSortsBeforeAlphabetAndTiesAlphabetically() {
        val entries = listOf(
            video("1", "Zebra", "abc12345678"),
            video("2", "Alpha", "abc12345679"),
            video("3", "Zebra", "abc12345670"),
            video("4", "Beta", "abc12345671"))
        assertEquals(listOf("Zebra", "Alpha", "Beta"),
            SavedCreatorBrowser.groups(entries).map { it.name })
    }

    @Test fun tracksKeepOriginalSavedOrderWithinEachCreator() {
        val third = video("C", "Creator", "abc12345670")
        val first = video("A", "Creator", "abc12345678")
        val second = video("B", "Creator", "abc12345679")
        assertEquals(listOf(third, first, second),
            SavedCreatorBrowser.groups(listOf(third, first, second)).single().tracks)
    }

    @Test fun sourceScanIsBoundedTo350() {
        val many = (0 until 351).map { i ->
            video("Music $i", "Creator $i", "v" + i.toString().padStart(10, '0'))
        }
        val groups = SavedCreatorBrowser.groups(many)
        assertEquals(48, groups.size)
        assertFalse(groups.any { it.name == "Creator 350" })
    }

    @Test fun creatorShelfIsBoundedTo48() {
        val tracks = (0 until 70).map { i ->
            video("Music $i", "Creator $i", "v" + i.toString().padStart(10, '0'))
        }
        assertEquals(SavedCreatorBrowser.MAX_GROUPS,
            SavedCreatorBrowser.groups(tracks).size)
    }

    @Test fun groupPublicQueueIsBoundedTo100() {
        val music = (0 until 125).map { i ->
            video("Saved $i", "Creator Group", "v" + i.toString().padStart(10, '0'))
        }
        val group = SavedCreatorBrowser.groups(music).single()
        assertEquals(125, group.trackCount)
        assertEquals(100, group.tracks.size)
        assertTrue(group.hasMore)
    }

    @Test fun doesNotInventUnseenTrackIdsWhenListTruncated() {
        val all = (0 until 150).map { i ->
            video("Saved $i", "Creator Group", "v" + i.toString().padStart(10, '0'))
        }
        val group = SavedCreatorBrowser.groups(all).single()
        assertEquals(all.take(100), group.tracks)
    }

    @Test fun creatorWithNoPrivateMetadataIsNotInvented() {
        val original = video("A", " ", "abc12345678")
        assertTrue(SavedCreatorBrowser.groups(listOf(original)).isEmpty())
    }

    @Test fun returnsExactOriginalVideoPreviewForPlayback() {
        val original = video("Unchanged (Live Remix)", "Original Creator", "abc12345678")
        val result = SavedCreatorBrowser.groups(listOf(original)).single().tracks.single()
        assertEquals(original, result)
        assertEquals(original.url, result.url)
    }
}
