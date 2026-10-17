#!/usr/bin/env python3
"""6O1 source and preservation checks only; not Kotlin compilation or phone tests."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def src(p): return (root/p).read_text(encoding="utf-8")
base="app/src/main/java/com/nightwave/player/"
policy=src(base+"net/SearchRelevancePolicy.kt")
saved=src(base+"net/SavedTrackSearch.kt")
gateway=src(base+"net/YouTubeGateway.kt")
activity=src(base+"v2/ui/NightWaveStudioActivity.kt")
tests=src("app/src/test/java/com/nightwave/player/net/SearchRelevancePolicyTest.kt")
ci=src(".github/workflows/android.yml")
gradle=src("app/build.gradle.kts")
assert "Normalizer.Form.NFKD" in policy
assert 'private val combining = Regex("\\\\p{M}+")' in policy
assert "query.terms.all { it in all }" in policy
assert "track.title" in policy and "track.author" in policy
assert "MusicIdentity.display(track.title, track.author)" in policy
assert "thenBy { it.index }" in policy
assert "fun rankPublic(query: String, results: List<VideoPreview>)" in policy
assert "score -= 110" in policy
assert "if (!prepared.valid) return emptyList()" in saved
assert "SearchRelevancePolicy.savedMatches(prepared, track)" in saved
assert "SearchRelevancePolicy.savedScore(prepared, it.value)" in saved
assert "distinctBy { it.value.url }" in saved
assert "take(limit.coerceIn(1, 24))" in saved
assert "SearchRelevancePolicy.rankPublic(query, videos)" in gateway
assert "mode == SearchMode.SONGS" in gateway
assert "else videos" in gateway
assert "VideoLink.normalizeSearchResult(item.url)" in gateway
assert "val savedMatches = remember(query, savedTracks)" in activity
assert "SavedTrackSearch.match(query, savedTracks)" in activity
assert "onPrimarySearch(query.trim(), mode)" in activity
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert "tools/verify_v2_phase6o1.py" in ci
assert "feature/nightwave-v2-phase6n3-listening-level" in ci
assert tests.count("@Test") >= 16
for name,data in (("policy",policy),("saved",saved),("gateway",gateway),
                  ("activity",activity),("tests",tests)):
    assert data.count("{") == data.count("}"), name
    imports=[l for l in data.splitlines() if l.startswith("import ")]
    assert len(imports)==len(set(imports)), name
print("PASS: 6O1 Unicode matching, stable ranking, preserved playable identity")
print("PENDING: Android Gradle/JUnit execution and physical phone verification")
