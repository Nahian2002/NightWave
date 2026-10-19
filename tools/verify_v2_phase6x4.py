#!/usr/bin/env python3
"""6X4 source integration gate, NOT Android/JUnit or physical-device certification."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
queue = read(base + "offline/OfflinePlaybackQueue.kt")
settings = read(base + "offline/OfflinePlaybackPreferencesStore.kt")
gapless = read(base + "offline/OfflineGaplessPolicy.kt")
service = read(base + "playback/PlaybackService.kt")
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
ui = read(base + "v2/ui/StudioPlayerScreens.kt")
tests = read("app/src/test/java/com/nightwave/player/offline/OfflinePlaybackQueueModesTest.kt")
transitions = read("app/src/test/java/com/nightwave/player/offline/OfflineGaplessRepeatShuffleTest.kt")
ci = read(".github/workflows/android.yml")
assert "fun setShuffle(enabled: Boolean)" in queue
assert "fun setRepeat(mode: PlaybackQueue.Repeat)" in queue
assert "repeat == PlaybackQueue.Repeat.ONE" in queue
assert "repeat == PlaybackQueue.Repeat.ALL" in queue
assert "visited.removeLast()" in queue
assert "unplayed.removeAt(pick(unplayed.size)" in queue
assert "ids.distinct()" not in queue
assert "take(MAX_ITEMS)" in queue
assert "nightwave_offline_controls_v1" in settings
assert "putString(\"repeat\", mode.name)" in settings
assert "putBoolean(\"shuffle\", enabled)" in settings
assert "automatic: Boolean, shuffled: Boolean = false" in gapless
assert "repeatAll: Boolean = false" in gapless
assert "offlineControls = OfflinePlaybackPreferencesStore(this)" in service
assert "offlineQueue.setShuffle(offlineControls.shuffle())" in service
assert "offlineQueue.setRepeat(offlineControls.repeat())" in service
assert "ACTION_SHUFFLE -> if (offlinePlaying)" in service
assert "ACTION_REPEAT -> if (offlinePlaying)" in service
assert "offlineControls.setShuffle(enabled)" in service
assert "offlineControls.setRepeat(mode)" in service
assert "shuffleModeEnabled = offlineGaplessActive && offlineQueue.shuffle" in service
assert "Player.REPEAT_MODE_ONE" in service
assert "Player.REPEAT_MODE_ALL" in service
assert "if (offlineGaplessActive || offlineQueue.size == 1)" in service
assert "advanceOffline(forward = true, automatic = automatic)" in service
assert "offlineQueue.next(automatic)" in service
assert "player.nextMediaItemIndex" in service
assert "player.previousMediaItemIndex" in service
assert "shuffled = offlineQueue.shuffle" in service
assert "repeatAll = offlineQueue.repeat == PlaybackQueue.Repeat.ALL" in service
assert "offlineQueue.shuffle || offlineQueue.repeat == PlaybackQueue.Repeat.ONE" in service
assert "ACTION_REPEAT -> if (offlinePlaying)" in service
assert "if (offlinePlaying) {" in bridge
assert "pendingOfflineRepeat" in bridge
assert "pendingOfflineShuffle" in bridge
assert "OfflinePlaybackPreferencesStore(context).shuffle()" in bridge
assert "OfflinePlaybackPreferencesStore(context).repeat()" in bridge
assert "EXTRA_SHUFFLE_ENABLED" in bridge
assert "EXTRA_REPEAT_MODE" in bridge
assert 'contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off"' in ui
assert "IconButton(onClick = actions::cycleRepeat," in ui
assert "IconButton(onClick = actions::toggleShuffle," in ui
assert tests.count("@Test") >= 8
assert transitions.count("@Test") >= 4
assert "python3 tools/verify_v2_phase6x4.py" in ci
assert "python3 tools/verify_v2_phase6x3.py" in ci
assert "feature/nightwave-v2-phase6x4-offline-repeat-shuffle" in ci
for label, value in (("queue",queue),("settings",settings),("gapless",gapless),
                     ("service",service),("bridge",bridge),("ui",ui),
                     ("tests",tests),("transitionTests",transitions)):
    assert value.count("{") == value.count("}"), label + " unbalanced braces"
    imports = [line for line in value.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), label + " duplicate imports"
print("PASS: Phase 6X4 offline repeat and shuffle source integration")
print("PENDING: Android Gradle build, unit execution, and physical device regression")
