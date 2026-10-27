package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW112: 135 curated cross-script query/title fixtures plus negative regressions. */
class SearchMultilingualAliasPolicyTest {
    private fun track(id: String, title: String) = VideoPreview(
        title, "Musician", "https://www.youtube.com/watch?v=$id", "", 220L
    )

    private val groups = listOf(
        listOf("jhum", "jhoom", "ঝুম"),
        listOf("bhalobasha", "valobasha", "ভালোবাসা"),
        listOf("bhalobashi", "valobashi", "ভালোবাসি")
    )

    @Test fun curatedWordAliasesAreSymmetricAndBounded() {
        groups.forEach { group ->
            group.forEach { original ->
                val variants = SearchMultilingualAliasPolicy.alternatives(original)
                assertEquals(2, variants.size)
                assertEquals(group.filterNot { it == original }.toSet(), variants.toSet())
            }
        }
        assertTrue(SearchMultilingualAliasPolicy.alternatives("").isEmpty())
        assertTrue(SearchMultilingualAliasPolicy.alternatives("alo").isEmpty())
        assertTrue(SearchMultilingualAliasPolicy.alternatives("jhumble").isEmpty())
        assertTrue(SearchMultilingualAliasPolicy.alternatives("jhum ".repeat(9).trim()).isEmpty())
    }

    @Test fun publicSongSearchRanks135ExplicitRomanizedAndBanglaFixtures() {
        val suffixes = listOf("", " official audio", " remix", " live", " acoustic")
        var checked = 0
        groups.forEach { group ->
            group.forEach { typed ->
                group.forEach { source ->
                    suffixes.forEach { suffix ->
                        val query = typed + suffix
                        val title = source + suffix
                        val wanted = track("abc12345678", title)
                        val distractor = track("abc12345679", "Unrelated mountain documentary")
                        val actual = SearchRelevancePolicy.rankPublic(
                            query, listOf(distractor, wanted))
                        assertEquals("Query '$query' should find '$title'",
                            wanted, actual.first())
                        assertEquals(listOf(distractor.url, wanted.url).toSet(),
                            actual.map { it.url }.toSet())
                        checked++
                    }
                }
            }
        }
        assertEquals(135, checked)
    }

    @Test fun directTypedTitleWinsOverAlternateSpelling() {
        val exact = track("abc12345678", "Jhum (Official Audio)")
        val alternative = track("abc12345679", "Jhoom (Official Audio)")
        assertEquals(exact, SearchRelevancePolicy.rankPublic(
            "jhum", listOf(alternative, exact)).first())
        assertEquals(alternative, SearchRelevancePolicy.rankPublic(
            "jhoom", listOf(exact, alternative)).first())
    }

    @Test fun savedMatchesUseAliasesOnlyWhenDirectHitsAbsent() {
        val exact = track("abc12345678", "Jhum")
        val alternate = track("abc12345679", "Jhoom")
        val native = track("abc12345670", "ঝুম")
        assertEquals(listOf(alternate), SavedTrackSearch.match("jhum", listOf(alternate)))
        assertEquals(listOf(native), SavedTrackSearch.match("jhoom", listOf(native)))
        assertEquals(listOf(exact), SavedTrackSearch.match("jhum", listOf(alternate, exact)))
        assertEquals(listOf(exact), SavedTrackSearch.match("ঝুম", listOf(exact)))
    }

    @Test fun nativeBanglaVowelsStayDistinctWithoutBroadTransliteration() {
        val mala = track("abc12345678", "মালা")
        val mela = track("abc12345679", "মেলা")
        assertEquals(listOf(mala), SavedTrackSearch.match("মালা", listOf(mela, mala)))
        assertEquals(listOf(mela), SavedTrackSearch.match("মেলা", listOf(mala, mela)))
        assertTrue(SearchMultilingualAliasPolicy.alternatives("মালা").isEmpty())
        assertTrue(SavedTrackSearch.match("মালা", listOf(mela)).isEmpty())
        assertFalse(SearchRelevancePolicy.prepare("মালা").text ==
            SearchRelevancePolicy.prepare("মেলা").text)
    }

    @Test fun repeatedQueryTokensNeedTwoActualWordOccurrences() {
        val once = track("abc12345678", "Jhoom")
        val twice = track("abc12345679", "Jhoom Jhoom")
        assertEquals(listOf(twice), SavedTrackSearch.match(
            "jhum jhum", listOf(once, twice)))
        assertTrue(SavedTrackSearch.match("jhum jhum", listOf(once)).isEmpty())
        assertTrue(SearchMultilingualAliasPolicy.alternatives("jhum jhum").size <= 15)
    }

    @Test fun typedMusicContextStillDemotesUnrelatedCultivation() {
        val cultivation = track("abc12345678", "Jhum Cultivation Explained")
        val music = track("abc12345679", "Jhoom Official Audio")
        assertEquals(music, SearchRelevancePolicy.rankPublic(
            "jhum", listOf(cultivation, music)).first())
        assertEquals(0, SearchMusicIntentPolicy.incidentalNonMusicPenalty(
            "jhum cultivation", cultivation))
    }

    @Test fun videosModePreservesProviderOrderAndOriginalUrls() {
        val cultivation = track("abc12345678", "Jhum Cultivation Documentary")
        val music = track("abc12345679", "Jhoom (Official Audio)")
        val provider = listOf(cultivation, music)
        val groups = SearchResultPresentationPolicy.groupMusic(
            "jhum", SearchMode.VIDEOS, provider)
        assertEquals(provider, groups.moreResults)
        assertTrue(groups.bestMatches.isEmpty())
        assertTrue(groups.otherVersions.isEmpty())
        val songs = SearchResultPresentationPolicy.groupMusic(
            "jhum", SearchMode.SONGS, provider)
        assertEquals(provider.toSet(),
            (songs.bestMatches + songs.otherVersions + songs.moreResults).toSet())
    }

    @Test fun unrelatedEnglishTitlesAndNumericTokensAreNotAliased() {
        val alone = track("abc12345678", "Alone (Official Audio)")
        val alo = track("abc12345679", "Alo Alo (Official Audio)")
        assertEquals(alo, SearchRelevancePolicy.rankPublic(
            "alo alo", listOf(alone, alo)).first())
        val partTwo = track("abc12345670", "Alone Part II")
        val year = track("abc12345671", "Alone 2024")
        assertEquals(partTwo, SearchRelevancePolicy.rankPublic(
            "alone 2", listOf(year, partTwo)).first())
    }
}
