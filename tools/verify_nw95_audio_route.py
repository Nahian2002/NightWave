#!/usr/bin/env python3
"""NW95 public headset command policy integration; not a device audio-focus test."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def load(path): return (root / path).read_text(encoding="utf-8")
svc = load("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
pol = load("app/src/main/java/com/nightwave/player/playback/AudioRouteTransportPolicy.kt")
tests = load("app/src/test/java/com/nightwave/player/playback/AudioRouteTransportPolicyTest.kt")
ci = load(".github/workflows/android.yml")

assert "AudioRouteTransportPolicy.decide(" in svc
assert "KeyEvent.KEYCODE_MEDIA_PLAY," in svc
assert "KeyEvent.KEYCODE_MEDIA_PAUSE," in svc
assert "KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE," in svc
assert "KeyEvent.KEYCODE_HEADSETHOOK ->" in svc
assert "event.repeatCount > 0" in svc
assert "AudioRouteTransportPolicy.Action.PAUSE_PENDING" in svc
assert "player.pause()" in svc.split("AudioRouteTransportPolicy.Action.PAUSE_PENDING",1)[1][:520]
assert "AudioRouteTransportPolicy.Action.RESUME_SAVED" in svc
assert "resumeSavedQueue()" in svc.split("AudioRouteTransportPolicy.Action.RESUME_SAVED",1)[1][:460]
assert "AudioRouteTransportPolicy.Action.DELEGATE -> false" in svc
assert "selectedCanonicalPublicSong = queue.current?.url?.let" in svc
assert "mediaItemLoaded = player.currentMediaItem != null" in svc
assert "offlinePlaying = offlinePlaying" in svc
assert "playWhenReady = player.playWhenReady" in svc
assert ".setHandleAudioBecomingNoisy(true)" in svc
assert "setAudioAttributes(" in svc
assert "if (offlinePlaying || mediaItemLoaded || !selectedCanonicalPublicSong)" in pol
assert "Key.TOGGLE -> if (playWhenReady) Action.PAUSE_PENDING else Action.RESUME_SAVED" in pol
assert tests.count("@Test fun ") >= 6
assert "python3 tools/verify_nw95_audio_route.py" in ci
assert "tools/NW95AudioRouteTransportSmoke.kt" in ci
print("NW95_AUDIO_ROUTE_SOURCE_WIRING_PASS")
