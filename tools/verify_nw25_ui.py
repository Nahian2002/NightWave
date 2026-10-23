#!/usr/bin/env python3
"""NW25 read-only integration checks. Never certify an unbuilt Android APK."""
from __future__ import annotations
import argparse
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
PATHS = {
    "policy": "app/src/main/java/com/nightwave/player/v2/ui/NightWaveAdaptiveChromePolicy.kt",
    "navigation": "app/src/main/java/com/nightwave/player/v2/ui/NightWavePremiumChrome.kt",
    "player": "app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt",
    "search": "app/src/main/java/com/nightwave/player/v2/ui/NightWaveDiscoveryChrome.kt",
    "tiles": "app/src/main/java/com/nightwave/player/v2/ui/StudioComponents.kt",
    "library": "app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt",
    "catalog": "app/src/main/java/com/nightwave/player/offline/FreeAudioCatalogActivity.kt",
    "downloads": "app/src/main/java/com/nightwave/player/offline/CatalogDownloadsActivity.kt",
    "direct": "app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadActivity.kt",
    "tests": "app/src/test/java/com/nightwave/player/v2/ui/NightWaveAdaptiveChromePolicyTest.kt",
    "smoke": "tools/kotlin/NW25AdaptiveChromeSmoke.kt",
    "runner": "tools/nw25_adaptive_chrome_smoke.py",
    "workflow": ".github/workflows/android.yml",
    "manifest": "app/src/main/AndroidManifest.xml",
}

def inspect(data: dict[str, str]) -> list[str]:
    errors: list[str] = []
    def require(ok: bool, description: str) -> None:
        if not ok:
            errors.append(description)
    p = data["policy"]; nav = data["navigation"]
    player = data["player"]; search = data["search"]
    tiles = data["tiles"]; library = data["library"]
    downloads = data["downloads"]; direct = data["direct"]
    require("fun artworkSizeDp(" in p and ".coerceAtLeast(0f)" in p and
            "isFinite()" in p, "Artwork policy must clamp split-screen dimensions")
    require("fun navHeightDp(" in p and "fun homeHeroHeightDp(" in p and
            "fun nativeRowHeightDp(" in p and "fun miniPlayerHeightDp(" in p,
            "Adaptive sizing variants missing")
    require("val navHeight = NightWaveAdaptiveChromePolicy.navHeightDp" in nav and
            ".height(navHeight)" in nav, "Navigation must adapt to large text")
    require("homeHeroHeightDp(fontScale)" in nav and
            'testTag("nightwave-home-hero")' in nav,
            "Home greeting needs responsive height and retained semantic tag")
    require("val compactRowHeight = NightWaveAdaptiveChromePolicy.miniPlayerHeightDp" in player and
            ".height(compactRowHeight)" in player,
            "Mini-player must grow when accessibility font increases")
    require("NightWaveAdaptiveChromePolicy.artworkSizeDp(" in player and
            "contentPaddingDp.dp" in player, "Now Playing dimensions must be clamped")
    require("val art = minOf(maxWidth - 58.dp" not in player,
            "Old potentially negative player artwork sizing still exists")
    require("actions::playPause" in player and "actions::next" in player and
            "onDismiss()" in player, "Primary player controls changed unexpectedly")
    require('testTag("nightwave-save-offline-search")' in search and
            "heightIn(min = 52.dp)" in search and "FreeAudioCatalogActivity::class.java" in search,
            "Music discovery must preserve full-width 52dp offline shortcut")
    require("heightIn(min = NightWaveCollectionChrome.COLLECTION_MIN_HEIGHT_DP.dp)" in tiles and
            "heightIn(min = NightWaveCollectionChrome.TRACK_MIN_HEIGHT_DP.dp)" in tiles,
            "Collection/track cards must expand with large font")
    require("pageSongs.forEach { song ->" in library and
            "NativeChrome" not in library, "NW24 library page rendering must remain")
    require("nativeRowHeightDp(" in library and
            "NightWaveSafeInsets.bind(frame)" in library,
            "Offline Library responsive rows/safe insets missing")
    require("NightWaveSafeInsets.bind(frame)" in downloads and
            "getWorkInfosByTagLiveData" in downloads,
            "Downloads screen layout/queue live data missing")
    require("NightWaveSafeInsets.bind(frame)" in direct and
            "total != null && total > 0L" in direct,
            "Direct-audio downloader responsive layout and zero-total progress safety missing")
    require("NightWaveSafeInsets.bind(frame)" in data["catalog"],
            "Licensed catalog must retain safe insets")
    require(data["tests"].count("@Test") >= 5 and
            "28 exact-production Kotlin adaptive chrome assertions" in data["smoke"],
            "UI JUnit/smoke tests missing")
    require("tools/nw25_adaptive_chrome_smoke.py" in data["workflow"] and
            "tools/verify_nw25_ui.py" in data["workflow"] and
            "feature/nightwave-v2-nw25-adaptive-premium-ui" in data["workflow"],
            "CI must target NW25 and run source/production policy checks")
    require('applicationId' not in data["manifest"] and
            'android:allowBackup="false"' in data["manifest"],
            "Manifest changed in unexpected way (backup/permissions)")
    for name in ("policy", "navigation", "player", "search", "tiles",
                 "library", "downloads", "direct"):
        imports = re.findall(r"^import\s+(\S+)", data[name], re.M)
        require(len(imports) == len(set(imports)), "Duplicate exact imports in " + name)
    return errors

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        empty = {key: "" for key in PATHS}
        assert len(inspect(empty)) >= 15
        print("PASS: NW25 UI source gate fails closed on missing features")
        return 0
    try:
        data = {key: (ROOT / path).read_text(encoding="utf-8")
                for key, path in PATHS.items()}
    except (OSError, UnicodeError):
        print("FAIL: NW25 complete source ZIP unavailable or invalid UTF-8")
        return 1
    errors = inspect(data)
    for error in errors:
        print("FAIL:", error)
    if errors:
        return 1
    print("PASS: NW25 responsive UI integration contracts")
    print("UNVERIFIED: Android Compose/native compilation, screenshots and device behavior")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
