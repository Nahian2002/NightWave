package com.nightwave.player.net

import com.nightwave.player.v2.library.LibraryBrowsePolicy
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class SearchPerformanceParityTest {
    private fun song(title: String, author: String = "Creator", id: Int = 1) =
        VideoPreview(title, author, "https://www.youtube.com/watch?v=${id.toString().padStart(11, '0')}", "", 210)

    // Independent old grade-first selection keeps occurrence semantics under test.
    private fun previousEvidence(terms: List<String>, words: List<String>, typo: Boolean): List<Int> {
        val used = BooleanArray(words.size)
        return terms.map { term ->
            var selected = 0
            for (grade in 3 downTo 1) {
                val at = words.indices.firstOrNull {
                    !used[it] && SearchTokenMatchingPolicy.evidence(term, listOf(words[it]), typo) == grade
                }
                if (at != null) { used[at] = true; selected = grade; break }
            }
            selected
        }
    }

    @Test fun singleScanRetainsGradeAndOccurrenceSemanticsAcrossSeededInputs() {
        val vocabulary = listOf("", "alo", "alone", "night", "nights", "nigth", "midnight", "midnigth",
            "123", "1234", "মালা", "মেলা", "ভালোবাসা", "moonlight", "moonligt")
        val random = Random(122)
        repeat(1_200) {
            val terms = List(random.nextInt(0, 6)) { vocabulary.random(random) }
            val words = List(random.nextInt(0, 14)) { vocabulary.random(random) }
            for (typo in listOf(false, true)) assertEquals(previousEvidence(terms, words, typo),
                SearchTokenMatchingPolicy.evidenceForTerms(terms, words, typo))
        }
    }

    @Test fun exactOccurrenceWinsBeforePrefixAndEarlierEqualOccurrenceWins() {
        assertEquals(listOf(3, 2), SearchTokenMatchingPolicy.evidenceForTerms(
            listOf("night", "night"), listOf("nightfall", "night"), true))
        assertEquals(listOf(3, 0), SearchTokenMatchingPolicy.evidenceForTerms(
            listOf("alo", "alo"), listOf("alo", "alone"), true))
        assertEquals(listOf(1, 0), SearchTokenMatchingPolicy.evidenceForTerms(
            listOf("midnigth", "midnight"), listOf("midnight"), true))
    }

    @Test fun preparedSavedScoresMatchDirectScoresForEveryModeAndScript() {
        val songs = listOf(song("Moonlight (Official Audio)"), song("ভালোবাসা", "বাংলা"),
            song("Alo Alo", "Alo"), song("Artist - Midnight", "Artist - Topic"))
        for (track in songs) for (text in listOf("", "moonlight", "moonligt", "midnigth", "alo alo", "ভালোবাসা", "ভলবস")) {
            val query = SearchRelevancePolicy.prepare(text)
            for (typo in listOf(false, true)) assertEquals(
                SearchRelevancePolicy.savedScore(query, track, typo),
                SearchRelevancePolicy.savedScore(query, SearchRelevancePolicy.prepareSaved(track), typo))
        }
    }

    @Test fun publicRankingMatchesScoreComparatorAndKeepsOriginalObjects() {
        val source = (0 until 65).map { song(listOf("Moonlight", "Moonlight remix", "Alo Alo",
            "ভালোবাসা", "Moonlight tutorial")[it % 5], "Creator ${it % 4}", it) }
        for (text in listOf("moonlight", "moonligt", "alo alo", "ভালোবাসা", "valobasha")) {
            val query = SearchRelevancePolicy.prepare(text)
            val previous = source.withIndex().sortedWith(compareByDescending<IndexedValue<VideoPreview>> {
                SearchRelevancePolicy.publicScore(query, it.value)
            }.thenBy { it.index }).map { it.value }
            val actual = SearchRelevancePolicy.rankPublic(text, source)
            assertEquals(previous, actual)
            actual.indices.forEach { assertSame(previous[it], actual[it]) }
        }
    }

    @Test fun invalidPublicQueryRetainsProviderOrderAndDuplicates() {
        val source = listOf(song("one"), song("two"), song("one"))
        assertSame(source, SearchRelevancePolicy.rankPublic("  ", source))
    }

    @Test fun invocationPreparationCannotHideAnEditedOrRemovedSong() {
        val old = song("Moonlight extended")
        val updated = old.copy(title = "ভালোবাসা")
        assertEquals(listOf(old), SavedTrackSearch.match("moonlight", listOf(old)))
        assertTrue(SavedTrackSearch.match("moonlight", listOf(updated)).isEmpty())
        assertEquals(listOf(updated), SavedTrackSearch.match("ভালোবাসা", listOf(updated)))
        assertTrue(SavedTrackSearch.match("ভালোবাসা", emptyList()).isEmpty())
    }

    @Test fun duplicateBestMetadataAndAliasFallbackKeepTheirPrecedence() {
        val weak = song("ভালোবাসা extended")
        val exact = weak.copy(title = "ভালোবাসা")
        assertEquals(listOf(exact), SavedTrackSearch.match("valobasha", listOf(weak, exact)))
        val direct = song("valobasha", id = 2)
        assertEquals(listOf(direct), SavedTrackSearch.match("valobasha", listOf(exact, direct)))
    }

    @Test fun sharedSuggestionResultsMatchStandaloneAcrossLimitsAndScripts() {
        val saved = listOf(song("Moonlight"), song("Moonlight remix", id = 2), song("ভালোবাসা", "বাংলা", 3))
        for (query in listOf("moonlight", "moonligt", "ভালোবাসা", "valobasha", "creator", "", "nomatch")) {
            val matches = SavedTrackSearch.match(query, saved)
            for (limit in listOf(-1, 0, 1, 6, 99)) assertEquals(
                LocalSearchSuggestions.suggest(query, saved, limit),
                LocalSearchSuggestions.fromMatches(query, saved, matches, limit))
        }
    }

    @Test fun largeLibrarySuggestionsFindItemsAfterFiveThousand() {
        val saved = List(5_000) { song("Other $it", id = it) } + song("Moonlight", id = 5_001)
        assertEquals(1, SavedTrackSearch.match("moonlight", saved).size)
        assertTrue(LocalSearchSuggestions.suggest("moonlight", saved).isNotEmpty())
        val boundedMatches = SavedTrackSearch.match("moonlight", saved)
        assertEquals(LocalSearchSuggestions.suggest("moonlight", saved),
            LocalSearchSuggestions.fromMatches("moonlight", saved, boundedMatches))
    }

    @Test fun preparedLibrarySortKeysMatchPreviousComparatorsIncludingStableTies() {
        val source = (0 until 70).map { song(listOf("Moonlight", "ভালোবাসা", "Artist - Night",
            "Alo Alo", "Moonlight (Official Audio)")[it % 5], "Creator ${it % 3}", it) }
        for (sort in listOf(LibraryBrowsePolicy.SORT_TITLE, LibraryBrowsePolicy.SORT_TITLE_DESC,
            LibraryBrowsePolicy.SORT_CREATOR)) {
            val title = compareBy<IndexedValue<VideoPreview>> {
                SearchRelevancePolicy.normalize(MusicIdentity.display(it.value.title, it.value.author).title)
            }
            val comparator = when (sort) {
                LibraryBrowsePolicy.SORT_TITLE_DESC -> title.reversed()
                LibraryBrowsePolicy.SORT_CREATOR -> compareBy<IndexedValue<VideoPreview>> {
                    SearchRelevancePolicy.normalize(MusicIdentity.display(it.value.title, it.value.author).artistOrCreator)
                }.then(title)
                else -> title
            }
            val expected = source.withIndex().sortedWith(comparator.thenBy { it.index }).map { it.value }
            assertEquals(expected, LibraryBrowsePolicy.songs(source, "", sort))
            assertEquals(expected.filter { SearchRelevancePolicy.savedMatches(SearchRelevancePolicy.prepare("moonlight"), it) },
                LibraryBrowsePolicy.songs(source, "moonlight", sort))
        }
    }

    @Test fun savedAndOldestLibraryViewsPreserveSourceAndInputIsNeverMutated() {
        val source = mutableListOf(song("B", id = 2), song("A", id = 1), song("A", id = 3))
        val before = source.toList()
        assertEquals(before, LibraryBrowsePolicy.songs(source, "", LibraryBrowsePolicy.SORT_SAVED))
        assertEquals(before.reversed(), LibraryBrowsePolicy.songs(source, "", LibraryBrowsePolicy.SORT_OLDEST))
        LibraryBrowsePolicy.songs(source, "", LibraryBrowsePolicy.SORT_TITLE_DESC)
        SavedTrackSearch.match("moonlight", source)
        assertEquals(before, source)
    }
}
