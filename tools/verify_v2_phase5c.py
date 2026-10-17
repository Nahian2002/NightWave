#!/usr/bin/env python3
"""NightWave Alpha 5C source wiring gate; does not compile an APK."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/nightwave/player"
read = lambda path: path.read_text(encoding="utf-8")
gradle = read(root / "app/build.gradle.kts")
service = read(src / "playback/PlaybackService.kt")
queue = read(src / "offline/OfflinePlaybackQueue.kt")
activity = read(src / "offline/OfflineLibraryActivity.kt")
bridge = read(src / "v2/playback/StudioPlaybackBridge.kt")
store = read(src / "offline/OfflineAudioLibrary.kt")
tests = read(root / "app/src/test/java/com/nightwave/player/offline/OfflinePlaybackQueueTest.kt")

assert 'versionCode = 20' in gradle
assert 'versionName = "2.0.0-alpha5d"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle

assert 'EXTRA_OFFLINE_IDS = "offline_ids"' in service
assert 'val ownedIds = runCatching { localLibrary.songs().map { it.id }.toSet() }' in service
assert 'offlineQueue.select(candidates, id)' in service
assert 'private fun advanceOffline(forward: Boolean)' in service
assert 'playOfflineId(id)' in service
assert 'if (state == Player.STATE_ENDED)' in service
assert 'if (exoPlayer?.playbackState == Player.STATE_ENDED)' in service
assert 'if (!advanceOffline(forward = false))' in service
assert 'if (offlinePlaying)' in service
assert 'offlineQueue.reset()' in service

assert 'private var searchText = ""' in activity
assert 'addTextChangedListener(object : TextWatcher' in activity
assert '"Search saved songs"' in activity
assert '"Sort offline music"' in activity
assert '"▶  Play all"' in activity
assert 'visibleSongs.map { it.id }' in activity
assert 'putStringArrayListExtra(PlaybackService.EXTRA_OFFLINE_IDS, ids)' in activity
assert 'private fun launchPlayback(song: OfflineSong)' in activity
assert 'filtered.asReversed()' in activity

assert 'class OfflinePlaybackQueue' in queue
assert 'const val MAX_ITEMS = 400' in queue
assert 'distinct().take(MAX_ITEMS)' in queue
assert 'fun next(): String?' in queue and 'fun previous(): String?' in queue
assert 'fun reset()' in queue
assert tests.count("@Test") >= 5

assert 'queue = if (offlineId != null) offlineList.first' in bridge
assert 'queue = if (state.offline) state.queue' in bridge
assert 'title = if (state.offline) state.title' in bridge
assert 'if (state.offline) state.durationMs' in bridge
assert 'private val root = File(context.applicationContext.filesDir, "offline_audio")' in store
assert 'fun fileFor(song: OfflineSong)' in store
assert 'require(prefs.edit().putString("items", encode(previous + song)).commit())' in store

# Keep the established public queue, lazy MediaSession startup and old namespaces.
creation = service.split("override fun onCreate()", 1)[1].split("override fun onStartCommand", 1)[0]
assert "savedQueue.restore(queue)" in creation
assert "resolveAndPlay(" not in creation
assert 'if (!url.startsWith("https://www.youtube.com/watch?v="))' in service
assert "nightwave_offline_v1" in store
assert "QueueStateStore" not in queue
assert "deleteRecursively" not in service + activity + store

for label, source in (
    ("offline queue", queue), ("offline UI", activity),
    ("Media3 service", service), ("Compose bridge", bridge)
):
    assert source.count("{") == source.count("}"), label + " braces"
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), label + " duplicate imports"

print("PASS: Phase 5C offline library filtering/sorting, local queue and scoped playback metadata")
print("PENDING: Android Gradle compilation, JUnit and actual phone/Bluetooth/offline device tests")
