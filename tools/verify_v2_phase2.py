#!/usr/bin/env python3
"""NightWave v2 Phase 2 static regression gate; does not claim Android compilation."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
base = root / "app/src/main/java/com/nightwave/player"
read = lambda relative: (base / relative).read_text(encoding="utf-8")
activity = read("v2/ui/NightWaveStudioActivity.kt")
screens = read("v2/ui/StudioPlayerScreens.kt")
bridge = read("v2/playback/StudioPlaybackBridge.kt")
plan = read("v2/playback/QueueSelection.kt")
service = read("playback/PlaybackService.kt")
legacy = read("ui/MainActivity.kt")
library = read("library/MusicLibrary.kt")
assert "Player.STATE_ENDED" in service and "MediaSessionService" in service
assert "ACTION_PLAY_QUEUE" in service and "queue.current?.let { resolveAndPlay(it) }" in service
assert "ACTION_REPLACE, ACTION_PLAY_QUEUE" in service
assert "MediaController.Builder" in bridge and "MediaController.releaseFuture" in bridge
assert "PlaybackService.ACTION_PLAY_QUEUE" in bridge and "QueueSelectionRules.plan" in bridge
assert all(name in screens for name in (
    "StudioMiniPlayer", "StudioNowPlayingOverlay", "StudioQueueSheet",
    "onOpenQueue", "Slider(", "CoverArt(", "onSaveToPlaylist", "actions.seekTo"
))
assert all(item in activity for item in (
    "playback.connect()", "playback.release()", "saveSongToPlaylist(",
    "showSongOptions(", "manageLibrary()", "onToggleFavorite",
    "StudioNowPlayingOverlay(", "StudioMiniPlayer(", "StudioQueueSheet(",
    "onPlay(song, recent)", "onPlay(song, tracks)", "widthIn(max = 840.dp)"
))
assert all(item in library for item in ("createPlaylist(", "deletePlaylist(", "renamePlaylist(", "toggleFavorite("))
assert "nightwave_web_search" in legacy  # existing search remains available
assert "QueueSelectionRules" in plan and "VideoLink.normalize" in plan
tests = (root / "app/src/test/java/com/nightwave/player/v2/playback/QueueSelectionRulesTest.kt").read_text()
assert tests.count("@Test") >= 5
app = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
android_name = "{http://schemas.android.com/apk/res/android}name"
activities = [x.attrib.get(android_name) for x in app.iter("activity")]
assert ".ui.MainActivity" in activities
assert ".v2.ui.NightWaveStudioActivity" in activities
for source in (activity, screens, bridge, plan):
    assert source.count("{") == source.count("}"), "Braces mismatched"
    imports = [x for x in source.splitlines() if x.startswith("import ")]
    assert len(set(imports)) == len(imports), "Duplicate imports"
gradle = (root / "app/build.gradle.kts").read_text()
assert 'id("org.jetbrains.kotlin.plugin.compose")' in gradle
assert "media3-session" in gradle
print("PASS: Media3 playback ownership, queued selection, Compose mini/full player,")
print("      playlist actions, legacy fallback and five queue regression tests")
print("PENDING: full Android build, live stream, screen-size and hardware QA")
