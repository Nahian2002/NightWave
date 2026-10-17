#!/usr/bin/env python3
"""NightWave 5A source sanity gate. Not Android compilation or runtime QA."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
source = root / "app/src/main/java/com/nightwave/player"
read = lambda path: path.read_text(encoding="utf-8")
service = read(source / "playback/PlaybackService.kt")
queue = read(source / "playback/QueueStateStore.kt")
profile = read(source / "v2/data/GuestProfileStore.kt")
studio = read(source / "v2/ui/NightWaveStudioActivity.kt")
offline = read(source / "offline/OfflineAudioLibrary.kt")
offline_screen = read(source / "offline/OfflineLibraryActivity.kt")
app = read(source / "NightWaveApp.kt")
diagnostics = read(source / "diagnostics/NightWaveCrashRecorder.kt")
classic = read(source / "ui/MainActivity.kt")
gradle = read(root / "app/build.gradle.kts")

assert 'versionName = "2.0.0-alpha5m"' in gradle
assert 'versionCode = 29' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
start = service.split("override fun onCreate()", 1)[1].split(
    "override fun onStartCommand", 1
)[0]
assert "savedQueue.restore(queue)" in start
assert "resolveAndPlay(" not in start
assert "main.postDelayed(progressWriter" not in start
assert 'ACTION_RESUME_SAVED' in service
assert 'KEYCODE_MEDIA_PLAY' in service and 'KEYCODE_HEADSETHOOK' in service
assert 'if (!player.isPlaying) return' in service
assert 'main.removeCallbacks(progressWriter)' in service
assert 'if (resolver.isShutdown) return' in service
assert 'RejectedExecutionException' in service
assert 'if (request != generation || exoPlayer == null) return@post' in service
assert 'if (!::savedQueue.isInitialized' in service
assert 'if (autoPlay) runCatching { MusicLibrary(this).addRecent(video) }' in service
assert "ClassCastException" in queue
assert 'return try {' in queue
assert 'val values = prefs.all' in profile
assert 'fun string(key: String, fallback: String)' in profile
assert 'val existingId = prefs.all["local_id"] as? String ?: return false' in profile
assert 'refreshShadows()' in studio
refresh = studio.split('private fun refresh() {', 1)[1].split(
    'private fun enableVerifiedRoomPreview()', 1
)[0]
assert "refreshShadows()" not in refresh
assert 'fun fileFor(song: OfflineSong): File? = try' in offline
assert 'runCatching {' in offline_screen
assert 'NightWaveCrashRecorder.install(this)' in app
assert 'NightWaveCrashRecorder.report(this)' in classic
assert 'previous.uncaughtException(thread, throwable)' in diagnostics
assert 'MAX_RECORDS = 5' in diagnostics
assert 'getString(KEY, "").orEmpty()' in diagnostics
assert 'it.className.startsWith("com.nightwave.player.")' in diagnostics
assert 'throwable.message' not in diagnostics and 'printStackTrace' not in diagnostics

for path, count in (
    ("app/src/androidTest/java/com/nightwave/player/v2/data/GuestProfileStartupInstrumentedTest.kt", 3),
    ("app/src/androidTest/java/com/nightwave/player/playback/QueueStateBookmarkInstrumentedTest.kt", 7),
    ("app/src/androidTest/java/com/nightwave/player/offline/OfflineAudioLibraryInstrumentedTest.kt", 4)
):
    assert read(root / path).count("@Test") >= count, path

manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
an = "{http://schemas.android.com/apk/res/android}name"
activities = {a.attrib.get(an) for a in manifest.iter("activity")}
assert ".ui.MainActivity" in activities and ".v2.ui.NightWaveStudioActivity" in activities
assert 'allowBackup="false"' in read(root / "app/src/main/AndroidManifest.xml")
assert 'fallbackToDestructiveMigration' not in read(
    source / "v2/data/room/NightWaveDatabase.kt"
).replace('// Never use fallbackToDestructiveMigration.', '')

for p, contents in (
    ("service", service), ("queue", queue), ("guest profile", profile),
    ("offline library", offline), ("diagnostics", diagnostics), ("studio", studio)
):
    assert contents.count("{") == contents.count("}"), p + " braces"
    imports = [line for line in contents.splitlines() if line.startswith("import ")]
    assert len(set(imports)) == len(imports), p + " repeated imports"

print("PASS: deferred stream restoration, bounded progress writer, corrupt-state")
print("      safeguards, crash-summary delegation, and regression-test wiring")
print("PENDING: Gradle compilation, Android instrumentation and actual device crash QA")
