#!/usr/bin/env python3
"""6X8C5 source regression guard for the one failing 6X8C4 Gradle JUnit test.

Pure Kotlin queue navigation is also exercised separately in an isolated
compiler run. This static check is NOT a full Gradle build or a device test.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def read(p): return (root / p).read_text(encoding="utf-8")
queue = read("app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt")
tests = read("app/src/test/java/com/nightwave/player/offline/OfflinePlaybackQueueTest.kt")
modes = read("app/src/test/java/com/nightwave/player/offline/OfflinePlaybackQueueModesTest.kt")
ci = read(".github/workflows/android.yml")
assert "class OfflinePlaybackQueue(" in queue
assert "if (shuffle && target != index) pushHistory(index)" in queue
assert "shuffle && visited.isNotEmpty() -> visited.removeLast()" in queue
assert "shuffle -> return null" in queue
assert "index > 0 -> index - 1" in queue
assert "repeat == PlaybackQueue.Repeat.ALL -> ids.lastIndex" in queue
assert "if (shuffle) {" in queue
assert "if (index >= 0) pushHistory(index)" in queue
assert "unplayed.remove(to)" in queue
assert "if (to !in ids.indices) return null" in queue
assert "unplayed.removeAt(pick(unplayed.size)" in queue
assert "fun setShuffle(enabled: Boolean)" in queue
assert "visited.clear()" in queue
assert "MAX_ITEMS = 400" in queue
assert 'assertEquals("b", queue.previous())' in tests
assert tests.count("@Test fun") >= 10
assert "previousAfterManualJumpFollowsVisibleNonShuffledOrder" in tests
assert "nonShuffledJumpAfterNextDoesNotUseOldPlaybackHistory" in tests
assert "failedJumpsDoNotChangeIndexHistoryOrShuffleOrder" in tests
assert "previousWithRepeatAllWrapsOnlyAtFirstSongWhenNotShuffled" in tests
assert "previousFollowsActualShuffledHistory" in modes
assert modes.count("@Test fun") >= 8
assert "python3 tools/verify_v2_phase6x8c5.py" in ci
assert "python3 tools/verify_v2_phase6x4.py" in ci
assert "fix/nightwave-v2-phase6x8c5-offline-previous-queue-order" in ci
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in ci
for label, source in (("queue",queue),("tests",tests),("modes",modes)):
    assert source.count("{") == source.count("}"), label + " braces"
    imports = [x for x in source.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), label + " duplicate imports"
print("PASS: 6X8C5 sequential Previous / shuffled-history source contract and regression tests")
print("UNVERIFIED: full Gradle/JUnit 771+ suite and physical playback")
