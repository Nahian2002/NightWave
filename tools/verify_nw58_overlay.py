#!/usr/bin/env python3
"""NW58 source guard for the opaque Now Playing backdrop."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
TEST = ROOT / "app/src/test/java/com/nightwave/player/v2/ui"


def verify(screen: str, policy: str, tests: str) -> None:
    required = (
        "MutableTransitionState(false)",
        "LaunchedEffect(expanded) { overlayVisibility.targetState = expanded }",
        "NightWavePlayerOverlayPolicy.retainBackdrop(",
        "overlayVisibility.currentState, overlayVisibility.targetState",
        '.testTag("nw58-opaque-player-backdrop")',
        "visibleState = overlayVisibility",
        "NightWaveMotionPolicy.PLAYER_ENTER_MS",
        "NightWaveMotionPolicy.PLAYER_EXIT_MS",
        "if (reduceMotion) EnterTransition.None",
        "if (reduceMotion) ExitTransition.None",
    )
    for token in required:
        if token not in screen:
            raise AssertionError("Player overlay source missing " + token)
    for token in (
        "requestedExpanded || transitionCurrent || transitionTarget",
        "fun retainBackdrop(",
    ):
        if token not in policy:
            raise AssertionError("Opaque overlay policy missing " + token)
    for token in (
        "overlayIsOpaqueBeforeFirstEnterFrame",
        "backdropStaysDuringEnterAndExit",
        "backdropIsRemovedAfterExitCompletes",
    ):
        if token not in tests:
            raise AssertionError("Missing overlay JVM test " + token)
    if "AnimatedVisibility(visible = expanded, enter = enter, exit = exit)" in screen:
        raise AssertionError("Old non-sticky fade-over-Home overlay restored")
    if "overlayVisibility.targetState = expanded\n    // NW58" in screen:
        raise AssertionError("Mutable transition state written during composition")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    files = [
        (UI / "StudioPlayerScreens.kt").read_text(encoding="utf-8"),
        (UI / "NightWavePlayerOverlayPolicy.kt").read_text(encoding="utf-8"),
        (TEST / "NightWavePlayerOverlayPolicyTest.kt").read_text(encoding="utf-8"),
    ]
    verify(*files)
    if args.self_test:
        for index, old, new in (
            (0, "visibleState = overlayVisibility", "visible = expanded"),
            (0, 'testTag("nw58-opaque-player-backdrop")', 'testTag("unused")'),
            (1, "requestedExpanded || transitionCurrent || transitionTarget",
             "requestedExpanded"),
            (2, "backdropStaysDuringEnterAndExit", "removedExitTest"),
        ):
            broken = files[:]
            assert old in broken[index]
            broken[index] = broken[index].replace(old, new, 1)
            try:
                verify(*broken)
            except AssertionError:
                continue
            raise AssertionError("NW58 unsafe fixture was accepted")
        print("NW58 overlay negative fixtures: PASS")
    print("NW58 opaque player/background source contracts: PASS")
    print("UNVERIFIED: actual transition rendering on Android.")


if __name__ == "__main__":
    main()
