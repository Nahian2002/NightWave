#!/usr/bin/env python3
"""6K sleep timer source and preservation gate (not Android compilation)."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def source(relative):
    return (root / relative).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
policy = source(base + "playback/SleepTimerPolicy.kt")
store = source(base + "playback/SleepTimerStatusStore.kt")
service = source(base + "playback/PlaybackService.kt")
activity = source(base + "v2/ui/NightWaveStudioActivity.kt")
player = source(base + "v2/ui/StudioPlayerScreens.kt")
dial = source(base + "v2/ui/NightWaveSleepTimerDialog.kt")
bridge = source(base + "v2/playback/StudioPlaybackBridge.kt")
test = source("app/src/test/java/com/nightwave/player/playback/SleepTimerPolicyTest.kt")
gradle = source("app/build.gradle.kts")
workflow = source(".github/workflows/android.yml")

# The service is the sole timer owner, bounded to 1–1440 minutes and using
# monotonic elapsed time. UI does not start a second countdown to pause audio.
assert "object SleepTimerPolicy" in policy
assert "val choicesMinutes = listOf(15, 30, 45, 60)" in policy
assert "const val MIN_MINUTES = 1" in policy
assert "const val MAX_MINUTES = 24 * 60" in policy
assert "if (!accepts(minutes) || nowMs < 0L) return null" in policy
assert "fun dialMinutes(" in policy
assert "Long.MAX_VALUE - duration" in policy
assert "SystemClock.elapsedRealtime()" in service
assert "private var sleepTimerGeneration = 0" in service
assert "private fun clearSleepTimer()" in service
assert "sleepTimerRunnable?.let(main::removeCallbacks)" in service
assert "private fun setSleepTimer(minutes: Int)" in service
assert "sleepTimerGeneration != token" in service
assert "SleepTimerPolicy.expires(SystemClock.elapsedRealtime(), deadline)" in service
assert "generation++" in service
assert "cancelPendingResolution()" in service
assert "exoPlayer?.pause()" in service
assert "exoPlayer?.playWhenReady = false" in service
assert "persistProgress(flush = true)" in service
assert "if (!sleepTimerPaused && exoPlayer?.playbackState == Player.STATE_ENDED)" in service
assert "if (isPlaying) sleepTimerPaused = false" in service

# Unrequested service recreation must not automatically re-arm the timer or
# play a saved song. A status store is ephemeral and lacks any song IDs.
assert "clearSleepTimer()\n        eqCapabilities.clear()" in service
assert "savedQueue.clearLookup()" in service
assert "override fun onDestroy()" in service
assert "ACTION_SLEEP_TIMER_SET -> setSleepTimer(" in service
assert "ACTION_SLEEP_TIMER_CANCEL -> clearSleepTimer()" in service
assert "private val preferences =" in store
assert "nightwave_sleep_timer_ephemeral" in store
assert "deadline_elapsed_ms" in store
for forbidden in ("MusicLibrary", "YouTubeGateway", "MediaController", "audioUrl", "trackId"):
    assert forbidden not in store, forbidden + " leaked into timer status"
assert "SleepTimerStatusStore(this)" in activity
assert "sleepTimerStatus.remainingMs()" in activity
assert "delay(5_000L)" in activity
assert "sleepRemainingMs = sleepRemainingMs" in activity
assert "onSleepTimer = onSleepTimer" in activity
assert "PlaybackService.ACTION_SLEEP_TIMER_SET" in activity
assert "PlaybackService.ACTION_SLEEP_TIMER_CANCEL" in activity
assert "onSleepTimer: (Int?) -> Unit" in player
assert "NightWaveSleepTimerDialog(" in player
assert "onSleepTimer(minutes)" in player
assert "onSleepTimer(null)" in player
assert "SleepTimerPolicy.label(sleepRemainingMs)" in player
assert "SleepTimerPolicy.dialMinutes(" in dial
assert "SleepTimerPolicy.choicesMinutes.forEach" in dial
assert "Exact minutes (1–1440)" in dial
assert "if (valid) onSet(requireNotNull(minutes))" in dial
assert "Cancel active timer" in dial
assert "onCancel = {" in player
assert "onSet = { minutes ->" in player

# No new Media3 ownership, arbitrary background alarms, app-data migration or
# package/signing changes. No automated stream extraction on service start.
assert "private var exoPlayer: ExoPlayer? = null" in service
assert "onGetSession(controllerInfo: MediaSession.ControllerInfo)" in service
assert "MediaController" not in policy and "MediaController" not in store
assert "ACTION_PLAY_QUEUE" in bridge
assert "savedQueue.restore(queue)" in service
assert "playWhenReady = false" in service
assert "versionCode = 35" in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert test.count("@Test") >= 8
assert "tools/verify_v2_phase6k.py" in workflow
for name, text in (("policy", policy), ("store", store), ("service", service),
                   ("activity", activity), ("player", player), ("dial", dial), ("tests", test)):
    assert text.count("{") == text.count("}"), name + " unmatched braces"
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: 6K/6W9 bounded 1–1440 minute dial, service-owned timer, no auto next after expiry and preserved Media3 data")
print("PENDING: actual Android Gradle/JVM, notification/background timing and physical-device signing QA")
