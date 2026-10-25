package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultPresentationPolicyTest {
    @Test fun resultsOnlyMatchTheSameSearchMode() {
        assertTrue(SearchResultPresentationPolicy.matches(
            "Moonlight", SearchMode.SONGS, "moonlight", SearchMode.SONGS))
        assertFalse(SearchResultPresentationPolicy.matches(
            "Moonlight", SearchMode.VIDEOS, "moonlight", SearchMode.SONGS))
    }

    @Test fun typedNewSearchHidesOldResults() {
        assertFalse(SearchResultPresentationPolicy.matches(
            "new track", SearchMode.SONGS, "old track", SearchMode.SONGS))
    }

    @Test fun normalizedWhitespaceAndAccentsRemainPredictable() {
        assertTrue(SearchResultPresentationPolicy.matches(
            "  Beyoncé   HALO ", SearchMode.SONGS, "beyonce halo", SearchMode.SONGS))
        assertFalse(SearchResultPresentationPolicy.matches(
            "!!!", SearchMode.SONGS, "!!!", SearchMode.SONGS))
    }

    @Test fun banglaVowelSpellingPreventsStalePublicResults() {
        assertFalse(SearchResultPresentationPolicy.matches(
            "মালা", SearchMode.SONGS, "মেলা", SearchMode.SONGS))
        assertTrue(SearchResultPresentationPolicy.matches(
            "ভালোবাসা", SearchMode.SONGS, "ভালোবাসা", SearchMode.SONGS))
    }

    @Test fun emptyProviderResultsAreNotMisrepresentedAsMissingSavedMusic() {
        assertTrue(SearchResultPresentationPolicy.emptyHint(emptyList()).contains("No public results"))
        val song = VideoPreview("Music", "Artist", "https://example.com", "", 200L)
        assertEquals("", SearchResultPresentationPolicy.emptyHint(listOf(song)))
    }
}
