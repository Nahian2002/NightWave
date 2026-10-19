#!/usr/bin/env python3
"""Phase 6X2 source integration gate. Does not substitute for Android compilation."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "app/src/main/java/com/nightwave/player/"
def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

policy = read(BASE + "playback/CrossfadePolicy.kt")
settings = read(BASE + "playback/CrossfadeSettingsStore.kt")
service = read(BASE + "playback/PlaybackService.kt")
activity = read(BASE + "v2/ui/NightWaveStudioActivity.kt")
player = read(BASE + "v2/ui/StudioPlayerScreens.kt")
tests = read("app/src/test/java/com/nightwave/player/playback/CrossfadePolicyTest.kt")

assert "DEFAULT_SECONDS = 0" in policy
assert "MAX_SECONDS = 10" in policy
assert "fun canPrepare(" in policy and "!offline || !gaplessTimeline" in policy
assert "fun gains(" in policy and "gain * (1f - t) to gain * t" in policy
assert "getSharedPreferences(" in settings
assert "CrossfadePolicy.accepts(seconds)" in settings
assert "ACTION_CROSSFADE_SET ->" in service
assert "crossfadeSettings.save(seconds)" in service
assert "CrossfadePolicy.canPrepare(" in service
assert "library.fileFor(indexed) == null" in service
# Phase 6X8 added headset-disconnect pause to both ExoPlayer builders.
# Don't require .build() immediately after the builder constructor.
assert "candidate = ExoPlayer.Builder(this)" in service
assert "candidate = ExoPlayer.Builder(this)\n                .setHandleAudioBecomingNoisy(true)\n                .build().apply {" in service
assert "runCatching { candidate?.release() }" in service
assert "runCatching { deck?.pause(); deck?.release() }" in service
assert "setAudioAttributes(AudioAttributes.Builder()" in service
assert "setMediaItems((0 until outgoing.mediaItemCount).map(outgoing::getMediaItemAt)" in service
assert "session.setPlayer(new)" in service
assert "crossfadeIncoming = null" in service
assert "old.removeListener(trackListener)" in service
assert "old.pause(); old.release()" in service
assert "sleepTimerPaused = true\n            cancelCrossfade()" in service
assert "private fun resolveAndPlay(" in service and "cancelCrossfade()" in service
assert "setMediaItems(timeline.first, timeline.second," in service
assert "RepeatModePolicy.resolve(" in service
assert "ACTION_CROSSFADE_SET" in activity
assert "crossfadeSeconds = crossfadeSettings.read()" in activity
assert "crossfade-seconds-slider" in player
assert 'else "${crossfadeDraft} seconds overlap"' in player
assert "Off by default." in player
assert tests.count("@Test") >= 4
for name, src in (("policy", policy), ("settings", settings),
                  ("service", service), ("activity", activity),
                  ("player", player), ("test", tests)):
    assert src.count("{") == src.count("}"), name + " mismatched brace count"
    imports = [line for line in src.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: Phase 6X2 bounded local dual-deck crossfade source wiring")
print("PENDING: Gradle Android compilation, JUnit, instrumentation and device audio testing")
