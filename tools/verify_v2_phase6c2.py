#!/usr/bin/env python3
"""Phase 6C2 source checks; deliberately NOT an Android build or device test."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(p):
    return (root / p).read_text(encoding="utf-8")
src = "app/src/main/java/com/nightwave/player/"
store = read(src + "v2/insights/ListeningMemoryStore.kt")
trend = read(src + "v2/insights/ListeningTrendEngine.kt")
trend_ui = read(src + "v2/ui/ListeningTrendPanel.kt")
memory_ui = read(src + "v2/ui/ListeningMemoryPanel.kt")
mixes = read(src + "v2/recommendation/PersonalMixEngine.kt")
activity = read(src + "v2/ui/NightWaveStudioActivity.kt")
cloud = read(src + "v2/cloud/CloudBackupCodec.kt")
library = read(src + "v2/data/LibraryBackupCodec.kt")
worker = read(src + "v2/cloud/NightWaveCloudWorker.kt")
service = read(src + "playback/PlaybackService.kt")
gradle = read("app/build.gradle.kts")
ci = read(".github/workflows/android.yml")
trend_tests = read("app/src/test/java/com/nightwave/player/v2/insights/ListeningTrendEngineTest.kt")
mix_tests = read("app/src/test/java/com/nightwave/player/v2/recommendation/PersonalMixMemoryTest.kt")
store_tests = read("app/src/androidTest/java/com/nightwave/player/v2/insights/ListeningMemoryTrendInstrumentedTest.kt")

# UTC days are explicit and are the observed pulse commit date, not invented
# sessions or geolocation. Retention is 30 days, pruned on data access.
assert 'data class DailyListeningBucket(' in trend
assert 'MAX_DAYS = 30' in trend
assert 'MS_PER_DAY = 86_400_000L' in trend
assert 'Math.floorDiv(nowWallMs, MS_PER_DAY)' in trend
assert 'fun rolling(' in trend
assert 'require(days in 1..MAX_DAYS)' in trend
assert 'fun groupedByWeek(' in trend
assert 'LocalDate.ofEpochDay' in trend
assert 'MAX_DAILY_JSON_BYTES = 8_000' in store
assert 'private fun readDaily(today: Long)' in store
assert 'if (day !in lower..today) continue' in store
assert 'days.distinctBy { it.epochDayUtc }' in store
assert 'val day = ListeningTrendEngine.utcEpochDay(pulse.recordedAtMs)' in store
assert 'putString("dailyUtc", dailyData).commit()' in store
assert 'putString("aggregates", data)' in store
assert 'if (prefs.getString("dailyUtc", "[]") != encoded)' in store
assert 'daily = days' in store
assert 'recommendationTracks = if (enabled)' in store
assert 'tracks.filter {' in store

# Users explicitly opt in; existing privacy fence and erase remain intact.
assert 'getBoolean("consent", false)' in store
assert 'fun eraseAndDisable()' in store
assert 'prefs.edit().clear().putLong("revision", next).commit()' in store
assert 'prefs.getLong("revision", 0L) != expectedRevision' in store
assert 'ListeningMemoryPanel(' in activity
assert 'ListeningTrendPanel(' in memory_ui
assert 'var window by rememberSaveable' in trend_ui
assert 'window == 7' in trend_ui
assert 'window == 30' in trend_ui
assert 'ListeningTrendEngine.groupedByWeek(points)' in trend_ui
assert 'No listening time recorded for this period.' in trend_ui
assert 'UTC dates' in trend_ui
assert 'playback may not appear' in trend_ui
assert 'Older daily buckets expire automatically' not in trend_ui

# Ranking is still local catalog-only, never creates URLs from memory.
assert 'memoryEnabled: Boolean = false' in mixes
assert 'val learned = if (memoryEnabled)' in mixes
assert 'memoryTracks.take(100)' in mixes
assert '(likes + plays + lists).distinctBy { it.url }' in mixes
assert 'val memoryCandidates' not in mixes  # no uncontrolled external list
assert 'val repeatCandidates = pool.filter' in mixes
assert 'learned[song.url]' in mixes
assert 'fun memoryAffinity(track: VideoPreview)' in mixes
assert 'add("memory", "Your listening rotation"' in mixes
assert 'return outputs.take(3)' in mixes
assert 'memoryTracks = memory.recommendationTracks' in activity
assert 'memoryEnabled = memory.enabled' in activity
assert 'tab == StudioTab.PROFILE || tab == StudioTab.HOME' in activity
assert 'StudioTab.HOME -> StudioHome(' in activity
assert 'onResumeSpotlight(it, recent)' in activity

# No new sources, analytics SDK, cloud schema, filesystem access or service.
assert 'nightwave_listening_memory_v1' not in cloud
assert 'nightwave_listening_memory_v1' not in library
assert 'dailyUtc' not in worker and 'dailyUtc' not in cloud
assert 'listeningMemory.record(pulse, revision)' in service
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert ':app:compileDebugAndroidTestKotlin' in ci
assert 'tools/verify_v2_phase6c2.py' in ci
assert trend_tests.count("@Test") >= 7
assert mix_tests.count("@Test") >= 5
assert store_tests.count("@Test") >= 5
assert 'assumeTrue(' in store_tests and 'Build.FINGERPRINT' in store_tests

for name, text in (("store", store), ("trend", trend),
                   ("trend UI", trend_ui), ("memory UI", memory_ui),
                   ("mixes", mixes), ("activity", activity)):
    assert text.count("{") == text.count("}"), name + " unmatched curly brace"
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: 6C2 local 7/30-day UTC trends, opt-in saved-only recommendations, privacy & caps")
print("UNVERIFIED: Android Gradle/JVM, instrumented tests, real playback/sync, APK signer and screen UX")
