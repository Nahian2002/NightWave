#!/usr/bin/env python3
"""Phase 6N2 source guard; not an Android compile or device result."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
def src(relative): return (root/relative).read_text(encoding="utf-8")
base="app/src/main/java/com/nightwave/player/"
service=src(base+"playback/PlaybackService.kt")
policy=src(base+"playback/PlaybackTransitionPolicy.kt")
tests=src("app/src/test/java/com/nightwave/player/playback/PlaybackTransitionPolicyTest.kt")
ci=src(".github/workflows/android.yml")
gradle=src("app/build.gradle.kts")
assert "ended.generation == currentGeneration" in policy
assert "ended.offline == currentOffline" in policy
assert "ended.mediaId == loadedMediaId" in policy
assert "ended.mediaId == selectedMediaId" in policy
assert "isEnded &&" in policy
assert "!timerPaused" in policy and "!alreadyHandled" in policy
assert "val ended = PlaybackTransitionPolicy.Ended(" in service
assert "if (!sleepTimerPaused && exoPlayer?.playbackState == Player.STATE_ENDED)" in service
assert "PlaybackTransitionPolicy.shouldAdvance(" in service
assert "handledEndMediaId = ended.mediaId" in service
assert "handledEndMediaId = null" in service
assert "val localLibrary = OfflineAudioLibrary(this)" in service
assert "if (playOfflineId(id, localLibrary)) return true" in service
assert "generation == retryGeneration && !offlinePlaying" in service
assert "offlineSpeedStore.save(percent) && offlinePlaying" in service
assert "setPlaybackSpeed(1f)" in service
assert "savedQueue.restore(queue)" in service
assert "playWhenReady = false" in service
assert "tools/verify_v2_phase6n2.py" in ci
assert "feature/nightwave-v2-phase6n1-offline-playback-speed" in ci
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert tests.count("@Test") >= 12
for name, data in (("service",service),("policy",policy),("tests",tests)):
    assert data.count("{") == data.count("}"), name
    imports=[line for line in data.splitlines() if line.startswith("import ")]
    assert len(set(imports)) == len(imports), name
print("PASS: 6N2 transition identity/epoch fencing, one-shot end and offline file safety")
print("PENDING: full Android build and physical-device playback confirmation")
