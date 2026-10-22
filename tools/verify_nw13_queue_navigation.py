#!/usr/bin/env python3
"""NW-13 source regression preflight for public queue navigation.

This verifies that all repository tests and production transitions are present.
It does not execute JUnit or compile Android; those remain mandatory Gradle gates.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

q = read("app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt")
test = read("app/src/test/java/com/nightwave/player/playback/PlaybackQueueNavigationModeTest.kt")
old = read("app/src/test/java/com/nightwave/player/playback/PlaybackQueueTest.kt")
upcoming = read("app/src/test/java/com/nightwave/player/playback/PlaybackQueueUpcomingTest.kt")
offline = read("app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt")
ci = read(".github/workflows/android.yml")

assert "var shuffle: Boolean = false" in q
assert "if (field != value)" in q
assert "visited.clear()" in q and "shuffledSeen.clear()" in q
assert "if (shuffle && index >= 0 && index != target)" in q
assert "if (shuffle && index >= 0 && index != to)" in q
assert "if (shuffle) {" in q and "if (visited.isEmpty()) return null" in q
assert "if (index > 0) index--" in q
assert "if (index < 0) return null" in q
assert "else if (repeat == Repeat.ALL && entries.isNotEmpty()) index = entries.lastIndex" in q
assert "Only shuffled playback requires a visit stack" in offline
assert test.count("@Test") == 9
for case in ("unshuffledPreviousFollowsVisibleRowAfterJump",
             "invalidJumpCannotContaminatePrevious",
             "repeatedLinearNextPreviousUsesDisplayedSequence",
             "shuffleBacktracksActualHistory",
             "shuffleModeChangesDiscardOldHistory",
             "repeatAllWrapsPreviousWhenShuffleOff",
             "selectedDuplicateKeepsCorrectLinearPrevious",
             "emptyQueuePreviousStaysEmpty",
             "shuffledPreviousWithNoHistoryDoesNotGuessLinearTrack"):
    assert case in test
assert "assertEquals(e(2), q.previous())" in old
assert "removalMaintainsLinearOrderAfterEarlierJumps" in upcoming
assert "fix/nightwave-v2-nw13-public-queue-previous" in ci
assert "python3 tools/verify_nw13_queue_navigation.py" in ci
for phase in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub",
              "nw04_premium_player", "nw05_integrated_stability",
              "nw06_discovery_search", "nw07_release_gate",
              "nw08_attestation", "nw09_device_reliability",
              "nw11_junit_source", "nw12_integration"):
    assert f"python3 tools/verify_{phase}.py" in ci
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in ci
print("PASS: NW13 public queue linear/shuffled source and regression contracts")
print("UNVERIFIED: JUnit execution, full Compose/Media3 Android compilation and APK")
