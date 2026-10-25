package com.nightwave.player.net

import org.junit.Assert.*
import org.junit.Test

/** Deterministic local matching, no network or Android device. */
class SavedTrackSearchTest {
    private fun track(title: String, artist: String, id: String) =
        VideoPreview(title, artist, "https://www.youtube.com/watch?v=$id", "", 100)

    @Test fun blankSearchDoesNotReturnEntireLibrary() {
        val library = listOf(track("Baby Shark", "Pinkfong", "dQw4w9WgXcQ"))
        assertTrue(SavedTrackSearch.match("  ", library).isEmpty())
    }

    @Test fun matchesTitleAndArtistEvenWithoutAnyProvider() {
        val item = track("Baby Shark", "Pinkfong", "dQw4w9WgXcQ")
        assertEquals(listOf(item), SavedTrackSearch.match("baby shark", listOf(item)))
        assertEquals(listOf(item), SavedTrackSearch.match("PINKFONG", listOf(item)))
    }

    @Test fun allSearchTermsMustMatchAcrossTitleAndArtist() {
        val right = track("Bangla Song", "Artist One", "abc12345678")
        val wrong = track("Bangla Talk", "Artist Two", "abc12345679")
        assertEquals(listOf(right),
            SavedTrackSearch.match("bangla one", listOf(wrong, right)))
    }

    @Test fun exactTitleRanksBeforePrefixAndDescriptions() {
        val near = track("Baby Shark remix", "Another", "abc12345678")
        val exact = track("Baby Shark", "Pinkfong", "abc12345679")
        assertEquals(listOf(exact, near),
            SavedTrackSearch.match("baby shark", listOf(near, exact)))
    }

    @Test fun duplicatesAreRemovedWithoutReorderingDistinctVideos() {
        val song = track("Night Sky", "Creator", "abc12345678")
        assertEquals(listOf(song),
            SavedTrackSearch.match("night", listOf(song, song)))
    }

    @Test fun duplicateUrlKeepsTheHighestRelevanceMetadata() {
        val first = track("Moonlight extended upload", "Creator", "abc12345678")
        val exact = track("Moonlight", "Creator", "abc12345678")
        val other = track("Moonlight remix", "Someone", "abc12345679")
        assertEquals(listOf(exact, other),
            SavedTrackSearch.match("moonlight", listOf(first, other, exact)))
    }

    @Test fun nonpositiveLimitReturnsNoTrackOrSuggestion() {
        val songs = listOf(track("Moonlight", "Creator", "abc12345678"))
        assertTrue(SavedTrackSearch.match("moon", songs, 0).isEmpty())
        assertTrue(SavedTrackSearch.match("moon", songs, -50).isEmpty())
    }

    @Test fun banglaSavedSongSearchRespectsActualVowelMarks() {
        val original = track("ভালোবাসা", "বাংলা গান", "abc12345678")
        assertEquals(listOf(original), SavedTrackSearch.match("ভালোবাসা", listOf(original)))
        assertTrue(SavedTrackSearch.match("ভলবস", listOf(original)).isEmpty())
    }

    @Test fun resultLimitIsBoundedAndDeterministic() {
        val many = (0 until 35).map {
            track("Night Track $it", "Creator", "v" + it.toString().padStart(10, '0'))
        }
        assertEquals(24, SavedTrackSearch.match("night", many, 50).size)
        assertEquals(1, SavedTrackSearch.match("night", many, 1).size)
    }
}
