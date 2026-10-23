#!/usr/bin/env python3
"""Source-level NW24 compatibility gate. Android compilation remains mandatory."""
from __future__ import annotations
import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATHS = {
    "capacity": "app/src/main/java/com/nightwave/player/offline/OfflineLibraryCapacity.kt",
    "library": "app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt",
    "ui": "app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt",
    "session": "app/src/main/java/com/nightwave/player/offline/OfflineSessionCodec.kt",
    "cloud": "app/src/main/java/com/nightwave/player/v2/locker/LockerAudioPolicy.kt",
    "download": "app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloader.kt",
    "test": "app/src/test/java/com/nightwave/player/offline/OfflineLibraryCapacityTest.kt",
    "smoke": "tools/kotlin/NW24CapacitySmoke.kt",
    "runner": "tools/nw24_capacity_smoke.py",
    "manifest": "app/src/main/AndroidManifest.xml",
    "workflow": ".github/workflows/android.yml",
}

def audit(source: dict[str, str]) -> list[str]:
    defects: list[str] = []
    def require(value: bool, name: str) -> None:
        if not value: defects.append(name)
    cap = source["capacity"]
    lib = source["library"]
    ui = source["ui"]
    require("const val MAX_ITEMS = 5_000" in cap, "5,000 index cap missing")
    require("const val MAX_LIBRARY_BYTES = 32L * 1024L * 1024L * 1024L" in cap,
            "32GiB index cap missing")
    require("currentBytes in 0..MAX_LIBRARY_BYTES" in cap and
            "addedBytes <= MAX_LIBRARY_BYTES - currentBytes" in cap,
            "Overflow-safe capacity comparison missing")
    require("const val MAX_SONGS = OfflineLibraryCapacity.MAX_ITEMS" in lib and
            "const val MAX_LIBRARY_BYTES = OfflineLibraryCapacity.MAX_LIBRARY_BYTES" in lib,
            "Library old capacity constants remain")
    require("OfflineLibraryCapacity.safeIndex(raw)" in lib and
            "OfflineLibraryCapacity.safeIndex(serialized)" in lib,
            "Corrupt/oversized legacy JSON index guard missing")
    require('getSharedPreferences("nightwave_offline_v1"' in lib and
            'getString("items", "[]")' in lib and
            '.putString("items", encode(' in lib,
            "Legacy index identity/serialization changed")
    require("data class OfflineSong(" in lib and
            all('val ' + key + ':' in lib for key in ("id", "title", "mimeType", "bytes")),
            "Legacy IDs or song data shape changed")
    require("val hash = audioSha256(pending)" in lib and
            "return@synchronized existing" in lib,
            "Manual imports must reuse byte-identical tracks")
    require(lib.index("val hash = audioSha256(pending)") <
            lib.index("OfflineLibraryCapacity.acceptsNew(previous.size, total, count)"),
            "Duplicate matching must precede capacity gate")
    require("inspectFile(song).second?.let" in lib and
            "inspectFile(song).second ?: continue" in lib,
            "Import dedup must not reload the index per track")
    require("output.fd.sync()" in lib and
            "pending.renameTo(finalFile)" in lib and
            ".commit()" in lib,
            "Crash-tolerant staging and durable index commit missing")
    require("MAX_SONG_BYTES = 250L * 1024L * 1024L" in lib,
            "Existing 250MiB audio validation changed without QA")
    require("pageSongs.forEach { song ->" in ui and
            ".take(OfflineLibraryCapacity.pageSize())" in ui,
            "Large UI must render one bounded track page at a time")
    require("uiHandler.postDelayed(update, 220L)" in ui and
            "pendingSearch?.let(uiHandler::removeCallbacks)" in ui,
            "Search refresh must be debounced and cleaned up")
    require("playable.drop(selectedOffset)" in ui,
            "Playback must include selection from late pages")
    require('private const val VERSION = "NWOS1"' in source["session"],
            "Offline listening session schema unexpectedly changed")
    require("fun chooseRestoredId(" in source["cloud"],
            "Cloud-restored legacy song IDs must be preserved")
    require("importPermittedStagedAudio(pending, fileName)" in source["download"],
            "Licensed downloader must still import through offline library")
    require(source["test"].count("@Test") >= 4,
            "Capacity regression JUnit cases missing")
    require("PASS: NW24 24 production Kotlin library capacity assertions" in source["runner"] and
            "NW24CapacitySmoke" in source["smoke"],
            "Exact-source executable policy smoke missing")
    require('android:allowBackup="false"' in source["manifest"],
            "Private library automatic Android backup policy changed")
    require("python3 tools/verify_nw24_library.py" in source["workflow"] and
            "python3 tools/nw24_capacity_smoke.py" in source["workflow"],
            "New source and JVM checks not wired in CI")
    for name in ("capacity", "library", "ui"):
        body = source[name]
        imports = re.findall(r"^import\s+([^\s]+)", body, re.M)
        require(len(imports) == len(set(imports)), "Duplicate imports: " + name)
    return defects

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        bad = {key: "" for key in PATHS}
        assert len(audit(bad)) >= 15
        print("PASS: NW24 regression gate fails closed on incomplete source")
        return 0
    try:
        data = {name: (ROOT / path).read_text(encoding="utf-8")
                for name, path in PATHS.items()}
    except (OSError, UnicodeError):
        print("FAIL: NW24 full-source tree unavailable")
        return 1
    errors = audit(data)
    for error in errors:
        print("FAIL:", error)
    if errors: return 1
    print("PASS: NW24 library compatibility, capacity, paging and import contracts")
    print("UNVERIFIED: complete Android Gradle build and live device migration")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
