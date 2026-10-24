#!/usr/bin/env python3
"""6V7A non-destructive premium UI source and regression-discovery audit.

Static wiring only: no Gradle compile, Compose render, Android device or APK.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
MAIN = "app/src/main/java/com/nightwave/player/v2/ui/"
UNIT = "app/src/test/java/com/nightwave/player/v2/ui/"
ANDROID = "app/src/androidTest/java/com/nightwave/player/v2/ui/"
CHECKS = {
    MAIN + "NightWaveChromePolicy.kt": (
        "object NightWaveChromePolicy",
        "NAV_ITEM_COUNT = 4", "NAV_HEIGHT_DP = 60",
        "NAV_MIN_TOUCH_DP = 56",
        "MINI_NEXT_MIN_WIDTH_DP = 365f",
        "fun showMiniNext(", "fun selectedTab(",
        "fun displayFirstName(", "fun greeting(",
    ),
    MAIN + "NightWavePremiumChrome.kt": (
        "fun NightWavePremiumNavigation(",
        "fun NightWaveHomeHero(",
        '"nightwave-tab-" + tags[index]',
        '"nightwave-home-hero"',
        "Role.Tab", "selected = active",
        'listOf("Home", "Search", "Library", "You")',
        "NightWaveChromePolicy.NAV_HEIGHT_DP.dp",
        "NightWaveAdaptiveChromePolicy.motionDurationMs(reducedMotion, 160)",
        "onNavigate(index)",
        "colors.surface", "Brush.horizontalGradient(",
    ),
    MAIN + "NightWaveAdaptiveChromePolicy.kt": (
        "if (reducedMotion) 0 else regularMs.coerceIn(0, 600)",
    ),
    MAIN + "NightWaveStudioActivity.kt": (
        "private enum class StudioTab(",
        "val pagerState = rememberPagerState(",
        "HorizontalPager(",
        "NightWavePremiumNavigation(",
        "selectedIndex = tab.ordinal",
        "navigateToTab(StudioTab.entries[",
        "NightWaveChromePolicy.selectedTab(target)",
        "NightWaveHomeHero(",
        "greeting = NightWaveChromePolicy.greeting(",
        "displayName = profile.displayName",
        "StudioMiniPlayer(playerState, playerActions,",
        "onHide = { compactHidden = true }",
        "userScrollEnabled = !expanded && !showQueue",
        "BackHandler(enabled = showQueue || expanded ||",
    ),
    MAIN + "StudioPlayerScreens.kt": (
        "fun StudioMiniPlayer(",
        'testTag("nightwave-mini-player")',
        "NightWaveChromePolicy.showMiniNext(",
        "queueCount = state.queue.size",
        "actions::playPause",
        "actions::next",
        "IconButton(onClick = onHide",
        "detectVerticalDragGestures(",
        "PlayerGestureRules.decideVerticalSwipe(",
        "PlayerProgressRules.fraction(",
        '"Hide mini-player; music keeps playing"',
    ),
    "tools/build-windows.ps1": (
        "check-phase6v7a-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ),
    "tools/check-builder-syntax.ps1": ("check-phase6v7a-integration.ps1",),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v7a.py --self-test",
        "python3 tools/verify_v2_phase6v7a.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
}
TESTS = {
    UNIT + "NightWaveChromePolicyTest.kt": (
        9, ("onlyShowNextForMultipleTracksAndEnoughRoom",
            "navigationIndexesClampToFourKnownTabs",
            "emptyAndWhitespaceProfileNamesRemainReadable")),
    ANDROID + "NightWavePremiumChromeInstrumentedTest.kt": (
        3, ("premiumNavigationKeepsAllFourTappableDestinationsOnSmallScreens",
            "premiumHomeHeroHandlesUserProfileWithoutTrackMetadata")),
    ANDROID + "NightWaveSwipeTabsInstrumentedTest.kt": (
        2, ("swipesChangeTheSelectedBottomTab",)),
    ANDROID + "NightWaveMiniDismissInstrumentedTest.kt": (
        2, ("closeHidesMiniPlayerWithoutTouchingPlayback",)),
}


def audit(root: Path) -> list[str]:
    errors: list[str] = []
    for path, strings in CHECKS.items():
        file = root / path
        if not file.is_file():
            errors.append("Missing source: " + path)
            continue
        data = file.read_text(encoding="utf-8")
        for token in strings:
            if token not in data:
                errors.append(path + ": missing " + repr(token))
    for path, (minimum, method_names) in TESTS.items():
        file = root / path
        if not file.is_file():
            errors.append("Missing regression: " + path)
            continue
        data = file.read_text(encoding="utf-8")
        if data.count("@Test fun ") < minimum:
            errors.append(path + f": too few tests (need {minimum})")
        for name in method_names:
            if "fun " + name + "(" not in data:
                errors.append(path + ": missing test " + name)
    return errors


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nw-6v7a-") as temp:
        root = Path(temp)
        for path, strings in CHECKS.items():
            dest = root / path
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text("\n".join(strings), encoding="utf-8")
        for path, (minimum, methods) in TESTS.items():
            dest = root / path
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text("\n".join(
                [f"@Test fun case{i}() {{}}" for i in range(minimum)] +
                [f"fun {name}() {{}}" for name in methods]),
                encoding="utf-8")
        assert not audit(root), audit(root)
        root_nav = root / (MAIN + "NightWaveStudioActivity.kt")
        root_nav.write_text(root_nav.read_text(encoding="utf-8").replace(
            "userScrollEnabled = !expanded && !showQueue",
            "userScrollEnabled = true"
        ), encoding="utf-8")
        assert any("userScrollEnabled" in problem for problem in audit(root))
        mini = root / (MAIN + "StudioPlayerScreens.kt")
        mini.write_text(mini.read_text(encoding="utf-8").replace(
            '"Hide mini-player; music keeps playing"', '"Stop music"'
        ), encoding="utf-8")
        assert any("Hide mini-player" in problem for problem in audit(root))
    print("PASS: synthetic 6V7A gate detects pager-swipe and hide-audio regressions")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    missing = audit(args.root)
    for issue in missing:
        print("FAIL:", issue)
    if missing:
        return 1
    print("PASS: 6V7A Home, premium navigation, mini-player and tests wired")
    print("NOT VERIFIED: Gradle, Compose instrumentation, APK signature or phone")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
