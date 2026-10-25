#!/usr/bin/env python3
"""NW52 targeted UI/action pre-build regressions.

Read-only contract inspection; cannot substitute for Kotlin compiler, Compose
instrumentation, screenshots, touch QA, audio rights or device crash testing.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
OFFLINE = ROOT / "app/src/main/java/com/nightwave/player/offline"
ANDROID_UI_TEST = ROOT / "app/src/androidTest/java/com/nightwave/player/v2/ui"


def check_instrumented_assertion_imports(test_sources: dict[str, str]) -> None:
    """assertExists/assertDoesNotExist are SemanticsNodeInteraction METHODS.

    They are not separately imported as top-level androidx.compose.ui.test API.
    Keep all valid node-query and Compose JUnit imports in place.
    """
    unsupported = (
        "import androidx.compose.ui.test.assertExists",
        "import androidx.compose.ui.test.assertDoesNotExist",
    )
    for filename, body in test_sources.items():
        for invalid_import in unsupported:
            if invalid_import in body.splitlines():
                raise AssertionError(filename + ": invalid import " + invalid_import)
        if "import androidx.compose.ui.test.junit4.createComposeRule" not in body:
            raise AssertionError(filename + ": Compose test rule import missing")
        if "import androidx.compose.ui.test.onNodeWithText" not in body:
            raise AssertionError(filename + ": Compose node finder import missing")
        if ".assertExists()" not in body:
            raise AssertionError(filename + ": assertions unexpectedly removed")


def check(activity: str, player: str, recent: str, chrome: str,
          offline: str, downloads: str) -> None:
    rules = [
        ("home-open-search-callback", "openSearch = { navigateToTab(StudioTab.SEARCH) }" in activity),
        ("home-search-does-not-query-generic-music",
         'Icons.Outlined.Search, "nw47-home-search", openSearch)' in activity),
        ("empty-home-discovers-real-search", '"Discover music", openSearch)' in activity),
        ("real-mood-search-remains-intentional", 'onSearch(shortcut.query)' in activity),
        ("saved-music-navigation", "selectedPlaylist = LIKED_COLLECTION" in activity),
        ("recent-remove-is-independent", "onRemove = { onRemoveRecent(song) }" in activity),
        ("recent-options-are-independent", "onOptions = { onSongOptions(song) }" in activity),
        ("compact-player-restoration-is-touchable",
         '.height(48.dp)) {\n                        Row(verticalAlignment = Alignment.CenterVertically,' in activity),
        ("no-tap-on-empty-track-offline-save",
         'onClick = onSaveOffline,\n                    enabled = state.hasTrack,' in player),
        ("empty-track-offline-action-explains-why",
         'if (!state.hasTrack) "Choose a track first"' in player),
        ("playback-buttons-still-wired",
         "actions.playPause()" in player and "actions.next()" in player),
        ("queue-menu-still-real", "onClick = onOpenQueue" in player),
        ("lyrics-tap-bound-to-original-track",
         "actions.seekToForTrack(cueOriginTrack," in player),
        ("lyrics-follow-accessible", 'testTag("lyrics-follow-toggle")' in player),
        ("reduce-motion-entrance", "if (reduceMotion) EnterTransition.None" in player),
        ("reduce-motion-exit", "if (reduceMotion) ExitTransition.None" in player),
        ("player-motion-policy", "NightWaveMotionPolicy::playerEntryOffsetPx" in player),
        ("recent-play-not-remove", "onClick = onPlay" not in recent
         and "onPlay()" in recent and "onClick = onRemove" in recent),
        ("recent-48dp-options", 'Modifier.size(48.dp)\n                .testTag("nw47-recent-options")' in recent),
        ("recent-48dp-remove", 'Modifier.size(48.dp)\n                .testTag("nw47-recent-remove")' in recent),
        ("navigation-only-on-new-tab", "if (!active) onNavigate(index)" in chrome),
        ("tab-reduced-motion", "if (reducedMotion) snap()" in chrome),
        ("offline-drive-shortcut-touch-target",
         '}, LinearLayout.LayoutParams(-1, dp(48)))\n        val controls' in offline),
        ("offline-sort-screenreader", 'contentDescription = "Sort offline music · "' in offline),
        ("offline-health-filters", "OfflineLibraryViewPolicy.includes(" in offline),
        ("download-ready-requires-file", "OfflineAudioLibrary(this).availableIds(allSavedIds)" in downloads),
        ("download-paused-state-honest", "Resume restarts from zero" in downloads),
        ("download-stalled-state-honest", "No new bytes · Waiting for source" in downloads),
    ]
    problems = [name for name, ok in rules if not ok]
    if problems:
        raise AssertionError("Broken UI/action contracts: " + ", ".join(problems))
    if 'Icons.Outlined.Search, "nw47-home-search") { onSearch("music") }' in activity:
        raise AssertionError("Generic Search shortcut bug returned")
    if '"Discover music") { onSearch("music") }' in activity:
        raise AssertionError("Empty Home still searches generic phrase")


def main() -> int:
    cli = argparse.ArgumentParser()
    cli.add_argument("--self-test", action="store_true")
    args = cli.parse_args()
    files = [
        (UI, "NightWaveStudioActivity.kt"),
        (UI, "StudioPlayerScreens.kt"),
        (UI, "NightWaveEverydaySurfaces.kt"),
        (UI, "NightWavePremiumChrome.kt"),
        (OFFLINE, "OfflineLibraryActivity.kt"),
        (OFFLINE, "CatalogDownloadsActivity.kt"),
    ]
    src = [ (base / name).read_text(encoding="utf-8") for base, name in files ]
    check(*src)
    assertion_tests = {
        filename: (ANDROID_UI_TEST / filename).read_text(encoding="utf-8")
        for filename in (
            "NightWaveEverydaySurfacesInstrumentedTest.kt",
            "NightWaveQueueSectionsInstrumentedTest.kt",
        )
    }
    check_instrumented_assertion_imports(assertion_tests)
    if args.self_test:
        for filename, invalid in (
            ("NightWaveEverydaySurfacesInstrumentedTest.kt",
             "import androidx.compose.ui.test.assertExists"),
            ("NightWaveQueueSectionsInstrumentedTest.kt",
             "import androidx.compose.ui.test.assertDoesNotExist"),
        ):
            broken = dict(assertion_tests)
            broken[filename] = invalid + "\n" + broken[filename]
            try:
                check_instrumented_assertion_imports(broken)
            except AssertionError:
                pass
            else:
                raise AssertionError("Invalid Compose test import fixture accepted")

        for index, old, bad in [
            (0, "openSearch = { navigateToTab(StudioTab.SEARCH) }",
             "openSearch = { onPrepareSearch(\"music\", SearchMode.SONGS) }"),
            (1, "enabled = state.hasTrack,", "enabled = true,"),
            (2, "onClick = onRemove", "onClick = onPlay"),
            (3, "if (!active) onNavigate(index)", "onNavigate(index)"),
            (4, "OfflineLibraryViewPolicy.includes(", "true // bypassed"),
            (5, "OfflineAudioLibrary(this).availableIds(allSavedIds)", "emptySet()"),
        ]:
            mutated = list(src)
            assert old in mutated[index], (index, old)
            mutated[index] = mutated[index].replace(old, bad, 1)
            try:
                check(*mutated)
            except AssertionError:
                continue
            raise AssertionError("UI gate accepted a broken fixture: " + old)
        print("NW52 UI pre-build negative fixtures: PASS")
    print("NW52 action, navigation, buttons, motion and offline source contracts: PASS")
    print("UNVERIFIED: full Kotlin/Compose build, appearance, animations on real screens.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
