#!/usr/bin/env python3
"""6V7B premium Search/Library source and regression check.

Static wiring validation only; never calls Android, Google Drive or Media3.
Does not establish that Compose builds or renders correctly.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
UI = "app/src/main/java/com/nightwave/player/v2/ui/"
TEST = "app/src/test/java/com/nightwave/player/v2/ui/"
ANDROID = "app/src/androidTest/java/com/nightwave/player/v2/ui/"

REQUIRED = {
    UI + "NightWaveBrowseChromePolicy.kt": (
        "object NightWaveBrowseChromePolicy",
        "COMPACT_WIDTH_DP = 440f", "TRACK_ART_DP = 55",
        "COLLECTION_ART_DP = 54",
        "fun countLabel(", "fun libraryCounts(",
        "fun showCompactCollections(",
        "Source.SAVED_LIBRARY", "Source.PUBLIC_VIDEO",
        "Saved in your library · playback may need internet", "internet required",
    ),
    UI + "NightWavePremiumBrowse.kt": (
        "fun NightWaveBrowseHeader(",
        "fun NightWaveSearchSourceHeading(",
        "nightwave-search-header",
        "nightwave-library-header",
        "nightwave-saved-results-count",
        "nightwave-public-results-count",
        "NightWaveBrowseChromePolicy.sourceDetail(source)",
        "NightWaveVisualLanguage.accent(key)",
    ),
    UI + "StudioComponents.kt": (
        "fun MusicTrackTile(",
        "fun MusicCollectionTile(",
        "NightWaveBrowseChromePolicy.TRACK_ART_DP.dp",
        "NightWaveBrowseChromePolicy.COLLECTION_ART_DP.dp",
        "NightWaveBrowseChromePolicy.MIN_TILE_HEIGHT_DP.dp",
        "TrackArtwork(video,",
        "TrackArtwork(cover,",
        "contentDescription = \"Artwork for \" + video.title",
        "IconButton(onClick = onOptions",
    ),
    UI + "NightWaveStudioActivity.kt": (
        "private fun StudioSearch(",
        "private fun StudioLibrary(",
        "NightWaveBrowseHeader(",
        "NightWaveSearchSourceHeading(",
        "NightWaveBrowseChromePolicy.Source.SAVED_LIBRARY",
        "NightWaveBrowseChromePolicy.Source.PUBLIC_VIDEO",
        "NightWaveBrowseChromePolicy.libraryCounts(",
        "NightWaveBrowseChromePolicy.showCompactCollections(",
        "nightwave-collection-shortcuts",
        "nightwave-search-field",
        "responseMatchesQuery",
        "LibraryBrowsePolicy.playlists(",
        "LibraryBrowsePolicy.songs(",
        "LibraryQuickPlayPolicy.plan(",
        "onSongOptions(song)",
        "onPlay(song,",
        "onSharePlaylist(selectedPlaylist, tracks)",
        "onImportSharedPlaylist",
        "onOffline",
    ),
    "tools/build-windows.ps1": (
        "check-phase6v7b-integration.ps1",
        "check-phase6v7a-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ),
    "tools/check-builder-syntax.ps1": ("check-phase6v7b-integration.ps1",),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v7b.py --self-test",
        "python3 tools/verify_v2_phase6v7b.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
}
TESTS = {
    TEST + "NightWaveBrowseChromePolicyTest.kt": (
        9, ("narrowCollectionCardsStackVertically",
            "savedResultsNeverPretendToBePublicVideos",
            "publicResultsDoNotClaimTheyAreOffline")),
    ANDROID + "NightWavePremiumBrowseInstrumentedTest.kt": (
        4, ("narrowSearchHeroShowsRealPublicDiscoveryContext",
            "sourceSectionsTellLocalResultsFromPublicResults",
            "offlineCollectionClickDoesNotChangeAnyPlaybackState")),
    ANDROID + "NightWavePremiumChromeInstrumentedTest.kt": (
        3, ("premiumNavigationKeepsAllFourTappableDestinationsOnSmallScreens",)),
    ANDROID + "NightWaveSwipeTabsInstrumentedTest.kt": (
        2, ("swipesChangeTheSelectedBottomTab",)),
}

def audit(root: Path) -> list[str]:
    issues = []
    for path, tokens in REQUIRED.items():
        target = root / path
        if not target.is_file():
            issues.append("Missing source: " + path)
            continue
        data = target.read_text(encoding="utf-8")
        for token in tokens:
            if token not in data:
                issues.append(path + ": missing " + repr(token))
    for path, (minimum, methods) in TESTS.items():
        target = root / path
        if not target.is_file():
            issues.append("Missing regression: " + path)
            continue
        data = target.read_text(encoding="utf-8")
        if data.count("@Test fun ") < minimum:
            issues.append(path + f": need at least {minimum} methods")
        for name in methods:
            if f"fun {name}(" not in data:
                issues.append(path + ": missing test " + name)
    return issues

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-v7b-") as tmp:
        root = Path(tmp)
        for path, tokens in REQUIRED.items():
            f = root / path
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("\n".join(tokens), encoding="utf-8")
        for path, (n, methods) in TESTS.items():
            f = root / path
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("\n".join(
                [f"@Test fun synthetic{i}() {{}}" for i in range(n)] +
                [f"fun {m}() {{}}" for m in methods]),
                encoding="utf-8")
        assert audit(root) == [], audit(root)
        x = root / (UI + "NightWaveStudioActivity.kt")
        x.write_text(x.read_text(encoding="utf-8").replace(
            "onPlay(song,", "removed_song_action("), encoding="utf-8")
        assert any("onPlay(song" in issue for issue in audit(root))
        y = root / (UI + "NightWaveBrowseChromePolicy.kt")
        y.write_text(y.read_text(encoding="utf-8").replace(
            "internet required", "offline without download"), encoding="utf-8")
        assert any("internet required" in issue for issue in audit(root))
    print("PASS: synthetic 6V7B gate catches removed play action and misleading online labels")

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", type=Path, default=ROOT)
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        self_test()
        return 0
    issues = audit(args.root)
    for issue in issues:
        print("FAIL:", issue)
    if issues:
        return 1
    print("PASS: 6V7B premium browse UI and regression declarations wired")
    print("UNVERIFIED: Android Gradle, Compose, emulator and APK compatibility")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
