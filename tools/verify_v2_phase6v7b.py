#!/usr/bin/env python3
"""Static 6V7B premium Search/Library wiring audit, NOT an Android compilation."""
import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "app/src/main/java/com/nightwave/player/v2/ui/"
REQUIRED = {
    PREFIX+"NightWaveCollectionChrome.kt": [
        "enum class NightWaveTrackSurface", "HOME, SEARCH, LIBRARY",
        "object NightWaveCollectionChrome", "TRACK_ARTWORK_DP = 52",
        "COLLECTION_ARTWORK_DP = 56", "fun sectionSubtitle(",
        "fun sourceLabel(", "fun lineCount(",
    ],
    PREFIX+"NightWaveDiscoveryChrome.kt": [
        "fun NightWaveDiscoveryHeader(", "fun NightWaveCollectionHeader(",
        '"nightwave-discovery-header"', '"nightwave-collection-header"',
        'testTag("nightwave-save-offline-search")',
        "FreeAudioCatalogActivity::class.java",
        "BorderStroke(1.dp, palette.primary.copy(alpha = .27f))",
        "Icons.Outlined.LibraryMusic",
    ],
    PREFIX+"NightWavePremiumDesignTokens.kt": [
        "const val ROW_ARTWORK_DP = 52",
    ],
    PREFIX+"StudioComponents.kt": [
        "surface: NightWaveTrackSurface = NightWaveTrackSurface.HOME",
        '"nightwave-track-" + surface.name.lowercase()',
        "TrackArtwork(video, Modifier.size(NightWavePremiumDesignTokens.ROW_ARTWORK_DP.dp)",
        "NightWaveCollectionChrome.COLLECTION_ARTWORK_DP.dp",
        '"nightwave-library-collection"',
        "AsyncImage(model = video.thumbnail",
        ".clickable(onClick = onClick)",
        "IconButton(onClick = onOptions",
    ],
    PREFIX+"NightWaveStudioActivity.kt": [
        "NightWaveDiscoveryHeader(searchQuery = query)", "NightWaveCollectionHeader(",
        "surface = NightWaveTrackSurface.SEARCH",
        "surface = NightWaveTrackSurface.LIBRARY",
        "SavedTrackSearch.match(query, savedTracks)",
        "LibraryBrowsePolicy.playlists(", "LibraryBrowsePolicy.songs(",
        "onClick = { onPlay(song, listOf(song)) }",
        "onSelectPlaylist(collection.name)",
        "onSelectPlaylist(LIKED_COLLECTION)",
        "onSearch = { navigateToTab(StudioTab.SEARCH) }",
    ],
    "tools/build-windows.ps1": [
        "check-phase6v7b-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ],
    "tools/check-builder-syntax.ps1": ["check-phase6v7b-integration.ps1"],
    ".github/workflows/android.yml": [
        "python3 tools/verify_v2_phase6v7b.py --self-test",
        "python3 tools/verify_v2_phase6v7b.py",
    ],
}
TESTS = {
    "app/src/test/java/com/nightwave/player/v2/ui/NightWaveCollectionChromeTest.kt": 8,
    "app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveSearchLibraryChromeInstrumentedTest.kt": 3,
    "app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveSwipeTabsInstrumentedTest.kt": 2,
    "app/src/androidTest/java/com/nightwave/player/v2/ui/NightWavePremiumChromeInstrumentedTest.kt": 3,
}

def audit(root):
    errors=[]
    for rel, tokens in REQUIRED.items():
        p=root/rel
        if not p.is_file():
            errors.append("Missing: "+rel)
            continue
        src=p.read_text(encoding="utf-8")
        for token in tokens:
            if token not in src:
                errors.append(rel+": missing "+repr(token))
    for rel, n in TESTS.items():
        p=root/rel
        if not p.is_file():
            errors.append("Missing test: "+rel)
        elif p.read_text(encoding="utf-8").count("@Test fun ")<n:
            errors.append(rel+": too few test declarations")
    return errors

def self_test():
    with tempfile.TemporaryDirectory() as tmp:
        root=Path(tmp)
        for rel, tokens in REQUIRED.items():
            p=root/rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("\n".join(tokens), encoding="utf-8")
        for rel,n in TESTS.items():
            p=root/rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("@Test fun placeholder() {}\n"*n, encoding="utf-8")
        assert audit(root)==[]
        p=root/(PREFIX+"NightWaveStudioActivity.kt")
        p.write_text(p.read_text().replace(
            "LibraryBrowsePolicy.songs(", "unsafe_reorder("
        ),encoding="utf-8")
        assert any("LibraryBrowsePolicy.songs" in e for e in audit(root))
        header = root/(PREFIX+"NightWaveDiscoveryChrome.kt")
        header.write_text(header.read_text(encoding="utf-8").replace(
            'testTag("nightwave-save-offline-search")',
            'testTag("offline-action-regression")'
        ),encoding="utf-8")
        assert any("nightwave-save-offline-search" in e for e in audit(root))
        card = root/(PREFIX+"StudioComponents.kt")
        card.write_text(card.read_text(encoding="utf-8").replace(
            "NightWavePremiumDesignTokens.ROW_ARTWORK_DP.dp", "28.dp"
        ), encoding="utf-8")
        assert any("NightWavePremiumDesignTokens.ROW_ARTWORK_DP.dp" in e
                   for e in audit(root))
    print("PASS: 6V7B synthetic audit catches loss of existing library sorting")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--root",type=Path,default=ROOT)
    p.add_argument("--self-test",action="store_true")
    a=p.parse_args()
    if a.self_test:
        self_test()
        return 0
    errors=audit(a.root)
    for error in errors: print("FAIL:",error)
    if errors: return 1
    print("PASS: Search/Library presentation wiring and test declarations present")
    print("NOT VERIFIED: JVM/Compose execution, Android APK, physical device")
    return 0

if __name__=="__main__":
    raise SystemExit(main())
