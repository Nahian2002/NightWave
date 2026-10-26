#!/usr/bin/env python3
"""NW98 source wiring. Android compilation and device behavior remain separate."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]
def read(name): return (p/name).read_text(encoding="utf-8")
svc=read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
policy=read("app/src/main/java/com/nightwave/player/playback/PlaybackResolutionPolicy.kt")
widget=read("app/src/main/java/com/nightwave/player/widget/NightWaveWidgetController.kt")
wpolicy=read("app/src/main/java/com/nightwave/player/widget/WidgetPlaybackIntentPolicy.kt")
ci=read(".github/workflows/android.yml")
assert "shouldResumePendingExplicitPlay(" in svc
assert "explicitPlay = intent.action == ACTION_PLAY_QUEUE" in svc
assert "pendingMatching = resolving" in svc
assert "alreadyWantsPlayback = exoPlayer?.playWhenReady == true" in svc
assert "explicitPlay && sameSelection && !loaded" in policy
assert "pendingMatching && !alreadyWantsPlayback" in policy
assert "WidgetPlaybackIntentPolicy.command(" in widget
assert "playWhenReady = connected.playWhenReady" in widget
assert "WidgetPlaybackIntentPolicy.Command.PAUSE -> connected.pause()" in widget
assert "WidgetPlaybackIntentPolicy.Command.PLAY -> connected.play()" in widget
assert "WidgetPlaybackIntentPolicy.Command.NONE -> Unit" in widget
assert "showsPauseIcon(item != null, player?.playWhenReady == true)" in svc
assert "override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int)" in svc
assert "publishWidgetState()" in svc.split("override fun onPlayWhenReadyChanged",1)[1][:200]
assert "hasLoadedMedia && playWhenReady" in wpolicy
assert "if (isPlaying) main.postDelayed(progressWriter, 2500L)" in svc
assert "python3 tools/verify_nw98_background_intent.py" in ci
assert "NW98BackgroundIntentSmoke.kt" in ci
print("NW98_BACKGROUND_INTENT_SOURCE_WIRING_PASS")
