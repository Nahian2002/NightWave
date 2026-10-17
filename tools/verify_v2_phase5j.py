#!/usr/bin/env python3
"""NightWave 5J source-level performance and compatibility gate.

This does NOT execute Android Gradle, PowerShell, emulators, or a phone.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda rel: (root / rel).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
gradle = read("app/build.gradle.kts")
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
policy = read(base + "v2/playback/PlayerSnapshotPolicy.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/playback/PlayerSnapshotPolicyTest.kt")
service = read(base + "playback/PlaybackService.kt")
offline = read(base + "offline/OfflineAudioLibrary.kt")
builder = read("tools/build-windows.ps1")
device = read("tools/check-device-update.ps1")
signer = read("tools/verify-apk-signature.ps1")
manifest = read("app/src/main/AndroidManifest.xml")

assert 'versionCode = 30' in gradle
assert 'versionName = "2.0.0-alpha5n"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert "-VersionName '2.0.0-alpha5n' -VersionCode '30'" in builder
assert "Version code: 30" in builder
assert "$versionName = '2.0.0-alpha5n'" in device
assert "$versionCode = 30" in device

# Media3 events must remain the authoritative immediate state synchronizer.
assert 'override fun onEvents(player: Player, events: Player.Events)' in bridge
assert 'updateFromPlayer(player)' in bridge
assert 'updatePlayhead(it)' in bridge
assert 'fun updatePlayhead(player: Player)' in bridge
assert 'private fun updateFromPlayer(player: Player)' in bridge
assert 'ticksSinceReconcile = PlayerSnapshotPolicy.RECONCILE_TICKS' in bridge
assert 'ticksSinceReconcile = 0' in bridge
assert 'PlayerSnapshotPolicy.needsFullRefresh(' in bridge
assert 'PlayerSnapshotPolicy.RECONCILE_TICKS' in bridge
assert 'main.postDelayed(this, 750L)' in bridge
assert 'val position = player.currentPosition.coerceAtLeast(0L)' in bridge

# The fast path must never parse a preference snapshot or enumerate local songs.
fast = bridge.split('private fun updatePlayhead(player: Player)', 1)[1].split(
    'private fun updateFromPlayer(player: Player)', 1)[0]
for costly in ('QueueStateStore(', 'OfflineSessionStore(', 'OfflineAudioLibrary(',
               '.read()', '.songs()', '.bookmark(', 'gateway.', '.resolve('):
    assert costly not in fast, f"Expensive I/O found in lightweight tick: {costly}"

# The full path must still read both offline and public queue state and
# preserve all original transport actions for rapid changes.
full = bridge.split('private fun updateFromPlayer(player: Player)', 1)[1]
assert 'val persistence = QueueStateStore(context)' in full
assert 'val current = persistence.read()' in full
assert 'offlineQueueFor(' in full
assert 'override fun refreshQueue()' in bridge
for action in ('override fun retry()', 'override fun previous()', 'override fun next()',
               'override fun jumpTo(index: Int)', 'override fun seekTo(positionMs: Long)'):
    assert action in bridge

assert 'const val RECONCILE_TICKS = 8' in policy
assert 'if (ticksSinceReconcile >= RECONCILE_TICKS - 1)' in policy
assert 'currentMediaId != null && currentMediaId != shownMediaId' in policy
assert 'currentMediaId == null && previouslyPlaying' in policy
assert tests.count('@Test') == 5

# Release + storage invariants and no privileged Android actions.
assert "apksigner.bat" in signer
assert "NightWave-device-update-check.txt" in device
assert "shell pm path $packageName" in device
assert 'nightwave_offline_v1' in offline
assert 'private fun resumeOfflineSession()' in service
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1

for label, kotlin in (('bridge', bridge), ('snapshot policy', policy), ('policy tests', tests)):
    assert kotlin.count('{') == kotlin.count('}'), label + ' braces'
    imports = [l for l in kotlin.splitlines() if l.startswith('import ')]
    assert len(imports) == len(set(imports)), label + ' duplicate imports'

print('PASS: Phase 5J lightweight playback UI ticker and release wiring source checks')
print('PENDING: Full Android build, repository JUnit, Compose runtime, device profiling')
