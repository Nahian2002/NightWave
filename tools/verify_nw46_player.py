#!/usr/bin/env python3
"""NW46 static regression gate for the immersive player, with negative fixtures.

Not a substitute for Kotlin compilation, Android instrumentation or device QA.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PLAYER = ROOT / "app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt"
POLICY = ROOT / "app/src/main/java/com/nightwave/player/v2/ui/NightWaveNowPlayingLayoutPolicy.kt"
TEST = ROOT / "app/src/test/java/com/nightwave/player/v2/ui/NightWaveNowPlayingLayoutPolicyTest.kt"


def verify(player: str, policy: str, test: str) -> None:
    mandatory = (
        "NightWaveNowPlayingLayoutPolicy.resolve(",
        "nightwave-now-playing-artwork",
        "nightwave-now-playing-play-pause",
        "nightwave-now-playing-offline-action",
        "nightwave-now-playing-queue-action",
        "NightWaveBackdrop(state.videoUrl",
        "PlayerGestureRules.decideHorizontalSwipe(",
        "PlayerGestureRules.decideVerticalSwipe(",
        "NightWaveSeekTimeline(",
        "actions.seekToForTrack(originatingTrack, it)",
        "actions::toggleShuffle",
        "actions::cycleRepeat",
        "actions::playPause",
        "actions.next()",
        "actions.previous()",
        "onLike",
        "onSaveOffline",
        "onSaveToPlaylist",
        "onOpenQueue",
        "onSleepTimer",
        "onCrossfade",
        "onEditLyrics",
        "onImportLyrics",
        "if (reduceMotion) snap()",
        "if (state.buffering)",
        "onClick = actions::retry",
    )
    absent = [item for item in mandatory if item not in player]
    if absent:
        raise AssertionError("NW46 lost required playback/UX contracts: " + ", ".join(absent))
    order = [
        'Text("Up next"',
        'onClick = { lyricsOpen = !lyricsOpen }',
        'onClick = { advancedAudioOpen = !advancedAudioOpen }',
    ]
    positions = [player.find(token) for token in order]
    if any(p < 0 for p in positions) or positions != sorted(positions):
        raise AssertionError("NW46 queue / lyrics / advanced settings hierarchy regressed")
    for marker in (
        "widthDp.takeIf { it.isFinite() }",
        "heightDp.takeIf { it.isFinite() }",
        "fontScale.takeIf { it.isFinite() }",
        "height < 680f",
        "scale >= 1.6f",
        "420f",
        "availableWidth",
        "coerceAtLeast(0f)",
    ):
        if marker not in policy:
            raise AssertionError("NW46 safe layout guard missing: " + marker)
    for marker in (
        "normalPhoneGetsImmersiveHeroWithoutHorizontalClipping",
        "smallOrSplitScreenRemainsCompactAndReachable",
        "largeTextYieldsMoreRoomForLabelsAndControls",
        "tabletAndLandscapeStayBounded",
        "invalidMeasurementsNeverCrashOrProduceNegativeDp",
    ):
        if marker not in test:
            raise AssertionError("NW46 test coverage missing: " + marker)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    player, policy, test = (
        PLAYER.read_text(encoding="utf-8"),
        POLICY.read_text(encoding="utf-8"),
        TEST.read_text(encoding="utf-8"),
    )
    verify(player, policy, test)
    if args.self_test:
        def must_fail(p: str, q: str, t: str) -> None:
            try:
                verify(p, q, t)
            except AssertionError:
                return
            raise AssertionError("NW46 gate accepted its negative fixture")

        must_fail(player.replace("actions.seekToForTrack(originatingTrack, it)",
                                 "actions.seekTo(it)"), policy, test)
        must_fail(player.replace('onClick = { lyricsOpen = !lyricsOpen }',
                                 'onClick = { advancedAudioOpen = !advancedAudioOpen }'),
                  policy, test)
        must_fail(player, policy.replace("widthDp.takeIf { it.isFinite() }",
                                         "widthDp"), test)
        must_fail(player, policy, test.replace(
            "invalidMeasurementsNeverCrashOrProduceNegativeDp", "deletedTest"))
        print("NW46 negative fixtures: PASS")
    print("NW46 source/policy/test wiring: PASS (not an Android build)")


if __name__ == "__main__":
    main()
