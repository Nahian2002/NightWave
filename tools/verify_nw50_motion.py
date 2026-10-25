#!/usr/bin/env python3
"""NW50 static safety regression gate. Not a substitute for Gradle/device QA."""
from __future__ import annotations
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
TESTS = ROOT / "app/src/test/java/com/nightwave/player/v2/ui"
ANDROID_TESTS = ROOT / "app/src/androidTest/java/com/nightwave/player/v2/ui"


def verify(files: dict[str, str]) -> None:
    tokens = {
        "NightWaveMotionPolicy.kt": [
            "const val PLAYER_ENTER_MS = 260",
            "const val PLAYER_EXIT_MS = 180",
            "const val COVER_MS = 190",
            "fun animateTabChange(",
            "kotlin.math.abs(fromIndex.toLong() - toIndex.toLong())",
            "fun tabHaptic(",
            "!reduceMotion &&",
            "fun playerEntryOffsetPx(",
            "coerceAtMost(240L)",
            "fun playerExitOffsetPx(",
            "coerceAtMost(200L)",
        ],
        "NightWavePremiumChrome.kt": [
            "val indicatorProgress by animateFloatAsState(",
            "if (reducedMotion) snap()",
            "hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)",
            "if (NightWaveMotionPolicy.tabHaptic(selectedIndex, index,",
            "if (!active) onNavigate(index)",
            "alpha = indicatorProgress",
            "scaleX = indicatorProgress",
            "Role.Tab",
            ".semantics { selected = active }",
        ],
        "NightWaveStudioActivity.kt": [
            "var tabNavigationJob by remember { mutableStateOf<Job?>(null) }",
            "tabNavigationJob?.cancel()",
            "NightWaveMotionPolicy.animateTabChange(",
            "pagerState.animateScrollToPage(destination.ordinal,",
            "pagerState.scrollToPage(destination.ordinal)",
            "selectedPlaylist = LIKED_COLLECTION",
            "userScrollEnabled = !expanded && !showQueue",
            "onPrimarySearch",
        ],
        "StudioPlayerScreens.kt": [
            "val hapticView = LocalView.current",
            "if (!reducedMotion)",
            "label = \"Mini player changing cover\"",
            "NightWaveMotionPolicy.COVER_MS",
            "initialOffsetY = NightWaveMotionPolicy::playerEntryOffsetPx",
            "targetOffsetY = NightWaveMotionPolicy::playerExitOffsetPx",
            "if (reduceMotion) EnterTransition.None",
            "if (reduceMotion) ExitTransition.None",
            "actions.playPause()",
            "actions.next()",
            "onHide()",
            "onExpand()",
            "actions.seekToForTrack(originatingTrack, it)",
            "actions.seekToForTrack(cueOriginTrack,",
            "NightWaveMotionPolicy.FEEDBACK_MS",
        ],
        "NightWaveEverydaySurfaces.kt": [
            'label = "Recent song playing"',
            'label = "Recent artwork emphasis"',
            "NightWaveMotionPolicy.artworkScale(isPlaying, reducedMotion)",
            "if (!reducedMotion)",
            "onPlay()",
            "onClick = onOptions",
            "onClick = onRemove",
            "nw47-recent-remove",
            "nw47-recent-play",
        ],
        "NightWaveMotionPolicyTest.kt": [
            "nearbyNavigationAnimatesButLongDistanceSkipsHeavyIntermediatePages",
            "invalidPagesAndExtremeIndicesDoNotAnimate",
            "hapticFeedbackIsOptionalAndUserNavigationOnly",
            "playerEntryExitAreSmallAndSafeEvenForInvalidMeasurements",
            "allDeclaredMotionIsBoundedAndReducibleToZero",
        ],
        "NightWavePremiumChromeInstrumentedTest.kt": [
            "repeatTapOnSelectedTabDoesNotDispatchDuplicateNavigation",
            "assertEquals(1, navigateCalls)",
        ],
    }
    for name, markers in tokens.items():
        missing = [marker for marker in markers if marker not in files[name]]
        if missing:
            raise AssertionError(name + " missing " + repr(missing))
    for name, body in files.items():
        imports = [x for x in body.splitlines() if x.startswith("import ")]
        if len(imports) != len(set(imports)):
            raise AssertionError(name + " has duplicate imports")
    player = files["StudioPlayerScreens.kt"]
    if 'slideInVertically(initialOffsetY = { it })' in player:
        raise AssertionError("NW50 full-height player fly-in regressed")
    if "Crossfade(targetState = state.thumbnail" in player:
        raise AssertionError("NW50 mini-player image change lost motion policy")
    if "hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)" not in player:
        raise AssertionError("No explicit user playback feedback remains")
    activity = files["NightWaveStudioActivity.kt"]
    if activity.index("tabNavigationJob?.cancel()") > activity.index(
            "pagerState.animateScrollToPage(destination.ordinal,"):
        raise AssertionError("Old animation must be cancelled before next tab animation")


def main() -> None:
    cli = argparse.ArgumentParser()
    cli.add_argument("--self-test", action="store_true")
    args = cli.parse_args()
    names = [
        "NightWaveMotionPolicy.kt", "NightWavePremiumChrome.kt",
        "NightWaveStudioActivity.kt", "StudioPlayerScreens.kt",
        "NightWaveEverydaySurfaces.kt", "NightWaveMotionPolicyTest.kt",
        "NightWavePremiumChromeInstrumentedTest.kt",
    ]
    def path(n: str) -> Path:
        return ((ANDROID_TESTS if n == "NightWavePremiumChromeInstrumentedTest.kt"
                 else TESTS if n.endswith("Test.kt") else BASE) / n)
    source = {n: path(n).read_text(encoding="utf-8") for n in names}
    verify(source)
    if args.self_test:
        def mutate(name: str, old: str, new: str) -> None:
            assert old in source[name], old
            broken = dict(source)
            broken[name] = broken[name].replace(old, new, 1)
            try:
                verify(broken)
            except AssertionError:
                return
            raise AssertionError("NW50 gate accepted a broken fixture")
        mutate("NightWaveMotionPolicy.kt", "coerceAtMost(240L)",
               "coerceAtMost(Long.MAX_VALUE)")
        mutate("NightWavePremiumChrome.kt", "if (!active) onNavigate(index)",
               "onNavigate(index)")
        mutate("NightWaveStudioActivity.kt", "tabNavigationJob?.cancel()",
               "tabNavigationJob = null")
        mutate("StudioPlayerScreens.kt",
               "if (reduceMotion) EnterTransition.None",
               "if (reduceMotion) fadeIn()")
        mutate("NightWaveEverydaySurfaces.kt",
               'label = "Recent artwork emphasis"',
               'label = "No animation"')
        print("NW50 negative fixtures: PASS")
    print("NW50 source motion, haptics, navigation and accessibility checks: PASS")
    print("UNVERIFIED: Android compilation, actual JVM/instrumented test execution and device QA")


if __name__ == "__main__":
    main()
