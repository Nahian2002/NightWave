#!/usr/bin/env python3
"""Phase 6L source and data-preservation checks; NOT Android compilation."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def src(p):
    return (root / p).read_text(encoding="utf-8")

prefix = "app/src/main/java/com/nightwave/player/"
engine = src(prefix + "v2/recommendation/PersonalMixEngine.kt")
policy = src(prefix + "v2/recommendation/MixDiversityPolicy.kt")
memory = src(prefix + "v2/insights/ListeningMemoryStore.kt")
activity = src(prefix + "v2/ui/NightWaveStudioActivity.kt")
tests = src("app/src/test/java/com/nightwave/player/v2/recommendation/MixDiversityPolicyTest.kt")
workflow = src(".github/workflows/android.yml")
build = src("app/build.gradle.kts")
assert "MixDiversityPolicy.select(source, dayNumber, 12, ::rank)" in engine
assert "MixDiversityPolicy.select(candidates, dayNumber, 12, ::memoryAffinity)" in engine
assert "if (memoryEnabled) memoryTracks.take(100)" in engine
assert "return outputs.take(4)" in engine
assert "if (!signatures.add(signature)) return" in engine
assert "VideoLink.normalize(it.url) == it.url" in engine
assert "private const val MAX_POOL = 500" in policy
assert "private const val MAX_OUTPUT = 12" in policy
assert "REPEATED_CREATOR_PENALTY = 3" in policy
assert "source.take(MAX_POOL).distinctBy { it.url }" in policy
assert "dailyKey(it.url, dayNumber)" in policy
assert "private fun creatorKey" in policy
assert "recommendationTracks = if (enabled)" in memory
assert "PersonalMixEngine.make(favorites, recent, playlistTracks, dayNumber" in activity
assert "mixes.chunked(2)" in activity
assert 'applicationId = "com.nightwave.player"' in build
assert "versionCode = 35" in build
assert "tools/verify_v2_phase6l.py" in workflow
assert "feature/nightwave-v2-phase6k-service-sleep-timer" in workflow
assert tests.count("@Test") >= 10
for name, text in (("engine", engine), ("policy", policy), ("tests", tests)):
    assert text.count("{") == text.count("}"), name
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name
print("PASS: 6L source checks; library-only, opt-in memory, unique and bounded mixes")
print("PENDING: complete Gradle Android/JVM tests and physical-device QA")
