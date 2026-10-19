#!/usr/bin/env python3
"""NW-06 Search & Discovery static source contract, not runtime/provider verification."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]

def read(p: str) -> str:
    return (root / p).read_text(encoding="utf-8")

net = "app/src/main/java/com/nightwave/player/net/"
ui = read("app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt")
suggest = read(net + "LocalSearchSuggestions.kt")
visible = read(net + "SearchResultPresentationPolicy.kt")
tests = read("app/src/test/java/com/nightwave/player/net/LocalSearchSuggestionsTest.kt")
state_tests = read("app/src/test/java/com/nightwave/player/net/SearchResultPresentationPolicyTest.kt")
gateway = read(net + "YouTubeGateway.kt")
workflow = read(".github/workflows/android.yml")

# No fabricated catalogue artists, new server, or opt-out network suggestions.
assert "SavedTrackSearch.match(query, saved.take(400), limit = 24)" in suggest
assert "MusicIdentity.creatorsFrom(saved.take(400), limit = 12)" in suggest
assert "if (!prepared.valid || limit <= 0 || saved.isEmpty())" in suggest
assert "limit.coerceAtMost(8)" in suggest
assert "LocalSearchSuggestion.Kind.CREATOR" in suggest
assert tests.count("@Test") >= 6
assert "creatorSuggestionRequiresActualSavedCreatorMatch" in tests
assert "duplicatesDoNotMultiplySuggestions" in tests

# A new query or switching Songs to Videos must hide stale results.
assert "typedMode == resultMode" in visible
assert "input.isNotBlank() && input == result" in visible
assert "No public results returned for this search" in visible
assert state_tests.count("@Test") >= 4
assert "resultsOnlyMatchTheSameSearchMode" in state_tests
assert "typedNewSearchHidesOldResults" in state_tests

assert "LocalSearchSuggestions.suggest(query, savedTracks, limit = 6)" in ui
assert 'item(key = "local-search-suggestions")' in ui
assert "suggestion.kind ==" in ui
assert "SearchResultPresentationPolicy.matches(" in ui
assert "SearchResultPresentationPolicy.emptyHint(response.videos)" in ui
assert 'Text("Suggestions from your library"' in ui
assert 'Text("YouTube")' in ui
assert '"Search NightWave"' in ui
assert "onPrimarySearch(query.trim(), mode)" in ui
assert "val submit = { if (query.isNotBlank()) onNativeSearch(query.trim(), mode) }" in ui
assert 'onNativeSearch(query.trim(), mode)' in ui
assert "responseMatchesQuery && state.videos.isNotEmpty()" in ui
assert "request != nativeSearchGeneration || isFinishing || isDestroyed" in ui
assert "previous?.videos.orEmpty()" in ui

# Preserve honest external-service limitations and existing provider ranking.
assert "CommunityVideoSearch()" in gateway
assert "SearchRelevancePolicy.rankPublic(query, videos)" in gateway
assert "fun resolve(url: String): PlayableAudio" in gateway

assert "feature/nightwave-v2-nw06-discovery-search" in workflow
assert "python3 tools/verify_nw06_discovery_search.py" in workflow
for phase in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub",
              "nw04_premium_player", "nw05_integrated_stability"):
    assert f"python3 tools/verify_{phase}.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

for name, body in (("Studio", ui), ("Suggestions", suggest), ("Result presentation", visible)):
    imports = [line for line in body.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate Kotlin imports"
    assert body.count("{") == body.count("}"), name + " mismatched braces"

print("PASS: NW-06 local suggestions, mode-safe search and honest no-result UI contracts")
print("UNVERIFIED: Gradle Kotlin compilation, JVM tests, provider availability and phone UI")
