#!/usr/bin/env python3
"""NightWave 5P source-only queue swipe / Undo safety contract.

Android Gradle, Compose instrumentation and service interleaving still need
a device or Windows SDK build; source wiring alone does not verify those.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda path: (root / path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
queue = read(base + "playback/PlaybackQueue.kt")
service = read(base + "playback/PlaybackService.kt")
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
ui = read(base + "v2/ui/StudioPlayerScreens.kt")
studio = read(base + "v2/ui/NightWaveStudioActivity.kt")
offline = read(base + "offline/OfflineAudioLibrary.kt")
manifest = read("app/src/main/AndroidManifest.xml")
gradle = read("app/build.gradle.kts")
builder = read("tools/build-windows.ps1")
device = read("tools/check-device-update.ps1")
unit = read("app/src/test/java/com/nightwave/player/playback/PlaybackQueueUpcomingTest.kt")
android = read("app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveQueueSwipeInstrumentedTest.kt")

assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert "-VersionName '2.0.0-alpha5s' -VersionCode '35'" in builder
assert "$versionName = '2.0.0-alpha5s'" in device
assert '$versionCode = 35' in device
assert '$reportedVersion -ne $versionName' in device

# Queue-only pure Kotlin mutation, guarded by current song and expected url.
assert 'data class RemovedUpcoming' in queue
assert 'fun removeUpcoming(at: Int, expectedUrl: String): RemovedUpcoming?' in queue
assert 'fun restoreUpcoming(removed: RemovedUpcoming): Boolean' in queue
assert 'at <= index || at !in entries.indices' in queue
assert 'entries[at].url != expectedUrl' in queue
assert 'shuffle ||' in queue
assert 'current?.url != removed.playingUrl' in queue
assert 'entries.any { it.url == removed.entry.url }' in queue
assert 'remapForRemoval(at)' in queue
assert 'remapForInsertion(removed.at)' in queue

# The service, not the UI, is authoritative for public queue edits.
assert 'ACTION_REMOVE_UPCOMING' in service
assert 'ACTION_UNDO_REMOVE_UPCOMING' in service
assert 'EXTRA_EXPECTED_URL' in service and 'EXTRA_QUEUE_TOKEN' in service
assert 'pendingQueueRemoval = token to removed' in service
assert 'pending?.first == token' in service
assert 'pendingQueueRemoval = null // one-shot' in service
assert 'if (!offlinePlaying' in service
assert 'queue.removeUpcoming(at, expected)' in service
assert 'queue.restoreUpcoming(pending.second)' in service
assert 'savedQueue.save(queue)' in service
assert 'pendingQueueRemoval = null' in service.split('private fun playNext(automatic: Boolean)', 1)[1]
assert 'OfflineAudioLibrary(this).delete' not in service
assert 'file.delete()' not in service

# Controller only sends guarded requests; it cannot delete playlists/files.
assert 'fun removeUpcoming(index: Int, expectedUrl: String): String?' in bridge
assert 'fun undoUpcoming(token: String)' in bridge
assert 'state.offline || state.shuffle || !state.connected' in bridge
assert 'index <= state.queueIndex' in bridge
assert 'state.queue.getOrNull(index)?.url != expectedUrl' in bridge
assert 'UUID.randomUUID().toString()' in bridge
assert 'ACTION_REMOVE_UPCOMING' in bridge
assert 'ACTION_UNDO_REMOVE_UPCOMING' in bridge
assert 'scheduleQueueRefresh()' in bridge

# Swipe left and explicit accessibility affordance share the same callback.
assert 'fun StudioQueueSheet(' in ui
assert 'onRemoveUpcoming: (Int, String) -> String?' in ui
assert 'onUndoRemoval: (String) -> Unit' in ui
assert 'detectHorizontalDragGestures(' in ui
assert 'travel <= -72.dp.toPx()' in ui
assert 'SnackbarHostState()' in ui
assert 'SnackbarResult.ActionPerformed' in ui
assert 'actionLabel = "Undo"' in ui
assert 'contentDescription = "Remove ${title.take(36)} from Up Next"' in ui
assert 'index > state.queueIndex' in ui
assert '!state.offline && !state.shuffle && state.connected' in ui
assert 'modifier = Modifier.weight(1f, fill = false)' in ui
assert 'animationSpec = tween(durationMillis = if (reduceMotion) 0 else 130)' in ui
assert 'onRemoveUpcoming = { index, url -> playerActions.removeUpcoming(index, url) }' in studio
assert 'onUndoRemoval = { token -> playerActions.undoUpcoming(token) }' in studio
assert 'reduceMotion = profile.motion == MotionPreference.REDUCED' in studio

# Only transient queue state may change; original local files remain untouched.
assert 'nightwave_offline_v1' in offline
assert 'NightWaveStudioActivity::class.java' in service
assert manifest.count('android.intent.category.LAUNCHER') == 1
assert 'android:allowBackup="false"' in manifest
ET.parse(root / "app/src/main/AndroidManifest.xml")

assert unit.count('@Test') == 8
assert android.count('@Test') == 2
for name, content in (('queue', queue), ('service', service), ('bridge', bridge),
                      ('Up Next', ui), ('premium root', studio)):
    assert content.count('{') == content.count('}'), name + ': brace imbalance'
    imports = [line for line in content.splitlines() if line.startswith('import ')]
    assert len(imports) == len(set(imports)), name + ': duplicate imports'

print('PASS: Phase 5P guarded queue-only swipe removal, Undo token and data-preservation source contracts')
print('PENDING: Android Gradle, repository JUnit, Compose gesture tests and phone playback')
