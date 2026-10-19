#!/usr/bin/env python3
"""NW-05 integrated playback safety source assertions.

This cannot substitute for Kotlin compilation, Gradle tests, device or UI QA.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def load(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

service = load("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
bridge = load("app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt")
ui = load("app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt")
seek_ui = load("app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekTimeline.kt")
eq_ui = load("app/src/main/java/com/nightwave/player/v2/ui/NightWaveSoundStudio.kt")
eq_policy = load("app/src/main/java/com/nightwave/player/playback/SoundStudioPolicy.kt")
queue_policy = load("app/src/main/java/com/nightwave/player/playback/PublicQueueSelectionPolicy.kt")
seek_policy = load("app/src/main/java/com/nightwave/player/v2/playback/PlaybackSeekIdentityPolicy.kt")
qtest = load("app/src/test/java/com/nightwave/player/playback/PublicQueueSelectionPolicyTest.kt")
stest = load("app/src/test/java/com/nightwave/player/v2/playback/PlaybackSeekIdentityPolicyTest.kt")
eqtest = load("app/src/test/java/com/nightwave/player/playback/SoundStudioPolicyTest.kt")
workflow = load(".github/workflows/android.yml")

# Service and its queue share the ORIGINAL tapped URL after deduplicating.
assert "PublicQueueSelectionPolicy.selectedIndex(entries, proposedQueue, selected)" in service
assert "val selectedEntry = proposedQueue.getOrNull(resolvedSelectionIndex)" in service
assert "queue.index == resolvedSelectionIndex" in service
assert "fun selectedIndex(original: List<QueueEntry>" in queue_policy
assert qtest.count("@Test") >= 4
assert "originalIndexResolvesAfterEarlierDuplicateRowsRemoved" in qtest

# Touches, drag releases and accessibility-seek events have a stable song ID.
assert "trackIdentity: String" in seek_ui
assert ".pointerInput(enabled, trackIdentity)" in seek_ui
assert "commitCallback(trackIdentity," in seek_ui
assert "trackIdentity = state.videoUrl" in ui
assert "actions.seekToForTrack(originatingTrack, it)" in ui
assert "fun seekToForTrack(expectedMediaId: String, positionMs: Long)" in bridge
assert "PlaybackSeekIdentityPolicy.accepts(" in bridge
assert "controller?.currentMediaItem?.mediaId" in bridge
assert "expected == shown && expected == mediaId" in seek_policy
assert stest.count("@Test") >= 4

# Manual EQ controls never offer outside of the service's validated ±15 dB.
assert "fun safeManualRange(deviceMinimumMb: Int, deviceMaximumMb: Int): IntRange?" in eq_policy
assert "maxOf(deviceMinimumMb, -1500)" in eq_policy
assert "minOf(deviceMaximumMb, 1500)" in eq_policy
assert "SoundStudioPolicy.safeManualRange(" in eq_ui
assert "manualRange.first" in eq_ui and "manualRange.last" in eq_ui
assert "values.all { it in -1500..1500 }" in service
assert "manualRangeNeverOffersValuesRejectedByPlaybackService" in eqtest

# Retain all earlier phase checks, not just the newest screenshot polish.
for phase in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub", "nw04_premium_player",
              "nw05_integrated_stability"):
    assert f"python3 tools/verify_{phase}.py" in workflow
assert "fix/nightwave-v2-nw05-integration-stability" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

for name, text in (("service", service), ("bridge", bridge), ("player", ui),
                   ("seek", seek_ui), ("eq", eq_ui)):
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), f"{name}: duplicate Kotlin imports"
    assert text.count("{") == text.count("}"), f"{name}: mismatched braces"

print("PASS: NW-05 source contract — queue selection, track-safe seeking and safe EQ range")
print("UNVERIFIED: Kotlin compile, Gradle unit tests, Google/EQ/streaming device acceptance")
