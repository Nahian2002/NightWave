#!/usr/bin/env python3
"""NW94 source-level integration checks; no Android or physical storage claims."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(name): return (ROOT / name).read_text(encoding="utf-8")
service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
store = read("app/src/main/java/com/nightwave/player/playback/QueueStateStore.kt")
policy = read("app/src/main/java/com/nightwave/player/playback/PublicBookmarkCheckpointPolicy.kt")
tests = read("app/src/test/java/com/nightwave/player/playback/PublicBookmarkCheckpointPolicyTest.kt")
workflow = read(".github/workflows/android.yml")
assert "PublicBookmarkCheckpointPolicy.shouldPersist(" in service
assert "url, queue.current?.url, offlinePlaying" in service
assert "savedQueue.saveBookmark(" in service
assert "player.currentPosition.coerceAtLeast(0L), duration, flush)" in service
assert "persistProgress(flush = true)" in service
assert "if (!offlinePlaying && queue.current?.url != selectedEntry?.url)" in service
assert "fun saveBookmark(" in store and "flush: Boolean = false" in store
assert "if (flush) editor.commit() else { editor.apply(); true }" in store
assert "fun save(queue: PlaybackQueue): Boolean" in store
assert 'return prefs.edit().putString("snapshot", state.toString()).commit()' in store
assert "val persisted = savedQueue.save(queue)" in service
assert "if (persisted && requestId.isNotBlank()" in service
assert "if (persisted && !requestId.isNullOrEmpty()" in service
assert "QueueRestorePolicy.isCanonicalPublicWatchUrl(loadedMediaId)" in policy
assert tests.count("@Test fun ") >= 6
assert "python3 tools/verify_nw94_playback.py" in workflow
assert "NW94PublicBookmarkSmoke.kt" in workflow
print("NW94_PLAYBACK_SOURCE_INTEGRATION_PASS")
print("Android APK, device and power-loss durability still require independent tests.")
