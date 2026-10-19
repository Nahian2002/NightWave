#!/usr/bin/env python3
"""Phase 6X6 local Daily Mix source contract; not an Android/JUnit runner."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def src(path): return (root / path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
daily = src(base + "v2/recommendation/LocalDailyMixEngine.kt")
personal = src(base + "v2/recommendation/PersonalMixEngine.kt")
diversity = src(base + "v2/recommendation/MixDiversityPolicy.kt")
memory = src(base + "v2/insights/ListeningMemoryStore.kt")
ui = src(base + "v2/ui/NightWaveStudioActivity.kt")
tests = src("app/src/test/java/com/nightwave/player/v2/recommendation/LocalDailyMixEngineTest.kt")
legacy_tests = src("app/src/test/java/com/nightwave/player/v2/recommendation/PersonalMixEngineTest.kt")
ci = src(".github/workflows/android.yml")
assert "object LocalDailyMixEngine" in daily
assert "MAX_POOL = 500" in daily
assert "MAX_DAILY_MIXES = 3" in daily
assert "MAX_PER_MIX = 12" in daily
assert "MIN_PER_MIX = 3" in daily
assert "VideoLink.normalize(it.url) == it.url" in daily
assert ".distinctBy { it.url }" in daily
assert "val poolIds = pool.mapTo(hashSetOf())" in daily
assert "if (memoryEnabled)" in daily
assert "it.id in poolIds" in daily
assert 'val used = hashSetOf<String>()' in daily
assert "pool.filterNot { it.url in used }" in daily
assert "used.addAll(selected.map { it.url })" in daily
assert "dayNumber + (lane * 7919L)" in daily
assert "MixDiversityPolicy.select(" in daily
assert "MusicIdentity.display(track.title, track.author)" in daily
assert '"Daily Mix 1", "Daily Mix 2", "Daily Mix 3"' in daily
assert "Playback history enabled" in daily
assert "Based on saved music" in daily
assert "fun make(" in personal
assert "object MixDiversityPolicy" in diversity
assert "recommendationTracks = if (enabled)" in memory
assert "ListeningMemorySummary(false, 0L, 0, 0, emptyList())" in ui
assert "LocalDailyMixEngine.make(favorites, recent, playlistTracks," in ui
assert "PersonalMixEngine.make(favorites, recent, playlistTracks," in ui
assert "daily + existing" in ui
assert "mixes.chunked(2)" in ui
assert "onPlay(it, chosenMix.tracks)" in ui
assert 'SectionHeading("Made for you",' in ui
assert "nightwave-v2-phase6x6-private-daily-mixes" in ci
assert "python3 tools/verify_v2_phase6x6.py" in ci
assert "python3 tools/verify_v2_phase6x5.py" in ci
assert tests.count("@Test") >= 11
# Gradle compileDebugUnitTestKotlin on 6X8C3 found accidental nested lists
# passed as a third argument, where make requires List<VideoPreview>.
assert "playlistTracks: List<VideoPreview>" in daily
assert "listOf(invalid), valid + valid, 333L" in tests
assert "LocalDailyMixEngine.make(favorites, recent, favorites, 500L)" in tests
assert "listOf(invalid), listOf(valid, valid), 333L" not in tests
assert "LocalDailyMixEngine.make(favorites, recent, listOf(favorites), 500L)" not in tests

assert legacy_tests.count("@Test") >= 7
for name, text in (("daily", daily), ("tests", tests)):
    assert text.count("{") == text.count("}"), name + " unmatched braces"
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: Phase 6X6 private daily mixes source wiring, disjointness, consent and provenance")
print("PENDING: Gradle compilation, JUnit execution and Android device confirmation")
