#!/usr/bin/env python3
"""NW55 source guard: compact native offline UI with unchanged file safety."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/nightwave/player/offline"
TEST = Path(__file__).resolve().parents[1] / "app/src/test/java/com/nightwave/player/offline"


def verify(library: str, downloads: str, policy: str, unit_test: str) -> None:
    mandatory = {
        "offline": (
            'label("Offline library", 24f',
            'label("Music saved on this device", 13f',
            'OfflineLibraryViewPolicy.accessibilityLabel(filter)',
            'OfflineLibraryViewPolicy.summary(',
            'extraTools.addView(label(',
            'Private on-device audio · Removed if NightWave is uninstalled',
            'OfflineLibraryViewPolicy.includes(',
            'library.healthReport()',
            'library.delete(song)',
            'Play all visible offline tracks',
            'onClickListener',
        ),
        "downloads": (
            'label("Downloads", 24f',
            'contentDescription = "Back to NightWave"',
            'actionRow.addView(label("Pause", 13f,',
            'Start download again',
            'Resume restarts from zero',
            'OfflineAudioLibrary(this).availableIds(allSavedIds)',
            'DownloadHistoryVisibility.hide(',
            'confirmSearchRetry(retryUrl, title,',
            'setOnClickListener',
        ),
        "policy": (
            'Filter.ATTENTION -> "Issues"',
            'Filter.ATTENTION -> "Needs attention"',
            'fun accessibilityLabel(',
            'fun summary(',
            'health == OfflineFileHealth.AVAILABLE',
            'health != OfflineFileHealth.AVAILABLE',
        ),
        "test": (
            'compactFiltersKeepFullAccessibleLabels',
            'summaryRemainsReadableAndDoesNotLeakInternalIndexDetails',
            'OfflineLibraryViewPolicy.Filter.ATTENTION',
        ),
    }
    bodies = dict(offline=library, downloads=downloads, policy=policy, test=unit_test)
    for name, markers in mandatory.items():
        for marker in markers:
            if marker not in bodies[name]:
                raise AssertionError(name + ": missing " + marker)
    if 'p.Filter.' in unit_test:
        raise AssertionError("Invalid Kotlin enum p.Filter reference returned")
    if "LayoutParams(-1, dp(44))" in downloads:
        raise AssertionError("44dp download row actions returned")
    if "root.addView(label(\n            \"Private on-device storage" in library:
        raise AssertionError("Storage warning takes permanent song-list height")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    sources = [
        (ROOT / "OfflineLibraryActivity.kt").read_text(encoding="utf-8"),
        (ROOT / "CatalogDownloadsActivity.kt").read_text(encoding="utf-8"),
        (ROOT / "OfflineLibraryViewPolicy.kt").read_text(encoding="utf-8"),
        (TEST / "OfflineLibraryViewPolicyTest.kt").read_text(encoding="utf-8"),
    ]
    verify(*sources)
    if args.self_test:
        for index, old, new in (
            (0, 'OfflineLibraryViewPolicy.accessibilityLabel(filter)', 'OfflineLibraryViewPolicy.label(filter)'),
            (1, 'Start download again', 'Resume from partial bytes'),
            (2, 'Filter.ATTENTION -> "Issues"', 'Filter.ATTENTION -> "Needs attention"'),
            (3, 'OfflineLibraryViewPolicy.Filter.ATTENTION', 'p.Filter.ATTENTION'),
        ):
            broken = sources[:]
            assert old in broken[index]
            broken[index] = broken[index].replace(old, new, 1)
            try:
                verify(*broken)
            except AssertionError:
                continue
            raise AssertionError("NW55 guard missed regression: " + old)
        print("NW55 native offline UI negative fixtures: PASS")
    print("NW55 Offline/Download source and accessibility contracts: PASS")
    print("UNVERIFIED: actual screen clipping, live transfers and offline playback.")


if __name__ == "__main__":
    main()
