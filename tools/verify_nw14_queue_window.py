#!/usr/bin/env python3
"""NW14 bounded queue selection source and test contracts.

These are source checks, NOT a substitute for Kotlin/JUnit/Android execution.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")

selection = read("app/src/main/java/com/nightwave/player/v2/playback/QueueSelection.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/playback/QueueSelectionRulesTest.kt")
bridge = read("app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt")
service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
workflow = read(".github/workflows/android.yml")

# Normalize and deduplicate BEFORE determining a selected song's position.
assert "val selectedUrl = VideoLink.normalize(selected.url)" in selection
assert "collection.mapNotNull { song ->" in selection
assert "}.distinctBy { it.url }" in selection
assert "val selectedAt = unique.indexOfFirst { it.url == selectedUrl }" in selection
assert "selectedAt < 0 -> listOf(selected.copy(url = selectedUrl)) +" in selection
assert "unique.size <= capacity -> unique" in selection
assert "val start = (selectedAt - capacity / 2)" in selection
assert ".coerceIn(0, unique.size - capacity)" in selection
assert "unique.subList(start, start + capacity)" in selection
assert "return QueueSelection(bounded, bounded.indexOfFirst { it.url == selectedUrl })" in selection
# Confirm playback uses this policy before constructing a service Intent.
assert "QueueSelectionRules.plan(video, collection)" in bridge
assert "putExtra(PlaybackService.EXTRA_INDEX, index)" in bridge
assert "val entries = urls.take(100).mapIndexedNotNull" in service
assert "val selected = intent.getIntExtra(EXTRA_INDEX, 0)" in service

# Source-level coverage is not counted as passing JUnit execution.
assert tests.count("@Test") >= 11
for case in (
    "selectedTrackBeyondCapIsNeverLost",
    "largeCollectionKeepsRealNextAndPreviousAroundSelection",
    "selectingLastSongKeepsItsPreviousNeighbour",
    "earlySelectionKeepsOriginalQueueHeadAndOrder",
    "selectedSongOutsideCollectionStaysFirstAndPlayable",
    "invalidAndDuplicateEntriesDoNotShiftSelectionToWrongSong",
    "capacityOneStillSelectsRequestedTrackNotSomeOtherSong",
    "malformedSelectionDoesNotAutoplayUnrelatedTrack",
):
    assert case in tests, "Missing queue selection test: " + case

assert "fix/nightwave-v2-nw14-large-queue-window" in workflow
assert "python3 tools/verify_nw14_queue_window.py" in workflow
for phase in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub",
              "nw04_premium_player", "nw05_integrated_stability",
              "nw06_discovery_search", "nw07_release_gate",
              "nw08_attestation", "nw09_device_reliability",
              "nw11_junit_source", "nw12_integration",
              "nw13_queue_navigation"):
    assert f"python3 tools/verify_{phase}.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
print("PASS: NW14 contiguous queue selection source contracts and eleven JUnit cases")
print("UNVERIFIED: current JVM compilation/JUnit run, Android APK and Samsung tests")
