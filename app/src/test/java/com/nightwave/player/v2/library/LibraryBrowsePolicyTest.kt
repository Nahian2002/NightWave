package com.nightwave.player.v2.library

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBrowsePolicyTest {
    private fun video(title: String, artist: String, id: String) =
        VideoPreview(title, artist, "https://www.youtube.com/watch?v=$id", "", 210L)
    private fun playlist(name: String, vararg titles: String) = LibraryPlaylistPreview(
        name, titles.mapIndexed { i, title ->
            video(title, "Creator", "x" + i.toString().padStart(10, '0'))
        })

    @Test fun defaultCollectionViewPreservesOriginalOrder() {
        val original = listOf(playlist("Zebra"), playlist("Alpha"), playlist("Middle"))
        assertEquals(original, LibraryBrowsePolicy.playlists(original, "", "saved", false))
    }

    @Test fun nameSortAscendingDoesNotMutateOriginalList() {
        val original = listOf(playlist("Zebra"), playlist("Alpha"), playlist("Middle"))
        assertEquals(listOf("Alpha", "Middle", "Zebra"),
            LibraryBrowsePolicy.playlists(original, "", "name", false).map { it.name })
        assertEquals("Zebra", original.first().name)
    }

    @Test fun nameSortDescending() {
        val source = listOf(playlist("Alpha"), playlist("Zebra"), playlist("Middle"))
        assertEquals(listOf("Zebra", "Middle", "Alpha"),
            LibraryBrowsePolicy.playlists(source, "", "name_desc", false).map { it.name })
    }

    @Test fun mostTracksSortUsesCountAndStableNameTieBreak() {
        val source = listOf(playlist("Z", "One"), playlist("B", "One", "Two"),
            playlist("A", "One", "Two"))
        assertEquals(listOf("A", "B", "Z"),
            LibraryBrowsePolicy.playlists(source, "", "most_tracks", false).map { it.name })
    }

    @Test fun withTracksOnlyDoesNotDeleteEmptyPlaylists() {
        val empty = playlist("Empty")
        val full = playlist("Full", "Song")
        val input = listOf(empty, full)
        assertEquals(listOf(full), LibraryBrowsePolicy.playlists(input, "", "saved", true))
        assertEquals(2, input.size)
    }

    @Test fun playlistNameFilterIgnoresAccentsAndPunctuation() {
        val input = listOf(playlist("Café / Nights"), playlist("Classical"))
        assertEquals(listOf("Café / Nights"),
            LibraryBrowsePolicy.playlists(input, "cafe nights", "saved", false).map { it.name })
    }

    @Test fun playlistNameFilterRequiresAllTerms() {
        val input = listOf(playlist("Night Drive"), playlist("Night Ambient"))
        assertEquals(listOf("Night Drive"),
            LibraryBrowsePolicy.playlists(input, "night drive", "saved", false).map { it.name })
    }

    @Test fun optionalPlaylistSearchFindsSongsWithoutChangingPlaylistName() {
        val night = LibraryPlaylistPreview("Evening", listOf(
            video("ভালোবাসা", "Bangla Creator", "abc12345678"),
            video("Café Nights", "French Creator", "abc12345679")))
        val empty = playlist("Travel")
        assertTrue(LibraryBrowsePolicy.playlists(
            listOf(night, empty), "cafe", "saved", false).isEmpty())
        assertEquals(listOf(night), LibraryBrowsePolicy.playlists(
            listOf(night, empty), "cafe", "saved", false,
            matchContainedSongs = true))
        assertEquals(listOf(night), LibraryBrowsePolicy.playlists(
            listOf(night, empty), "বাংলা", "saved", false,
            matchContainedSongs = true))
        assertEquals("Evening", night.name)
    }

    @Test fun songTermsMustMatchOneSongNotAcrossDifferentPlaylistTracks() {
        val combo = LibraryPlaylistPreview("My songs", listOf(
            video("North Wind", "A", "abc12345678"),
            video("South Coast", "B", "abc12345679")))
        assertTrue(LibraryBrowsePolicy.playlists(
            listOf(combo), "north south", "saved", false,
            matchContainedSongs = true).isEmpty())
    }

    @Test fun punctuationOnlyPlaylistFilterIsSafe() {
        val input = listOf(playlist("A"), playlist("B"))
        assertEquals(input, LibraryBrowsePolicy.playlists(input, "--", "saved", false))
    }

    @Test fun emptyPlaylistsStillHaveNoCover() {
        val empty = playlist("Empty")
        assertEquals(0, empty.size)
        assertEquals(null, empty.cover)
    }

    @Test fun songSavedOrderUnchanged() {
        val a = video("Zebra", "X", "abc12345678")
        val b = video("Alpha", "Y", "abc12345679")
        assertEquals(listOf(a, b), LibraryBrowsePolicy.songs(listOf(a, b), "", "saved"))
    }

    @Test fun songTitleOrderUsesConservativeDisplayOnlyTitle() {
        val first = video("Zebra - Something [Official Audio]", "Zebra", "abc12345678")
        val second = video("Alpha", "Other", "abc12345679")
        assertEquals(listOf(second, first),
            LibraryBrowsePolicy.songs(listOf(first, second), "", "title"))
    }

    @Test fun songTitleDescending() {
        val a = video("Alpha", "X", "abc12345678")
        val z = video("Zebra", "Y", "abc12345679")
        assertEquals(listOf(z, a),
            LibraryBrowsePolicy.songs(listOf(a, z), "", "title_desc"))
    }

    @Test fun songCreatorSortDoesNotInventArtist() {
        val later = video("Zulu", "Beta Publisher", "abc12345678")
        val first = video("Alpha", "Alpha Publisher", "abc12345679")
        assertEquals(listOf(first, later),
            LibraryBrowsePolicy.songs(listOf(later, first), "", "artist"))
    }

    @Test fun songSearchMatchesTitleAndOriginalUploaderAcrossTerms() {
        val target = video("Beyoncé - Halo", "BeyonceVEVO", "abc12345678")
        val other = video("Halo", "Other", "abc12345679")
        assertEquals(listOf(target),
            LibraryBrowsePolicy.songs(listOf(other, target), "beyonce halo", "saved"))
    }

    @Test fun songFilterCanMatchPunctuationAndAccents() {
        val target = video("Après-Midi / Café", "Uploader", "abc12345678")
        assertEquals(listOf(target),
            LibraryBrowsePolicy.songs(listOf(target), "apres midi cafe", "saved"))
    }

    @Test fun trackReferencesAndOriginalSourceOrderRemainUnchanged() {
        val original = listOf(video("Z", "Artist", "abc12345678"),
            video("A", "Artist", "abc12345679"))
        val sorted = LibraryBrowsePolicy.songs(original, "", "title")
        assertEquals(original.reversed(), sorted)
        assertEquals("Z", original.first().title)
        assertEquals(original.map { it.url }.toSet(), sorted.map { it.url }.toSet())
    }

    @Test fun unknownSortModeFallsBackToSavedOrder() {
        val original = listOf(playlist("Z"), playlist("A"))
        assertEquals(original, LibraryBrowsePolicy.playlists(original, "", "unknown", false))
    }

    @Test fun identicalTitlesKeepSourceOrder() {
        val a = video("Same", "Creator", "abc12345678")
        val b = video("Same", "Creator", "abc12345679")
        assertEquals(listOf(a, b), LibraryBrowsePolicy.songs(listOf(a, b), "", "title"))
    }

    @Test fun filterDoesNotRewritePrivateOrCanonicalIdentifiers() {
        val offline = VideoPreview("Offline local", "Local", "offline:stored-id", "", 0L)
        assertEquals(listOf(offline), LibraryBrowsePolicy.songs(listOf(offline), "offline", "saved"))
        assertTrue(LibraryBrowsePolicy.songs(listOf(offline), "other", "saved").isEmpty())
        assertFalse(offline.url.startsWith("https://www.youtube.com/watch"))
    }
}
