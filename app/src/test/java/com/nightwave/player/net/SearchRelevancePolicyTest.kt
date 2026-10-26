package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRelevancePolicyTest {
    private fun track(id: String, title: String, creator: String = "Creator", seconds: Long = 220L) =
        VideoPreview(title, creator, "https://www.youtube.com/watch?v=" + id, "", seconds)
    private val a = "abc12345678"
    private val b = "abc12345679"
    private val c = "abc12345670"

    @Test fun banglaVowelSignsArePreservedAndRemainDistinct() {
        val complete = "ভালোবাসা"
        val withoutVowels = "ভলবস"
        assertEquals(complete, SearchRelevancePolicy.normalize(complete))
        assertFalse(SearchRelevancePolicy.normalize(complete) ==
            SearchRelevancePolicy.normalize(withoutVowels))
        val original = track(a, "ভালোবাসা - রাত", "বাংলা সংগীত")
        assertEquals(listOf(original), SavedTrackSearch.match("ভালোবাসা", listOf(original)))
        assertTrue(SavedTrackSearch.match("ভলবস", listOf(original)).isEmpty())
    }

    @Test fun staleBanglaSearchResultsCannotMatchDifferentVowelSpelling() {
        val first = SearchRelevancePolicy.prepare("মালা")
        val second = SearchRelevancePolicy.prepare("মেলা")
        assertFalse(first.text == second.text)
        assertTrue(first.valid && second.valid)
    }

    @Test fun accentInsensitiveSavedTitleMatch() {
        val original = track(a, "Beyoncé - Halo", "Beyoncé")
        assertEquals(listOf(original), SavedTrackSearch.match("beyonce halo", listOf(original)))
    }

    @Test fun originalSpellingAndUrlAreNeverModified() {
        val original = track(a, "Beyoncé - Halo (Official Audio)", "Beyoncé")
        val actual = SavedTrackSearch.match("beyonce", listOf(original)).single()
        assertEquals(original, actual)
        assertEquals(original.url, actual.url)
        assertEquals("Beyoncé - Halo (Official Audio)", actual.title)
    }

    @Test fun punctuationActsAsWordBoundary() {
        val original = track(a, "Late-Night / Drive", "A Band")
        assertEquals(listOf(original), SavedTrackSearch.match("late night drive", listOf(original)))
    }

    @Test fun allTermsMustMatchEvenWhenAcrossTitleAndCreator() {
        val hit = track(a, "Ocean Lights", "North Star")
        val miss = track(b, "Ocean Lights", "Different")
        assertEquals(listOf(hit), SavedTrackSearch.match("ocean north", listOf(miss, hit)))
    }

    @Test fun exactTitleOutranksPrefix() {
        val prefix = track(a, "Midnight Drive Extended")
        val exact = track(b, "Midnight Drive")
        assertEquals(listOf(exact, prefix), SavedTrackSearch.match("midnight drive", listOf(prefix, exact)))
    }

    @Test fun exactCreatorOutranksWeakPartialTitle() {
        val partial = track(a, "A Tribute to Airwave Nights", "Elsewhere")
        val exact = track(b, "Unrelated Song", "Airwave")
        assertEquals(listOf(exact, partial), SavedTrackSearch.match("airwave", listOf(partial, exact)))
    }

    @Test fun displayOnlyIdentitySupportsVerifiedSpacedName() {
        val original = track(a, "Ellie Goulding - Love Me Like You Do (Official Music Video)",
            "EllieGouldingVEVO")
        assertEquals(listOf(original), SavedTrackSearch.match("ellie goulding", listOf(original)))
    }

    @Test fun duplicateReferencesRemainDeduplicatedAndStable() {
        val first = track(a, "Northern Lights")
        val second = track(b, "Northern Lights")
        assertEquals(listOf(first, second),
            SavedTrackSearch.match("northern", listOf(first, first, second)))
    }

    @Test fun punctuationOnlyDoesNotDisplayWholeLibrary() {
        assertTrue(SavedTrackSearch.match("?! -", listOf(track(a, "Anything"))).isEmpty())
    }

    @Test fun savedSearchLimitRemainsTwentyFour() {
        val many = (1..36).map { i ->
            track("x" + i.toString().padStart(10, '0'), "Dream " + i)
        }
        assertEquals(24, SavedTrackSearch.match("dream", many, 999).size)
    }

    @Test fun publicMusicSearchDemotesIrrelevantReactions() {
        val reaction = track(a, "Moonlight official audio reaction", "Reviewer")
        val official = track(b, "Moonlight (Official Audio)", "Song Creator")
        assertEquals(listOf(official, reaction),
            SearchRelevancePolicy.rankPublic("moonlight", listOf(reaction, official)))
    }

    @Test fun reactionIntentStillFindsReactions() {
        val reaction = track(a, "Moonlight reaction", "Reviewer")
        val generic = track(b, "Moonlight", "Other")
        val prepared = SearchRelevancePolicy.prepare("moonlight reaction")
        assertTrue(SearchRelevancePolicy.publicScore(prepared, reaction) >
            SearchRelevancePolicy.publicScore(prepared, generic))
    }

    @Test fun publicResultsAreSortedButNeverDroppedOrRewritten() {
        val first = track(a, "Unrelated track")
        val second = track(b, "Moonlight")
        val third = track(c, "Moonlight acoustic", "Another")
        val ranked = SearchRelevancePolicy.rankPublic("moonlight", listOf(first, second, third))
        assertEquals(3, ranked.size)
        assertEquals(listOf(first.url, second.url, third.url).toSet(), ranked.map { it.url }.toSet())
        assertEquals(second, ranked.first())
    }

    @Test fun equalPublicScoresPreserveProviderOrdering() {
        val first = track(a, "North Wind", "Creator")
        val second = track(b, "North Wind", "Creator")
        assertEquals(listOf(first, second),
            SearchRelevancePolicy.rankPublic("north", listOf(first, second)))
    }

    @Test fun blankQuickRankingPreservesProviderOrder() {
        val original = listOf(track(a, "North Wind"), track(b, "South Wind"))
        assertEquals(original, SearchRelevancePolicy.rankPublic("   ", original))
    }

    @Test fun typoTolerantDoesNotMeanFalseVerifiedCatalog() {
        val query = SearchRelevancePolicy.prepare("moonlight")
        val other = track(a, "The Sunny Day", "Uploader")
        assertFalse(SearchRelevancePolicy.savedMatches(query, other))
    }

    @Test fun nw82AloAloNeverMisranksAloneOverExactMusic() {
        val wrong = track(a, "Alan Walker - Alone (Official Audio)", "Alan Walker")
        val right = track(b, "Alo Alo (Official Audio)", "Alo Bangla")
        val ranked = SearchRelevancePolicy.rankPublic("Alo Alo", listOf(wrong, right))
        assertEquals(right, ranked.first())
        assertTrue(SearchRelevancePolicy.publicScore(
            SearchRelevancePolicy.prepare("alo alo"), right) >
            SearchRelevancePolicy.publicScore(
                SearchRelevancePolicy.prepare("alo alo"), wrong))
        assertTrue(SavedTrackSearch.match("alo alo", listOf(wrong)).isEmpty())
        assertEquals(listOf(right), SavedTrackSearch.match("alo alo", listOf(wrong, right)))
    }

    @Test fun nw82ExactSongBeatsPrefixAndTypoAndUnrequestedVersions() {
        val remix = track(a, "Moonlight Remix", "Creator")
        val typo = track(b, "Moonlights", "Creator")
        val exact = track(c, "Moonlight (Official Audio)", "Creator")
        assertEquals(exact, SearchRelevancePolicy.rankPublic(
            "moonlight", listOf(remix, typo, exact)).first())
        assertEquals(remix, SearchRelevancePolicy.rankPublic(
            "moonlight remix", listOf(exact, remix)).first())
        assertTrue(SearchRelevancePolicy.publicScore(
            SearchRelevancePolicy.prepare("moonligt"), exact) >
            SearchRelevancePolicy.publicScore(
                SearchRelevancePolicy.prepare("moonligt"), track(b, "Sunny Afternoon")))
    }

    @Test fun nw82BanglaVowelSpellingsStayDistinctAndEnglishTyposAreLimited() {
        val right = track(a, "মালা", "সঙ্গীত")
        val wrong = track(b, "মেলা", "সঙ্গীত")
        assertEquals(listOf(right), SavedTrackSearch.match("মালা", listOf(wrong, right)))
        assertEquals(listOf(wrong), SavedTrackSearch.match("মেলা", listOf(right, wrong)))
        val correctEnglish = track(c, "Midnight Drive", "Artist")
        assertEquals(listOf(correctEnglish),
            SavedTrackSearch.match("midnigth", listOf(correctEnglish)))
        assertTrue(SavedTrackSearch.match("mal", listOf(wrong, right)).isEmpty())
    }

    @Test fun nw82ExactSavedHitsPreventBroadFuzzyMatches() {
        val exact = track(a, "Moonlight", "Musician")
        val approx = track(b, "Moonlights", "Elsewhere")
        val results = SavedTrackSearch.match("moonlight", listOf(approx, exact))
        assertEquals(listOf(exact, approx), results)
        assertEquals(listOf(exact), SavedTrackSearch.match("moonligt", listOf(exact)))
        assertEquals(listOf(approx), SavedTrackSearch.match("moonlights", listOf(exact, approx)))
    }
}
