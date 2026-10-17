#!/usr/bin/env python3
"""Phase 6G read-only source safety gate. This is NOT Android compilation."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(p):
    return (root / p).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
policy = read(base + "v2/playback/PlaybackCommandPolicy.kt")
service = read(base + "playback/PlaybackService.kt")
health = read(base + "v2/playback/PlaybackHealthPolicy.kt")
test = read("app/src/test/java/com/nightwave/player/v2/playback/PlaybackCommandPolicyTest.kt")
windows = read("build-windows.cmd")
report = read("send-build-report.cmd")
diagnostics = read("tools/diagnose-build.ps1")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")

# The Activity/controller bridge may only play its *selected* media ID;
# a merely non-null MediaItem may be an unrelated public video or offline file.
assert "object PlaybackCommandPolicy" in policy
assert "selectedId == loadedId" in policy
assert "!playerHasError" in policy
assert "private fun selectedMediaId(): String?" in bridge
assert "state.queue.getOrNull(state.queueIndex)?.url" in bridge
assert "PlaybackCommandPolicy.canResumeLoaded(" in bridge
assert "selectedMediaId(), active.currentMediaItem?.mediaId" in bridge
assert "selectedMediaId(), it.currentMediaItem?.mediaId" in bridge
assert "else requestSelectedResume()" in bridge
assert "action(PlaybackService.ACTION_RESUME_SAVED)" in bridge
assert "action(PlaybackService.ACTION_RESUME_OFFLINE)" in bridge
# 6H tightened the old unconditional "already playing" shortcut: it may
# be idempotent only when the selected queue ID equals the current MediaItem.
assert "PlaybackCommandPolicy.resumeSatisfied(" in bridge
assert "selectedMediaId(), active.currentMediaItem?.mediaId" in bridge
assert "active.playerError != null, active.isPlaying" in bridge
assert "active.play()" in bridge
assert "private fun resumeSavedQueue()" in service
assert "PlaybackResolutionPolicy.reusesLoadedTrack(" in service
assert "val selectedId = offlineQueue.current ?: offlineSession.read()?.selectedId" in service
assert 'loadedId == "offline:$selectedId"' in service
assert "resumeOfflineSession()" in service
assert "private fun playOfflineId(" in service

# One offline, privacy-sanitized diagnostic rather than manually sharing many
# unredacted build logs. Does not collect app/device data or send anything.
assert "tools\\diagnose-build.ps1" in report
assert "dist\\NightWave-build-diagnostic.txt" in report
assert "does not use ADB" in report
assert "send-build-report.cmd" in windows
assert "NightWave-build-diagnostic.txt" in windows
assert "URL token replacement must preserve non-secret query parameters" in diagnostics
assert "multiple URL query credentials must be redacted independently" in diagnostics
assert "access_token=<redacted>" in diagnostics
assert "Authorization:" in diagnostics
assert "NightWave build recovery summary (Phase 6G)" in diagnostics
assert "NightWave-build-diagnostic.txt" in diagnostics

# Preserve previously added Health/6D playback, signing, identity and cloud.
assert "object PlaybackHealthPolicy" in health
assert "cancelPendingResolution()" in service
assert "listeningMemory.record(pulse, revision)" in service
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert ":app:compileDebugAndroidTestKotlin" in workflow
assert "tools/verify_v2_phase6g.py" in workflow
assert test.count("@Test") >= 8
for label, source in (
    ("Bridge", bridge), ("Policy", policy), ("Service", service),
    ("New unit tests", test)
):
    assert source.count("{") == source.count("}"), label + " brace mismatch"
    imports = [row for row in source.splitlines() if row.startswith("import ")]
    assert len(imports) == len(set(imports)), label + " duplicate imports"
print("PASS: 6G media-ID-safe play commands, offline Retry and offline redacted build reporting")
print("PENDING: Android/Compose compile, full JVM tests, device playback, signature QA")
