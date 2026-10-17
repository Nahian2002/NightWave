#!/usr/bin/env python3
"""6C static source contract only. Not a Kotlin, Android or playback test."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
meter = read(base + "v2/insights/ListeningSessionMeter.kt")
store = read(base + "v2/insights/ListeningMemoryStore.kt")
service = read(base + "playback/PlaybackService.kt")
activity = read(base + "v2/ui/NightWaveStudioActivity.kt")
panel = read(base + "v2/ui/ListeningMemoryPanel.kt")
cloud = read(base + "v2/cloud/CloudBackupCodec.kt")
backup = read(base + "v2/data/LibraryBackupCodec.kt")
test = read("app/src/test/java/com/nightwave/player/v2/insights/ListeningSessionMeterTest.kt")
ci = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")
manifest = read("app/src/main/AndroidManifest.xml")

# Opt-in before any collection. Disable/erase checked synchronously under same
# in-process lock as the playback-service write.
assert 'getBoolean("consent", false)' in store
assert 'fun setEnabled(enabled: Boolean)' in store
assert 'fun eraseAndDisable()' in store
assert 'prefs.edit().clear().commit()' in store
assert 'private val lock = Any()' in store
assert 'if (!prefs.getBoolean("consent", false)) return@synchronized false' in store
assert 'MAX_TRACKS = 250' in store
assert 'MAX_JSON_BYTES = 120_000' in store
assert 'fun snapshot(): ListeningMemorySummary' in store
assert 'offlineId.matches(value)' in store
assert 'VideoLink.normalize(value) == value' in store
assert '"nightwave_listening_memory_v1"' in store
assert 'trackId = current' in meter
assert 'MIN_PLAY_MS = 30_000L' in meter
assert 'FLUSH_MS = 10_000L' in meter
assert 'MAX_OBSERVED_GAP_MS = 5_000L' in meter
assert 'wasPlaying && prev != null' in meter
assert 'nowElapsedMs - prev' in meter
assert 'if (!playCredited && sessionMs >= MIN_PLAY_MS)' in meter
assert 'pendingQualifyingPlay = true' in meter
assert 'emitPending(nowWallMs)' in meter
assert 'fun reset()' in meter
assert 'fun finish(nowElapsedMs: Long' in meter

# Media3 playback service is the ONLY writer; no UI timing or network logging.
assert 'private val listeningMeter = ListeningSessionMeter()' in service
assert 'listeningMemory = ListeningMemoryStore(this)' in service
assert 'SystemClock.elapsedRealtime()' in service
assert 'playing = player.isPlaying && player.playbackState == Player.STATE_READY' in service
assert 'if (!::listeningMemory.isInitialized) return' in service
assert 'val revision = listeningMemory.consentRevision()' in service
assert 'if (revision != lastMemoryConsentRevision)' in service
assert 'listeningMeter.reset()' in service
assert 'if (!listeningMemory.isEnabled())' in service
assert 'putLong("revision", next)' in store
assert 'prefs.edit().clear().putLong("revision", next).commit()' in store
assert 'listeningMeter.reset()' in service
assert 'pulses.forEach { listeningMemory.record(it) }' in service
assert 'sampleListening()' in service
assert 'sampleListening(endSession = true)' in service
assert 'Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT' in service
assert 'main.postDelayed(this, 2500L)' in service

# Controls disclose limits; confirm erase before a destructive operation.
assert 'ListeningMemoryPanel(' in activity
assert 'onMemoryEnabledChange' in activity
assert 'confirmEraseListeningMemory()' in activity
assert 'setPositiveButton("Erase & turn off")' in activity
assert 'listeningMemoryStore.eraseAndDisable()' in activity
assert 'if (tab == StudioTab.PROFILE) onMemoryRefresh()' in activity
assert 'onMemoryRefresh = onMemoryRefresh' in activity
assert 'Switch(checked = memory.enabled' in panel
assert '30s+ sessions' in panel
assert 'Observed time' in panel
assert 'Most observed listening time' in panel
assert 'Very short or interrupted intervals may not be recorded' in panel
assert 'onErase' in panel

# No additions to backups/Google Drive or Android permissions/signing/app ID.
assert 'nightwave_listening_memory_v1' not in cloud
assert 'nightwave_listening_memory_v1' not in backup
assert 'nightwave_listening_memory_v1' not in read(base + "v2/cloud/NightWaveCloudWorker.kt")
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'android:allowBackup="false"' in manifest
assert test.count('@Test') >= 10
assert 'tools/verify_v2_phase6c.py' in ci
assert ':app:compileDebugAndroidTestKotlin' in ci

for name, source in (("meter", meter), ("store", store), ("service", service),
                     ("activity", activity), ("panel", panel)):
    assert source.count('{') == source.count('}'), name + " braces unbalanced"
    imports = [i for i in source.splitlines() if i.startswith('import ')]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: opt-in, consent-guarded bounded local listening aggregates & non-intrusive Media3 sampling")
print("PENDING: Android Gradle, JVM/instrumentation, lifecycle & in-place APK signing QA")
