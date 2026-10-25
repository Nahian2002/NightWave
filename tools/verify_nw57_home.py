#!/usr/bin/env python3
"""NW57: protect useful music above the fold with unchanged Home actions."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
STUDIO = ROOT / "app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt"


def verify(source: str) -> None:
    begin = source.index("private fun StudioHome(")
    end = source.index("@Composable\nprivate fun MoodTile(", begin)
    home = source[begin:end]
    required = (
        'item(key = "nightwave-premium-home-header")',
        'NightWaveHomeHero(',
        'item(key = "nw47-home-quick-access")',
        'onResumeSpotlight(it, recent)',
        'item(key = "recent-heading")',
        'item(key = "nw47-recent-shelf")',
        'NightWaveRecentShelfTile(',
        'onRemove = { onRemoveRecent(song) }',
        'onOptions = { onSongOptions(song) }',
        'onPlay = { onPlay(song, recent) }',
        '"Discover music", openSearch',
        'item(key = "nw47-home-library-stats")',
        'NightWaveLibraryNumbers(',
        'if (mixes.isNotEmpty()) {',
        'onSearch(shortcut.query)',
        'openLikedSongs',
        'openOffline',
    )
    for token in required:
        if token not in home:
            raise AssertionError("Home action missing: " + token)
    ordered = (
        'nightwave-premium-home-header',
        'nw47-home-quick-access',
        'recent-heading',
        'nw47-recent-shelf',
        'nw47-home-library-stats',
    )
    offsets = [home.index(t) for t in ordered]
    if offsets != sorted(offsets):
        raise AssertionError("Recently played must precede optional counters")
    if "ListeningSpotlight(" in home:
        raise AssertionError("Duplicate Home last-played card returned")
    if 'onSearch("night drive music")' in home:
        raise AssertionError("Duplicate spotlight's fabricated mood search returned")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    source = STUDIO.read_text(encoding="utf-8")
    verify(source)
    if args.self_test:
        for old, new in (
            ('NightWaveRecentShelfTile(', 'UnusedShelf('),
            ('item(key = "nw47-home-library-stats")', 'item(key = "hidden-stats")'),
            ('onResumeSpotlight(it, recent)', 'onPlay(it, listOf(it))'),
        ):
            bad = source.replace(old, new, 1)
            try:
                verify(bad)
            except AssertionError:
                continue
            raise AssertionError("NW57 invalid Home layout fixture accepted")
        print("NW57 Home structure negative fixtures: PASS")
    print("NW57 first-fold content/actions source contract: PASS")
    print("UNVERIFIED: actual layout heights and frame rendering.")


if __name__ == "__main__":
    main()
