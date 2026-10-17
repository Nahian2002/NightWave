#!/usr/bin/env python3
"""Phase 6A2 mini-player and layout source gate; NOT a device or Gradle test."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/v2/ui/"
player = read(base + "StudioPlayerScreens.kt")
studio = read(base + "NightWaveStudioActivity.kt")
components = read(base + "StudioComponents.kt")
test = read("app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveMiniDismissInstrumentedTest.kt")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")

mini = player.split("fun StudioMiniPlayer(", 1)[1].split("private fun CoverArt(", 1)[0]
assert 'Icons.Outlined.Close' in mini
assert 'IconButton(onClick = onHide, modifier = Modifier.size(48.dp))' in mini
assert 'contentDescription = "Hide mini-player; music keeps playing"' in mini
# Phase 6I keeps the original whole-card vertical swipe gesture, while
# shortening only the visual drag-handle strip.
assert 'Box(Modifier.fillMaxWidth().height(9.dp)' in mini
assert '.pointerInput(state.videoUrl)' in mini
assert 'PlayerGestureRules.decideVerticalSwipe(' in mini
assert 'PlayerVerticalSwipe.MINIMIZE -> onHide()' in mini
assert 'PlayerVerticalSwipe.EXPAND -> onExpand()' in mini
assert 'val showNext = state.queue.size > 1 && maxWidth >= 365.dp' in mini
assert 'if (showNext)' in mini
assert '.clickable(onClick = onExpand)' in mini
assert 'onClick = actions::playPause' in mini
assert 'onClick = actions::next' in mini
# Hide must never stop/pause, change queue, or re-resolve streams.
close = mini.split('IconButton(onClick = onHide', 1)[1].split('\n            }', 1)[0]
for forbidden in ("playPause", "ACTION_STOP", "pause()", "clearMediaItems",
                  "playback.stop", "navigateToTab", "resolveAndPlay"):
    assert forbidden not in close, forbidden
assert 'onHide = { compactHidden = true }' in studio
assert 'onClick = { compactHidden = false }' in studio
assert 'LaunchedEffect(playerState.videoUrl) { compactHidden = false }' in studio
assert 'StudioMiniPlayer(playerState, playerActions,' in studio
assert 'if (playerState.hasTrack && compactHidden)' in studio

# Scaffold already subtracts bottomBar. Only add per-page scroll room.
assert 'HorizontalPager(' in studio
assert 'modifier = Modifier.fillMaxSize().padding(safe)' in studio
for value in ('bottom = 36.dp', 'bottom = 34.dp', 'bottom = 40.dp', 'bottom = 52.dp'):
    assert value in studio, value
assert 'bottom = 150.dp' not in studio
assert 'contentPadding = PaddingValues(19.dp)' not in studio

# No text/cover overlap: Row owns sibling widths, not two overlapping
# Box alignments. The artwork shrinks before the title is squeezed.
spotlight = components.split("fun ListeningSpotlight(", 1)[1].split("private fun TrackArtwork(", 1)[0]
assert 'BoxWithConstraints(' in spotlight
assert 'val artworkSize = if (maxWidth < 340.dp) 96.dp else 124.dp' in spotlight
assert 'Row(Modifier.fillMaxSize().padding(horizontal = 16.dp' in spotlight
assert 'Column(Modifier.weight(1f)' in spotlight
assert 'Modifier.size(artworkSize)' in spotlight
assert 'maxLines = 2' in spotlight
assert 'onClick = if (hasRecent) onResume else onDiscover' in spotlight
assert 'fillMaxWidth(.65f)' not in spotlight

# The new tests are authored for a disposable Android emulator only.
assert test.count('@Test') >= 2
assert 'assertEquals(0, actions.mutations)' in test
assert 'onNodeWithContentDescription("Next track").assertDoesNotExist()' in test
assert 'import androidx.compose.ui.test.assertExists' not in test
assert 'import androidx.compose.ui.test.assertDoesNotExist' not in test
assert 'tools/verify_v2_phase6a2.py' in workflow
assert ':app:compileDebugAndroidTestKotlin' in workflow
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
for name, source in (("mini-player", player), ("activity", studio), ("components", components)):
    assert source.count('{') == source.count('}'), name + ": unbalanced braces"
    imports = [x for x in source.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), name + ": duplicate imports"

print("PASS: 6A2 mini dismissal without playback calls, responsive bottom padding and non-overlapping Home cover")
print("PENDING: Compose Gradle, emulator gestures, device signing and screen capture QA")
