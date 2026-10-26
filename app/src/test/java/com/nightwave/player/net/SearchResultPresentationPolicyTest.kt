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

    @Test fun nw82AlternateVersionsAreGroupedButNeverHiddenOrPlayedAutomatically() {
        val exact = VideoPreview("Moonlight (Official Audio)", "Singer",
            "https://www.youtube.com/watch?v=abc12345678", "", 215L)
        val remix = VideoPreview("Moonlight Remix", "DJ",
            "https://www.youtube.com/watch?v=abc12345679", "", 225L)
        val live = VideoPreview("Moonlight (Live)", "Singer",
            "https://www.youtube.com/watch?v=abc12345670", "", 240L)
        val unrelated = VideoPreview("A Landscape Tour", "Uploader",
            "https://www.youtube.com/watch?v=abc12345671", "", 205L)
        val originals = listOf(unrelated, remix, live, exact)
        val grouped = SearchResultPresentationPolicy.groupMusic(
            "moonlight", SearchMode.SONGS, originals)
        assertEquals(exact, grouped.bestMatches.first())
        assertEquals(setOf(remix, live), grouped.otherVersions.toSet())
        val visible = grouped.bestMatches + grouped.otherVersions + grouped.moreResults
        assertEquals(originals.size, visible.size)
        assertEquals(originals.toSet(), visible.toSet())
        assertTrue(grouped.moreResults.contains(unrelated))
        // Videos mode never guesses versions and never changes provider order.
        val videoMode = SearchResultPresentationPolicy.groupMusic(
            "moonlight", SearchMode.VIDEOS, originals)
        assertTrue(videoMode.bestMatches.isEmpty())
        assertTrue(videoMode.otherVersions.isEmpty())
        assertEquals(originals, videoMode.moreResults)
    }

    @Test fun nw82RequestedRemixRemainsPrimaryRatherThanAnUnrequestedVariant() {
        val remix = VideoPreview("Moonlight Remix", "DJ",
            "https://www.youtube.com/watch?v=abc12345678", "", 220L)
        val original = VideoPreview("Moonlight (Official Audio)", "Singer",
            "https://www.youtube.com/watch?v=abc12345679", "", 220L)
        val result = SearchResultPresentationPolicy.groupMusic(
            "moonlight remix", SearchMode.SONGS, listOf(original, remix))
        assertEquals(remix, result.bestMatches.first())
        assertFalse(result.otherVersions.contains(remix))
    }
}
