#!/usr/bin/env python3
"""NW93 structural integration checks, not an Android build receipt."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def load(path):
    return (root / path).read_text(encoding="utf-8")

queue = load("app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt")
store = load("app/src/main/java/com/nightwave/player/playback/QueueStateStore.kt")
policy = load("app/src/main/java/com/nightwave/player/playback/ShuffleRecoveryPolicy.kt")
tests = load("app/src/test/java/com/nightwave/player/playback/ShuffleRecoveryPersistenceTest.kt")
ci = load(".github/workflows/android.yml")
assert "fun shuffleRecoveryState(): ShuffleRecoverySnapshot" in queue
assert "fun restoreShuffleRecovery(state: ShuffleRecoverySnapshot?)" in queue
assert "ShuffleRecoveryPolicy.validated(" in queue
assert "visited.clear()" in queue.split("fun restoreShuffleRecovery", 1)[1]
assert "forwardVisits.clear()" in queue.split("fun restoreShuffleRecovery", 1)[1]
assert "shuffledSeen.clear()" in queue.split("fun restoreShuffleRecovery", 1)[1]
assert "queue.shuffleRecoveryState()" in store
assert 'state.put("shuffle_history", JSONObject()' in store
assert "readShuffleRecovery(state.optJSONObject(\"shuffle_history\"))" in store
assert "queue.restoreShuffleRecovery(snapshot.shuffleRecovery)" in store
assert "val shuffleRecovery: ShuffleRecoverySnapshot? = null" in store
assert "array.length() > ShuffleRecoveryPolicy.MAX_HISTORY" in store
assert "(array.opt(i) as? String) ?: return null" in store
assert "stored.seen.distinct().size" in policy
assert "selectedUrl !in available" in policy
assert tests.count("@Test fun ") >= 6
assert "tools/verify_nw93_queue_recovery.py" in ci
assert "NW93ShuffleRecoverySmoke.kt" in ci
print("NW93_QUEUE_RECOVERY_SOURCE_WIRING_PASS")
