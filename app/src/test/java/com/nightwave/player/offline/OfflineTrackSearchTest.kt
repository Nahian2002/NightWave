package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTrackSearchTest {
    private fun song(name: String, ext: String = "mp3") =
        OfflineSong("00000000-0000-4000-8000-000000000001.$ext", name, "audio/mpeg", 1200)

    @Test fun matchesTermsAcrossArtistAndTitle() {
        val tracks = listOf(song("Tonight - Dua Lipa"), song("Adele Hello"))
        assertEquals(listOf("Tonight - Dua Lipa"),
            OfflineTrackSearch.match("DUA tonight", tracks).map { it.title })
    }

    @Test fun ranksExactMatchAheadOfContainingText() {
        val tracks = listOf(song("Acoustic Morning Mix"), song("Morning"))
        assertEquals("Morning", OfflineTrackSearch.match("morning", tracks).first().title)
    }

    @Test fun punctuationAndUnicodeRemainSearchable() {
        val tracks = listOf(song("ফিরে দেখা - Live"), song("Night Drive"))
        assertEquals("ফিরে দেখা - Live",
            OfflineTrackSearch.match("ফিরে   দেখা", tracks).single().title)
    }

    @Test fun noMatchDoesNotFallThroughToPublicResults() {
        assertTrue(OfflineTrackSearch.match("unknown", listOf(song("Music"))).isEmpty())
    }

    @Test fun emptyQueryKeepsOriginalLibraryOrder() {
        val tracks = listOf(song("First"), song("Second"))
        assertEquals(tracks, OfflineTrackSearch.match("", tracks))
    }
}
