#!/usr/bin/env python3
"""NW82 deterministic source contracts (not a Kotlin or device test)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "app/src/main/java/com/nightwave/player/"
TEST = "app/src/test/java/com/nightwave/player/"
def source(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

token = source(BASE + "net/SearchTokenMatchingPolicy.kt")
relevance = source(BASE + "net/SearchRelevancePolicy.kt")
intent = source(BASE + "net/SearchMusicIntentPolicy.kt")
saved = source(BASE + "net/SavedTrackSearch.kt")
offline = source(BASE + "offline/OfflineSearchRanking.kt")
versions = source(BASE + "net/SearchVersionGroupingPolicy.kt")
groups = source(BASE + "net/SearchResultPresentationPolicy.kt")
ui = source(BASE + "v2/ui/NightWaveStudioActivity.kt")
creator = source(BASE + "net/LocalSearchSuggestions.kt")
smoke = source("tools/NW82SearchSmoke.kt")
tests = source(TEST + "net/SearchTokenMatchingPolicyTest.kt")
search_tests = source(TEST + "net/SearchRelevancePolicyTest.kt")
group_tests = source(TEST + "net/SearchResultPresentationPolicyTest.kt")

# No fake short-prefix results or overbroad Unicode-typo matching.
assert "term.length >= 4" in token
assert "term.length >= 5 && asciiLatin.matches(term)" in token
assert "oneEditApart(term, it)" in token
assert "evidenceForTerms(" in token
assert "val used = BooleanArray(words.size)" in token
assert "SearchTokenMatchingPolicy.evidenceForTerms(" in relevance
assert "if (covered == terms.size)" in relevance
assert "SearchTokenMatchingPolicy.allCovered(" in intent
assert "allowTypo = true" in saved
assert "if (strict.isNotEmpty()) strict else" in saved
assert "term.length >= 4 && word.startsWith(term)" in offline
assert "SearchTokenMatchingPolicy.allCovered(" in creator

# Version grouping must not coalesce or silently mutate playback IDs.
assert "fun isOtherVersion(" in versions
assert "val otherVersions: List<VideoPreview>" in groups
assert "partition { track ->" in groups
assert "return Groups(best, remaining, versions)" in groups
assert '"nw82-alternate-recordings-heading"' in ui
assert "publicGroups.otherVersions" in ui
assert '"alternate:" + it.url' in ui
assert "onClick = { playSinglePublic(song) }" in ui
search = ui.split("private fun StudioSearch(", 1)[1].split("private fun StudioLibrary(", 1)[0]
assert search.count("onPlay(selected, listOf(selected))") == 1
assert search.count("playSinglePublic(song)") == 3
assert 'onPlay(song, state.videos)' not in search

for needle in (
    "aloIsNotAloneEvenWhenSongQueryRepeatsIt",
    "shortAndNonLatinNamesAreNeverTypoMerged",
):
    assert needle in tests
for needle in (
    "nw82AloAloNeverMisranksAloneOverExactMusic",
    "nw82ExactSongBeatsPrefixAndTypoAndUnrequestedVersions",
    "nw82BanglaVowelSpellingsStayDistinctAndEnglishTyposAreLimited",
):
    assert needle in search_tests
assert "nw82AlternateVersionsAreGroupedButNeverHiddenOrPlayedAutomatically" in group_tests
assert "NW82_SEARCH_POLICY_SMOKE_PASS" in smoke
print("NW82_SEARCH_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: Kotlin compilation, Gradle, provider search quality and device QA")
