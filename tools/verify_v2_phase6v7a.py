#!/usr/bin/env python3
"""6V7A non-destructive premium UI source and regression-discovery audit.

Static wiring only: no Gradle compile, Compose render, Android device or APK.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import tempfile
import re

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
        "NightWaveAdaptiveChromePolicy.navHeightDp(fontScale).dp",
        "NightWaveMotionPolicy.duration(",
        "NightWaveMotionPolicy.tabScale(active, reducedMotion)",
        "if (reducedMotion) snap()",
        "onNavigate(index)",
        "colors.surface", "Brush.horizontalGradient(",
    ),
    MAIN + "NightWaveAdaptiveChromePolicy.kt": (
        "if (reducedMotion) 0 else regularMs.coerceIn(0, 600)",
    ),
    MAIN + "NightWaveMiniPlayerLayoutPolicy.kt": (
        "NightWaveChromePolicy.showMiniNext(queueCount, width)",
        "!(scale >= 1.8f && width < 460f)",
    ),
    MAIN + "NightWaveMotionPolicy.kt": (
        "object NightWaveMotionPolicy",
        "if (reduceMotion) 0 else requested.coerceIn(0, MAX_MS)",
        "if (reduceMotion) 1f",
    ),
    MAIN + "NightWaveTheme.kt": (
        "LocalNightWaveReduceMotion provides reduceMotion",
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
        "NightWaveMiniPlayerLayoutPolicy.resolve(",
        "val showNext = miniLayout.showNext",
        "if (showNext)",
        "queueCount = state.queue.size",
        "actions.playPause()",
        "actions.next()",
        "onHide()",
        "IconButton(onClick = {",
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


def hide_only_changes_visibility(source: str) -> bool:
    """Inspect the actual Close button lambda, not an obsolete click syntax.

    NW50 added optional Android haptics; the button legitimately uses
    IconButton(onClick = { ... onHide() }) instead of onClick = onHide.
    This guard must reject any audio/playback/service call in that lambda.
    """
    marker = '"Hide mini-player; music keeps playing"'
    position = source.rfind(marker)
    if position < 0:
        return False
    begin = source.rfind("IconButton(onClick = {", 0, position)
    if begin < 0:
        return False
    end = source.find("}, modifier = Modifier.size(48.dp))", begin)
    if end < begin or end > position:
        return False
    handler = source[begin:end]
    return ("onHide()" in handler and
        re.search(r"\bactions\s*\.|\b(?:pause|stop)\s*\(|"
                  r"startService\s*\(|ACTION_(?:PAUSE|STOP)", handler, re.I) is None)


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
        if path == MAIN + "StudioPlayerScreens.kt" and not hide_only_changes_visibility(data):
            errors.append(path + ": Hide must not call playback, pause, stop or service controls")
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
        mini_fixture = root / (MAIN + "StudioPlayerScreens.kt")
        mini_source = mini_fixture.read_text(encoding="utf-8")
        mini_source += (
            '\nIconButton(onClick = { onHide() }, modifier = Modifier.size(48.dp)) {'
            '\n  Icon(Icons.Outlined.Close,'
            '\n       contentDescription = "Hide mini-player; music keeps playing")'
            '\n}\n'
        )
        mini_fixture.write_text(mini_source, encoding="utf-8")
        for path, (minimum, methods) in TESTS.items():
            dest = root / path
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text("\n".join(
                [f"@Test fun case{i}() {{}}" for i in range(minimum)] +
                [f"fun {name}() {{}}" for name in methods]),
                encoding="utf-8")
        assert not audit(root), audit(root)
        for path, token in (
            (MAIN + "StudioPlayerScreens.kt", "NightWaveMiniPlayerLayoutPolicy.resolve("),
            (MAIN + "StudioPlayerScreens.kt", "val showNext = miniLayout.showNext"),
            (MAIN + "StudioPlayerScreens.kt", "if (showNext)"),
            (MAIN + "NightWaveMiniPlayerLayoutPolicy.kt", "NightWaveChromePolicy.showMiniNext(queueCount, width)"),
            (MAIN + "NightWaveMiniPlayerLayoutPolicy.kt", "!(scale >= 1.8f && width < 460f)"),
        ):
            target = root / path
            original = target.read_text(encoding="utf-8")
            target.write_text(original.replace(token, "REMOVED"), encoding="utf-8")
            assert any(token in problem for problem in audit(root)), token
            target.write_text(original, encoding="utf-8")
        root_nav = root / (MAIN + "NightWaveStudioActivity.kt")
        root_nav.write_text(root_nav.read_text(encoding="utf-8").replace(
            "userScrollEnabled = !expanded && !showQueue",
            "userScrollEnabled = true"
        ), encoding="utf-8")
        assert any("userScrollEnabled" in problem for problem in audit(root))
        mini = root / (MAIN + "StudioPlayerScreens.kt")
        safe_source = mini.read_text(encoding="utf-8")
        mini.write_text(safe_source.replace(
            'contentDescription = "Hide mini-player; music keeps playing"',
            'contentDescription = "Stop music"'
        ), encoding="utf-8")
        assert any("Hide mini-player" in problem or "Hide must not" in problem
                   for problem in audit(root))
        mini.write_text(safe_source.replace(
            'onHide() }, modifier', 'onHide(); actions.playPause() }, modifier'
        ), encoding="utf-8")
        assert any("Hide must not" in problem for problem in audit(root))
        mini.write_text(safe_source.replace(
            'onHide() }, modifier', 'actions.playPause() }, modifier'
        ), encoding="utf-8")
        assert any("Hide must not" in problem for problem in audit(root))
        motion = root / (MAIN + "NightWaveMotionPolicy.kt")
        motion.write_text(motion.read_text(encoding="utf-8").replace(
            "if (reduceMotion) 0 else requested.coerceIn(0, MAX_MS)",
            "requested.coerceIn(0, MAX_MS)"
        ), encoding="utf-8")
        assert any("reduceMotion" in problem for problem in audit(root))
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
