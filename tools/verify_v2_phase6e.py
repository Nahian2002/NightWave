#!/usr/bin/env python3
"""Phase 6E structural source gate. NOT Gradle, JVM or Android runtime testing."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
policy = read(base + "v2/playback/PlaybackHealthPolicy.kt")
panel = read(base + "v2/ui/PlaybackHealthPanel.kt")
player = read(base + "v2/ui/StudioPlayerScreens.kt")
activity = read(base + "v2/ui/NightWaveStudioActivity.kt")
service = read(base + "playback/PlaybackService.kt")
unit = read("app/src/test/java/com/nightwave/player/v2/playback/PlaybackHealthPolicyTest.kt")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")

# Playback phases are truthful and must never leak source URLs or
# invent success/error details. The health policy is pure Kotlin.
assert "enum class PlaybackPhase" in policy
assert "DISCONNECTED" in policy and "ERROR" in policy
assert "LOOKING_UP" in policy and "BUFFERING" in policy
assert "PLAYING" in policy and "PAUSED" in policy
assert "if (!connected)" in policy
assert "if (!error.isNullOrBlank())" in policy
assert "if (resolving && !offline)" in policy
assert "if (buffering)" in policy
assert "if (playing)" in policy
assert "val resolving: Boolean = false" in bridge
assert "resolving = resolving," in bridge
assert "state.resolving" in bridge
assert "resolving = false," in bridge
assert "val resolving = media == null && persistence.lookupPending(savedTrack?.url)" in bridge

# One attempt only, foreground-only, no automatic media commands.
assert "private var foregroundReconnectAvailable = true" in bridge
assert "private val oneShotReconnect = Runnable {" in bridge
assert "if (!released && ticking && foregroundReconnectAvailable" in bridge
assert "foregroundReconnectAvailable = false" in bridge
assert "main.postDelayed(oneShotReconnect, 1_200L)" in bridge
assert "main.removeCallbacks(oneShotReconnect)" in bridge
assert "foregroundReconnectAvailable = true" in bridge
assert "if (controller == null && future == null) connect()" in bridge
assert "fun release()" in bridge
callback = bridge.split("private val oneShotReconnect = Runnable {", 1)[1].split("\n    }", 1)[0]
for forbidden in ("play()", "resumeCurrent()", "ACTION_RESUME_SAVED", "resolveAndPlay("):
    assert forbidden not in callback, forbidden

# All user-visible states and health panel use the same policy.
assert "PlaybackHealthPolicy.classify(" in player
assert "health.phase == PlaybackPhase.LOOKING_UP" in player
assert "health.phase == PlaybackPhase.BUFFERING" in player
assert "health.phase == PlaybackPhase.DISCONNECTED" in player
assert "health.phase == PlaybackPhase.ERROR" in player
assert "Text(health.title" in player
assert "fun PlaybackHealthPanel(" in panel
assert "onRecover: () -> Unit" in panel
assert "OutlinedButton(onClick = onRecover" in panel
assert "This panel" in panel
assert "state.queue.size" in panel
assert "Source: imported local audio" in panel
assert "playbackState = playerState" in activity
assert "onRecoverPlayback = { playerActions.retry() }" in activity
assert 'StudioProfileGroup("App health")' in activity
assert "PlaybackHealthPanel(playbackState, onRecoverPlayback)" in activity

# Keep 6D bounded resolver and app identity unchanged.
assert "PlaybackResolutionPolicy.acceptsResult" in service
assert "cancelPendingResolution()" in service
assert "listeningMemory.record(pulse, revision)" in service
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert unit.count("@Test") >= 8
assert "tools/verify_v2_phase6e.py" in workflow
assert ":app:compileDebugAndroidTestKotlin" in workflow

for name, source in (("bridge", bridge), ("policy", policy),
                     ("panel", panel), ("player", player),
                     ("activity", activity)):
    assert source.count("{") == source.count("}"), name + " unbalanced braces"
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: 6E state consistency, foreground-once reconnect, private playback health UI")
print("PENDING: Android Gradle, JUnit, emulator/disconnect, device and signing QA")
