#!/usr/bin/env python3
"""NW40 source-integration gate (standard library only).

Checks wiring and invariants introduced by NW36–NW40. This is NOT an Android
compiler, device test, security audit, or authorization/download proof.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATHS = {
    "activity": "app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt",
    "mutation": "app/src/main/java/com/nightwave/player/library/LibraryMutationPlan.kt",
    "facade": "app/src/main/java/com/nightwave/player/library/MusicLibrary.kt",
    "writer": "app/src/main/java/com/nightwave/player/library/LegacyLibraryTransaction.kt",
    "history": "app/src/main/java/com/nightwave/player/v2/ui/SearchHistoryStore.kt",
    "pins": "app/src/main/java/com/nightwave/player/v2/library/PlaylistPinStore.kt",
    "gesture": "app/src/main/java/com/nightwave/player/v2/library/PlaylistReorderGesturePolicy.kt",
    "build": "app/build.gradle.kts",
    "unit": "app/src/test/java/com/nightwave/player/library/LibraryMutationPlanTest.kt",
    "history_test": "app/src/androidTest/java/com/nightwave/player/v2/ui/SearchHistoryStoreInstrumentedTest.kt",
}

def audit(sources: dict[str, str]) -> list[tuple[str, bool]]:
    """Pure checks; also easy to exercise with intentionally damaged fixtures."""
    a = sources["activity"]
    m = sources["mutation"]
    h = sources["history"]
    result: list[tuple[str, bool]] = []
    def check(name: str, passed: bool) -> None:
        result.append((name, bool(passed)))

    check("All NW39 guarded operation cases exist", all(
        "is LibraryChange." + name + " ->" in m for name in (
            "BulkRemoveFromPlaylist", "BulkAddToPlaylist",
            "ReorderPlaylist", "RestorePlaylistIfUnchanged")))
    check("Bulk operations guard exact playlist URL snapshot",
        m.count("tracks.map { it.url } != operation.expectedUrls") >= 2)
    check("Undo refuses changed target playlist",
        "old.songs(operation.name) != operation.expected" in m)
    check("Reorder requires exact permutation",
        "operation.orderedUrls.toSet() != current.toSet()" in m)
    check("Playlist edits use existing guarded legacy writer",
        all("LegacyLibraryTransaction.mutate(app," in sources["facade"][
            sources["facade"].index("fun bulkRemoveFromPlaylist"):]))
    check("Existing writer retains recovery commit",
        "if (!saved)" in sources["writer"] and "if (!editor.commit())" in sources["writer"])
    check("Dedicated drag gestures and accessible move controls exist",
        "rememberDraggableState" in a and
        'contentDescription = "Move " + song.title + " up"' in a and
        'contentDescription = "Move " + song.title + " down"' in a)
    check("UI has guarded bulk copy and Undo callback",
        "val latestSource = library.playlist(fromName)" in a and
        "restorePlaylistIfUnchanged(" in a)
    check("Source clipboard/share don't expose file paths",
        "VideoLink.normalize(it.url)" in a and
        'Intent.createChooser(share, "Share selected songs")' in a)
    check("Offline song menu guards index lookup failure",
        "val offlineReady = runCatching {" in a)
    check("Pinned playlists refresh on Activity resume",
        "pinnedPlaylistNames = pinStore.snapshot(library.playlistNames())" in a)
    check("Search history parsing rejects corruption",
        "private fun decode(): List<String>?" in h and
        "require(json.length() <= SearchHistoryPolicy.MAX_ENTRIES)" in h)
    check("All search mutations fail closed on corrupted input",
        h.count("decode() ?: return emptyList()") >= 4)
    check("Instrumentation uses isolated test preferences",
        "private class TestContext" in sources["history_test"] and
        "name + suffix" in sources["history_test"])
    check("Pinned playlists use separate preferences",
        '"nightwave_playlist_pins_v1"' in sources["pins"] and
        '"nightwave_library_v1"' not in sources["pins"])
    check("Legacy song and platform app identity unchanged",
        'applicationId = "com.nightwave.player"' in sources["build"] and
        "versionCode = 35" in sources["build"])
    check("NW39 regressions have executable test methods",
        all("fun " + name in sources["unit"] for name in (
            "bulkRemovalIsAtomicAndKeepsAllOtherCollections",
            "reorderOnlyAllowsExactPermutationAndStaleSourceIsRejected",
            "conditionalUndoRestoresOnlyUnchangedPlaylistAndMetadata")))
    return result

def self_test() -> bool:
    sample = {key: "" for key in PATHS}
    baseline = audit(sample)
    assert baseline and not any(ok for _, ok in baseline)
    assert len({name for name, _ in baseline}) == len(baseline)
    assert all(isinstance(ok, bool) for _, ok in baseline)
    return True

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        print("PASS: NW40 integration gate self-test")
        return 0
    missing = [path for path in PATHS.values() if not (ROOT / path).is_file()]
    if missing:
        print("FAIL: Required source files missing:")
        for path in missing:
            print("  " + path)
        return 1
    sources = {key: (ROOT / path).read_text(encoding="utf-8")
        for key, path in PATHS.items()}
    checks = audit(sources)
    failed = [name for name, passed in checks if not passed]
    if args.json:
        print(json.dumps({"type": "source-only", "passed": len(checks) - len(failed),
                          "total": len(checks), "failed": failed}, indent=2))
    else:
        for name, passed in checks:
            print(("PASS" if passed else "FAIL") + ": " + name)
        print(f"NW40 source-only: {len(checks)-len(failed)}/{len(checks)}")
    if failed:
        print("This integration audit failed. No Android build claim is possible.")
        return 1
    print("Source gates pass. Android Gradle compile, unit tests and device QA still required.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
