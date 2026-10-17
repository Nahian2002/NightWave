#!/usr/bin/env python3
"""Phase 6N3 source checks, not execution of Android Gradle or device tests."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
def source(p):return (r/p).read_text(encoding="utf-8")
base="app/src/main/java/com/nightwave/player/"
policy=source(base+"playback/ListeningLevelPolicy.kt")
store=source(base+"playback/ListeningLevelStore.kt")
service=source(base+"playback/PlaybackService.kt")
activity=source(base+"v2/ui/NightWaveStudioActivity.kt")
screen=source(base+"v2/ui/StudioPlayerScreens.kt")
tests=source("app/src/test/java/com/nightwave/player/playback/ListeningLevelPolicyTest.kt")
ci=source(".github/workflows/android.yml")
gradle=source("app/build.gradle.kts")
assert "presetsPercent = listOf(100, 85, 70)" in policy
assert "if (accepts(percent)) percent else DEFAULT_PERCENT" in policy
assert "nightwave_listening_level_v1" in store
assert "ListeningLevelPolicy.accepts(value)" in store
assert "ListeningLevelPolicy.accepts(percent)" in store
assert ".commit()" in store
assert "ACTION_LISTENING_LEVEL_SET" in service
assert "listeningLevelStore = ListeningLevelStore(this)" in service
assert "volume = ListeningLevelPolicy.gain(listeningLevelStore.read())" in service
assert "listeningLevelStore.save(percent)" in service
assert "exoPlayer?.volume = ListeningLevelPolicy.gain(percent)" in service
assert "setAudioAttributes(" in service
assert "setWakeMode(" in service
assert "setPlaybackSpeed(1f)" in service
assert "handledEndMediaId = ended.mediaId" in service
assert "playWhenReady = false" in service
assert "ListeningLevelStore(this)" in activity
assert "listeningLevelPercent = listeningLevelStore.read()" in activity
assert "PlaybackService.ACTION_LISTENING_LEVEL_SET" in activity
assert "onListeningLevel = onListeningLevel" in activity
assert "onListeningLevel(percent)" in screen
assert "ListeningLevelPolicy.presetsPercent.forEach" in screen
assert "Listening level · " in screen
assert "tools/verify_v2_phase6n3.py" in ci
assert "feature/nightwave-v2-phase6n2-transition-fencing" in ci
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert tests.count("@Test") >= 10
for name,data in (("policy",policy),("store",store),("service",service),
                  ("activity",activity),("screen",screen),("tests",tests)):
    assert data.count("{") == data.count("}"), name
    imports=[ln for ln in data.splitlines() if ln.startswith("import ")]
    assert len(imports)==len(set(imports)), name
print("PASS: 6N3 bounded non-boosting volume trim; service-owned and durable")
print("PENDING: Android compile/JVM runtime, actual audio routes, phone QA")
