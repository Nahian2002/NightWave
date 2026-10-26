#!/usr/bin/env python3
"""NW97 evidence-backed source contracts, not a complete Android build."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
q = read("app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt")
g = read("app/src/main/java/com/nightwave/player/offline/OfflineGaplessPolicy.kt")
svc = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
tests = read("app/src/test/java/com/nightwave/player/offline/OfflineShuffleCycleIntegrityTest.kt")
ci = read(".github/workflows/android.yml")

start = q.index("fun previous(): String?")
end = q.index("fun snapshot(): List<String>", start)
previous = q[start:end]
assert "forwardVisits.add(index)" in previous
assert "unplayed.remove(target)" in previous
assert "unplayed.add(index)" not in previous
assert "if (index !in unplayed) unplayed.add(index)" not in q
assert "forwardVisits.clear() // explicit new branch" in q
assert "if (unplayed.isEmpty() && repeat == PlaybackQueue.Repeat.ALL)" in q
assert "if (automatic && repeat == PlaybackQueue.Repeat.ONE)" in q
assert "selectedIndex(ids, ids[currentIndex]) == null" in g
assert "currentIndex !in ids.indices" in g
assert "if (target == currentIndex) return null" in g
assert "if (!shuffled && target != currentIndex + 1" in g
assert "OfflineGaplessPolicy.transitionIndex(" in svc
assert "player.seekTo(next, 0L)" in svc
assert tests.count("@Test fun ") >= 8
assert "python3 tools/verify_nw97_offline_transitions.py" in ci
assert "NW97PlaybackTransitionSmoke.kt" in ci
print("NW97_OFFLINE_TRANSITION_SOURCE_WIRING_PASS")
