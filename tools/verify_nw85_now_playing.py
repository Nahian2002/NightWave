#!/usr/bin/env python3
"""NW85 source-only presentation contracts. Not an Android renderer test."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/v2/ui/"
player = read(base + "StudioPlayerScreens.kt")
policy = read(base + "NightWaveNowPlayingPresentationPolicy.kt")
overlay = read(base + "NightWavePlayerOverlayPolicy.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/ui/NightWaveNowPlayingPresentationPolicyTest.kt")
smoke = read("tools/NW85PlayerSmoke.kt")

# Keep opaque animation backdrop and all verified NW80 gesture/seek paths.
assert "NightWavePlayerOverlayPolicy.retainBackdrop(" in player
assert ".testTag(\"nw58-opaque-player-backdrop\")" in player
assert "NightWaveMotionPolicy.PLAYER_ENTER_MS" in player
assert "actions.seekToForTrack(originatingTrack, it)" in player
assert "PlayerGestureRules.decideHorizontalSwipe(" in player
assert "detectVerticalDragGestures(" in player
assert "if (reduceMotion) EnterTransition.None" in player
assert "if (reduceMotion) ExitTransition.None" in player
assert "fun retainBackdrop(" in overlay

# Truthful queue preview: explicit Play Next, repeat-one/all, shuffle unknown,
# failed/empty queue and original track ID/slot remain distinct.
for phrase in (
    "fun queuePreview(", "if (repeatOne)",
    "for (requested in manualNextUrls)", "if (shuffled)",
    'QueuePreview("Shuffle queue"', "repeatAll && urls.isNotEmpty()",
    "Waiting for queue selection", "fun offlineActionLabel(",
    'else -> "Offline options"', "fun lyricsSubtitle(",
):
    assert phrase in policy, phrase
for phrase in (
    "NightWaveNowPlayingPresentationPolicy.queuePreview(",
    "manualNextUrls = state.manualNextUrls",
    "repeatOne = state.repeat == PlaybackQueue.Repeat.ONE",
    "repeatAll = state.repeat == PlaybackQueue.Repeat.ALL",
    "playerIdentity = remember(state.title, state.artist)",
    '"Artwork for " + playerIdentity.title',
    '"Artwork for " + identity.title',
    "NightWaveNowPlayingPresentationPolicy.offlineActionLabel(",
    "NightWaveNowPlayingPresentationPolicy.lyricsSubtitle(",
    "NightWaveNowPlayingPresentationPolicy.titleLines(",
    "NightWaveNowPlayingPresentationPolicy.artistLines(",
    '"nightwave-now-playing-play-pause"',
    '"nightwave-now-playing-queue-action"',
    '"nightwave-now-playing-offline-action"',
    '"nightwave-now-playing-lyrics-action"',
    "Crossfade(",
    "NightWaveMotionPolicy.duration(",
):
    assert phrase in player, phrase

for name in (
    "regularQueueShowsExactlyTheNextSavedSong",
    "priorityPlayNextUsesExplicitSelectionWithoutReplacingSourceIndex",
    "shuffleWithoutManualPriorityDoesNotPredictRandomNext",
    "repeatsAreExplainedTruthfullyIncludingLastSongWrap",
    "invalidStateAndMissingMetadataNeverCrashOrInventTracks",
    "honestLyricsAndOfflineDescriptionsNeverPromiseRestrictedDownloads",
    "textHierarchySupportsLargeFontsAndGestureTruth",
):
    assert name in tests, name
assert "NW85_PRESENTATION_POLICY_SMOKE_PASS" in smoke
assert 'onClick = onOpenQueue' in player
assert 'onClick = onSaveOffline' in player
assert 'onClick = { lyricsOpen = !lyricsOpen }' in player
assert 'onDismissRequest = onDismiss' in player
print("NW85_NOW_PLAYING_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: full Android build, Compose screenshots, queue audio and device QA")
