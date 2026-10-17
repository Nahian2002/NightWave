#!/usr/bin/env python3
"""NightWave Alpha 5F source-only safety gate; does not build Android."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/nightwave/player"
read = lambda path: path.read_text(encoding="utf-8")
gradle = read(root / "app/build.gradle.kts")
player = read(src / "v2/ui/StudioPlayerScreens.kt")
gesture = read(src / "v2/ui/PlayerGestureRules.kt")
progress = read(src / "v2/ui/PlayerProgressRules.kt")
bridge = read(src / "v2/playback/StudioPlaybackBridge.kt")
studio = read(src / "v2/ui/NightWaveStudioActivity.kt")
service = read(src / "playback/PlaybackService.kt")
offline = read(src / "offline/OfflineSessionStore.kt")
gestureTests = read(root / "app/src/test/java/com/nightwave/player/v2/ui/PlayerGestureRulesTest.kt")
progressTests = read(root / "app/src/test/java/com/nightwave/player/v2/ui/PlayerProgressRulesTest.kt")
manifest = read(root / "app/src/main/AndroidManifest.xml")

assert 'versionCode = 27' in gradle
assert 'versionName = "2.0.0-alpha5k"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle

# Gesture action is bounded and routed through the existing player controller.
assert 'internal enum class PlayerArtworkSwipe { NONE, PREVIOUS, NEXT }' in gesture
assert 'const val MIN_SWIPE_DP = 88f' in gesture
assert 'queueSize < 2' in gesture
assert '!distancePx.isFinite()' in gesture
assert 'fun decideHorizontalSwipe(' in gesture
assert 'detectHorizontalDragGestures(' in player
assert 'state.videoUrl, state.queue.size, state.connected, state.buffering' in player
assert 'PlayerGestureRules.decideHorizontalSwipe(' in player
assert 'PlayerArtworkSwipe.NEXT -> actions.next()' in player
assert 'PlayerArtworkSwipe.PREVIOUS -> actions.previous()' in player
assert 'if (canSwipeArtwork)' in player
assert 'detectVerticalDragGestures(' in player
assert 'onDragCancel = { horizontalTravel = 0f }' in player

# Progress never divides by zero, an old drag cannot seek a newly selected song.
assert 'fun fraction(positionMs: Long, durationMs: Long)' in progress
assert 'fun requestedPosition(durationMs: Long, fraction: Float): Long?' in progress
assert 'durationMs <= 0L' in progress
assert '!fraction.isFinite()' in progress
assert 'var dragFraction by remember(state.videoUrl)' in player
assert 'PlayerProgressRules.requestedPosition(state.durationMs, it)' in player
assert 'enabled = state.connected && state.durationMs > 0L' in player
assert 'PlayerProgressRules.fraction(state.positionMs, state.durationMs)' in player

# Art fallback and transitions honor reduced motion; offline actions remain scoped.
assert 'Crossfade(' in player
assert 'if (reduceMotion) 0 else 180' in player
assert 'CoverArt(thumbnail, Modifier.fillMaxSize(), 26)' in player
assert 'Canvas(Modifier.fillMaxSize(.64f))' in player
assert 'if (url.isNotBlank())' in player
assert 'if (state.buffering)' in player
assert 'enabled = !state.offline' in player
assert 'Text(if (state.offline) "OFFLINE AUDIO" else "NOW PLAYING"' in player
assert 'Button(onClick = actions::retry)' in player
assert 'if (state.queue.size > 1)' in player
assert 'Icon(Icons.Outlined.SkipNext, contentDescription = "Next track"' in player

# No changes to existing data identities, lazy offline resume or network service.
assert 'fun StudioNowPlayingOverlay(' in player
assert 'fun StudioMiniPlayer(' in player
assert 'class StudioPlaybackBridge(' in bridge
assert 'ACTION_RESUME_OFFLINE' in service
assert 'private fun resumeOfflineSession()' in service
assert '"nightwave_offline_session_v1"' in offline
assert 'YouTubeSearchBrowser(this).show(query)' in studio
creation = service.split("override fun onCreate()", 1)[1].split("override fun onStartCommand", 1)[0]
assert 'resolveAndPlay(' not in creation and 'playOfflineId(' not in creation

assert gestureTests.count('@Test') >= 6
assert progressTests.count('@Test') >= 4
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1

for name, code in (
    ('player UI', player), ('gesture rules', gesture),
    ('progress rules', progress), ('controller', bridge)
):
    assert code.count('{') == code.count('}'), name + ' braces unbalanced'
    imports = [line for line in code.splitlines() if line.startswith('import ')]
    assert len(imports) == len(set(imports)), name + ' duplicate imports'

print('PASS: Phase 5F gesture, progress, artwork and old-session source checks')
print('PENDING: Android Gradle build, JUnit, Compose rendering, device crash QA')
