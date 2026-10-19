#!/usr/bin/env python3
"""Phase 6X3 static integration gate. Not a substitute for Android compilation."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
base = "app/src/main/java/com/nightwave/player/"
def file(path):
    return (root / path).read_text(encoding="utf-8")

policy = file(base + "playback/OfflineNormalizationPolicy.kt")
store = file(base + "playback/OfflineNormalizationStore.kt")
analyzer = file(base + "playback/OfflineLoudnessAnalyzer.kt")
service = file(base + "playback/PlaybackService.kt")
activity = file(base + "v2/ui/NightWaveStudioActivity.kt")
player = file(base + "v2/ui/StudioPlayerScreens.kt")
tests = file("app/src/test/java/com/nightwave/player/playback/OfflineNormalizationPolicyTest.kt")
workflow = file(".github/workflows/android.yml")

assert "TARGET_RMS = 0.13f" in policy
assert "MAX_ANALYSIS_WALL_MS = 3_000L" in policy
assert "MAX_SAMPLES = 1_500_000L" in policy
assert "fun attenuation(" in policy
assert "fun combined(" in policy
assert "listeningGain.isFinite()" in policy
assert "getSharedPreferences(" in store and "nightwave_offline_normalization_v1" in store
assert 'getBoolean("enabled", false)' in store
assert 'prefs.getLong("bytes_$id", -1L) != bytes' in store
assert "OfflineNormalizationPolicy.accepts(" not in service
assert "OfflineNormalizationPolicy.gain(" in service
assert "OfflineNormalizationPolicy.combined(" in service
assert "private val normalizationWorker = ThreadPoolExecutor(" in service
assert "1, 1, 30L" in service
assert "normalizationStore.savedGain(id, song.bytes)" in service
assert "OfflineLoudnessAnalyzer().measure(file)" in service
assert "val file = song?.let { library.fileFor(it) }" in service
assert "normalizationStore.saveGain(id, song.bytes, measured)" in service
assert "normalizationEpoch++" in service
assert "ACTION_NORMALIZATION_SET ->" in service
assert "normalizationStore.setEnabled(enabled)" in service
assert "normalizationWorker.shutdownNow()" in service
assert "if (crossfadeStartedAtMs > 0L) return" in service
assert "normalizedMaster(player.currentMediaItem?.mediaId)" in service
assert "normalizedMaster(incoming.currentMediaItem?.mediaId)" in service
assert "normalizedMaster(new.currentMediaItem?.mediaId)" in service
assert "normalizedMaster(exoPlayer?.currentMediaItem?.mediaId)" in service
assert "offlinePlaying = false\n        offlineGaplessActive = false" in service
assert "applyCurrentMaster() // clear offline attenuation before public source resolution" in service
assert "MediaExtractor()" in analyzer
assert "MediaCodec.createDecoderByType(mime)" in analyzer
assert "SystemClock.elapsedRealtime() - startMs" in analyzer
assert "AudioFormat.ENCODING_PCM_16BIT" in analyzer
assert "AudioFormat.ENCODING_PCM_FLOAT" in analyzer
assert "finally {" in analyzer and "codec?.release()" in analyzer
assert "normalizationEnabled = normalizationStore.enabled()" in activity
assert "PlaybackService.ACTION_NORMALIZATION_SET" in activity
assert "onNormalizationChange = onNormalizationChange" in activity
assert "offline-volume-normalization-switch" in player
assert "onCheckedChange = onNormalizationChange" in player
assert tests.count("@Test") >= 8
assert "python3 tools/verify_v2_phase6x3.py" in workflow
assert "python3 tools/verify_v2_phase6x2.py" in workflow
for label,src in (("policy",policy),("store",store),("analyzer",analyzer),
                  ("service",service),("activity",activity),
                  ("player",player),("tests",tests)):
    assert src.count("{") == src.count("}"), label + " braces"
    imports = [line for line in src.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), label + " duplicate imports"
print("PASS: 6X3 offline bounded attenuation source integration; no boosted output, no server")
print("PENDING: full Android Gradle build, JUnit execution, and actual device loudness audition")
