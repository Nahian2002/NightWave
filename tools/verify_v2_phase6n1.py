#!/usr/bin/env python3
"""6N1 source and data-preservation checks; NOT Gradle/device verification."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def src(p): return (root / p).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
policy = src(base + "playback/OfflinePlaybackSpeedPolicy.kt")
prefs = src(base + "playback/OfflinePlaybackSpeedStore.kt")
service = src(base + "playback/PlaybackService.kt")
activity = src(base + "v2/ui/NightWaveStudioActivity.kt")
now = src(base + "v2/ui/StudioPlayerScreens.kt")
tests = src("app/src/test/java/com/nightwave/player/playback/OfflinePlaybackSpeedPolicyTest.kt")
workflow = src(".github/workflows/android.yml")
gradle = src("app/build.gradle.kts")

assert "val presetsPercent = listOf(75, 100, 125, 150, 200)" in policy
assert "if (accepts(percent)) percent else NORMAL_PERCENT" in policy
assert "context.applicationContext.getSharedPreferences(" in prefs
assert "nightwave_offline_speed_v1" in prefs
assert "OfflinePlaybackSpeedPolicy.accepts(saved)" in prefs
assert "if (!OfflinePlaybackSpeedPolicy.accepts(percent)) return false" in prefs
assert "prefs.edit().putInt(" in prefs and ".commit()" in prefs
assert "ACTION_OFFLINE_SPEED_SET" in service and "EXTRA_OFFLINE_SPEED_PERCENT" in service
assert "offlineSpeedStore = OfflinePlaybackSpeedStore(this)" in service
assert "offlineSpeedStore.save(percent) && offlinePlaying" in service
assert "exoPlayer?.setPlaybackSpeed(OfflinePlaybackSpeedPolicy.speed(percent))" in service
assert "setPlaybackSpeed(OfflinePlaybackSpeedPolicy.speed(offlineSpeedStore.read()))" in service
assert "setPlaybackSpeed(1f)" in service
assert "setMediaItem(item)" in service and "clearMediaItems()" in service
assert "playWhenReady = false" in service
assert "generation++" in service and "cancelPendingResolution()" in service
assert "offlineSpeedPercent = offlineSpeedStore.read()" in activity
assert "PlaybackService.ACTION_OFFLINE_SPEED_SET" in activity
assert "onOfflineSpeed: (Int) -> Unit" in activity
assert "onOfflineSpeed: (Int) -> Unit" in now
assert "if (state.offline)" in now
assert "OfflinePlaybackSpeedPolicy.presetsPercent.forEach" in now
assert "onOfflineSpeed(percent)" in now
assert "tools/verify_v2_phase6n1.py" in workflow
assert "feature/nightwave-v2-phase6m2-reviewed-playlist-import" in workflow
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert tests.count("@Test") >= 10

for name, data in (("policy", policy), ("prefs", prefs), ("service", service),
                   ("activity", activity), ("now", now), ("tests", tests)):
    assert data.count("{") == data.count("}"), name
    imports = [line for line in data.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name

print("PASS: 6N1 offline-only speed policy, service owner and normal public stream")
print("PENDING: Android compilation, JVM suite, signing and device verification")
