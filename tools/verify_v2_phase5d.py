#!/usr/bin/env python3
"""Phase 5D source-only safety gate; Android Gradle / device QA remains mandatory."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
base = root / "app/src/main/java/com/nightwave/player"
read = lambda f: f.read_text(encoding="utf-8")
gradle = read(root / "app/build.gradle.kts")
codec = read(base / "offline/OfflineSessionCodec.kt")
checkpoint = read(base / "offline/OfflineSessionStore.kt")
queue = read(base / "offline/OfflinePlaybackQueue.kt")
service = read(base / "playback/PlaybackService.kt")
offline = read(base / "offline/OfflineLibraryActivity.kt")
studio = read(base / "v2/ui/NightWaveStudioActivity.kt")
player = read(base / "v2/ui/StudioPlayerScreens.kt")
bridge = read(base / "v2/playback/StudioPlaybackBridge.kt")
audio = read(base / "offline/OfflineAudioLibrary.kt")
tests = read(root / "app/src/test/java/com/nightwave/player/offline/OfflineSessionCodecTest.kt")
queue_tests = read(root / "app/src/test/java/com/nightwave/player/offline/OfflinePlaybackQueueTest.kt")
manifest = read(root / "app/src/main/AndroidManifest.xml")

assert 'versionCode = 23' in gradle
assert 'versionName = "2.0.0-alpha5g"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'private const val VERSION = "NWOS1"' in codec
assert 'MessageDigest.getInstance("SHA-256")' in codec
assert 'const val MAX_POSITION_MS' in codec
assert 'const val MAX_BYTES = 24_000' in codec
assert 'takeIf(::isValid)' in codec
assert 'private val safeId = Regex(' in codec
assert 'fun read(): OfflineSession?' in checkpoint
assert '"nightwave_offline_session_v1"' in checkpoint
assert 'fun saveSelection(' in checkpoint
assert 'fun savePosition(' in checkpoint
assert 'return runCatching {' in checkpoint
assert 'fun snapshot(): List<String>' in queue
assert 'fun jump(to: Int): String?' in queue
assert 'offlineSession = OfflineSessionStore(this)' in service
assert 'ACTION_RESUME_OFFLINE' in service
assert 'ACTION_JUMP_OFFLINE' in service
assert 'private fun resumeOfflineSession()' in service
assert 'saved.ids.filter { it in known }' in service
assert 'offlineSession.saveSelection(offlineQueue.snapshot(), id, resumePositionMs)' in service
assert 'offlineSession.savePosition(url.removePrefix("offline:"),' in service
assert 'persistProgress(flush = true)' in service
assert 'if (offlinePlaying) persistProgress(flush = true)' in service
assert 'ACTION_RESUME_OFFLINE' in offline and 'resumeControl.visibility' in offline
assert 'offlineResumeTitle' in studio and 'onResumeOffline = {' in studio
assert 'onClick = onResumeOffline' in studio
assert 'offlineQueueFor(id: String)' in bridge
assert 'queueIndex = if (offlineId != null) offlineList.second else current.index' in bridge
assert 'ACTION_JUMP_OFFLINE' in bridge
assert 'enabled = !state.offline' in player
assert 'private val root = File(context.applicationContext.filesDir, "offline_audio")' in audio
assert 'nightwave_offline_v1' in audio
assert 'QueueStateStore' not in checkpoint + codec
assert 'deleteRecursively' not in checkpoint + codec + service
assert tests.count('@Test') >= 6
assert queue_tests.count('@Test') >= 6
creation = service.split('override fun onCreate()', 1)[1].split('override fun onStartCommand', 1)[0]
assert 'resolveAndPlay(' not in creation
assert 'playOfflineId(' not in creation
assert 'ACTION_RESUME_OFFLINE explicitly' in creation
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1

for name, code in (
    ("codec", codec), ("checkpoint", checkpoint), ("queue", queue),
    ("Media3 service", service), ("offline UI", offline),
    ("Compose library", studio), ("player UI", player), ("bridge", bridge)
):
    assert code.count('{') == code.count('}'), name + ' brace mismatch'
    imports = [x for x in code.splitlines() if x.startswith('import ')]
    assert len(imports) == len(set(imports)), name + ' duplicate imports'

print('PASS: Phase 5D source wiring, explicit offline restoration and old-data boundaries')
print('PENDING: Android Gradle build, repository JUnit execution, instrumented/phone testing')
