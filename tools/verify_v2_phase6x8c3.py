#!/usr/bin/env python3
"""Regression gate for the two 2026-10-19 Android Kotlin compiler failures.

These are source/model contract checks. A real Gradle compilation is still
required before publishing, installing or labeling an APK release-ready.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")

service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
smart = read("app/src/main/java/com/nightwave/player/v2/library/SmartLibraryPolicy.kt")
model = read("app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/library/SmartLibraryPolicyTest.kt")
preflight = read("tools/check-kotlin-compat.ps1")
workflow = read(".github/workflows/android.yml")

assert "androidx.media3:media3-session:1.6.1" in read("app/build.gradle.kts")
assert "MediaSession.ConnectionResult.AcceptedResultBuilder(session)" in service
assert "AcceptedResultBuilder(session, controller)" not in service
assert service.count("MediaLibrarySession.Builder(this, player,") == 1
assert "setAvailableSessionCommands(commands)" in service
assert "class PlaybackService : MediaLibraryService()" in service
assert "override fun onCustomCommand(" in service
assert "onMediaButtonEvent(" in service
assert service.count("ExoPlayer.Builder(") == 2

assert "data class VideoPreview(" in model
assert "val durationSeconds: Long" in model
assert "it.durationSeconds >= 240L" in smart
assert "it.duration >= 240L" not in smart
assert "MAX_COLLECTIONS = 8" in smart
assert "Long listens" in smart
assert "longListenUsesActualDurationSecondsFieldAndFourMinuteBoundary" in tests
assert "unknownOrNegativeDurationCannotBeCalledLongMusic" in tests

assert "AcceptedResultBuilder" in preflight
assert "durationSeconds" in preflight
assert "VideoPreview.duration" in preflight
assert "python3 tools/verify_v2_phase6x8c3.py" in workflow
assert "python3 tools/verify_v2_phase6x5.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

for name, source in (("service", service), ("smart", smart), ("tests", tests)):
    assert source.count("{") == source.count("}"), name + " braces"
    imports = [s for s in source.splitlines() if s.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"

print("PASS: Media3 builder and VideoPreview.durationSeconds source compatibility contracts")
print("UNVERIFIED: Gradle compile, JVM tests, Android instrumentation, device playback and signer")
