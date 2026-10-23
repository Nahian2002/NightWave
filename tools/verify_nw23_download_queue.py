#!/usr/bin/env python3
"""NW23 source-level regression gate, not a substitute for Android compilation."""
from pathlib import Path
import argparse
import re

ROOT = Path(__file__).resolve().parents[1]

FILES = {
    "queue": "app/src/main/java/com/nightwave/player/offline/CatalogDownloadQueue.kt",
    "identity": "app/src/main/java/com/nightwave/player/offline/CatalogWorkIdentity.kt",
    "worker": "app/src/main/java/com/nightwave/player/offline/CatalogDownloadWorker.kt",
    "screen": "app/src/main/java/com/nightwave/player/offline/CatalogDownloadsActivity.kt",
    "catalog": "app/src/main/java/com/nightwave/player/offline/FreeAudioCatalogActivity.kt",
    "library": "app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt",
    "manifest": "app/src/main/AndroidManifest.xml",
    "tests": "app/src/test/java/com/nightwave/player/offline/CatalogWorkIdentityTest.kt",
    "workflow": ".github/workflows/android.yml",
}

def check(source: dict[str, str]) -> list[str]:
    errors = []
    def require(condition: bool, msg: str) -> None:
        if not condition:
            errors.append(msg)
    q = source["queue"]; w = source["worker"]; u = source["screen"]
    require("ExistingWorkPolicy.KEEP" in q, "Duplicate unfinished queue work must be blocked")
    require("NetworkType.CONNECTED" in q, "Queue must require network")
    require("BackoffPolicy.EXPONENTIAL" in q, "Network recovery must back off")
    require("CatalogWorkIdentity.uniqueName(url, sha1)" in q, "Use a deterministic source key")
    require("FreeAudioCatalogPolicy.permittedLicense(entry.licenseName)" in q,
            "Reject unlicensed catalog items")
    require("FreeAudioCatalogPolicy.safeMediaUrl(entry.sourceUrl)" in q,
            "Reject untrusted catalog source URLs")
    require("FreeAudioIntegrity.isValidSourceHash(entry.sha1Base36)" in q,
            "Missing required source checksum")
    require("CatalogDownloadQueue.valid(candidate)" in w, "Worker must revalidate work input")
    require("Network transfer failed; retry from the catalog" in w,
            "Network failures must have a bounded outcome")
    require("runAttemptCount < 2" in w, "Network retries must be bounded")
    require("setForegroundAsync" in w, "Foreground transfer must announce its progress")
    require("override fun onStopped()" in w and "currentDownload?.cancel()" in w,
            "Stopping the work must interrupt the active HTTP request")
    require("candidate.sha1Base36, candidate.bytes" in w,
            "Download must verify bytes and expected original checksum")
    require("WorkManager.getInstance" in u and "getWorkInfosByTagLiveData" in u,
            "Queue needs observable WorkManager progress")
    require("cancelWorkById(info.id)" in u, "Queue must support per-job cancellation")
    require("CatalogDownloadQueue.enqueue(this, entry)" in source["catalog"],
            "Catalog Save action must reach the background queue")
    require("beginDownload(entry)" in source["catalog"],
            "Legacy foreground downloader must remain available")
    require("CatalogDownloadsActivity::class.java" in source["library"],
            "Offline Library must link to the queue screen")
    m = source["manifest"]
    require('android.permission.FOREGROUND_SERVICE_DATA_SYNC' in m,
            "Android 14+ requires dataSync foreground-service permission")
    require('android:name="androidx.work.impl.foreground.SystemForegroundService"' in m and
            'android:foregroundServiceType="dataSync"' in m,
            "WorkManager foreground service must be declared as dataSync")
    require(bool(re.search(r'android:name="\.offline\.CatalogDownloadsActivity"\s+'
            r'android:exported="false"', m)),
            "Download queue Activity must not be exported")
    require(source["tests"].count("@Test") >= 3, "Queue identity tests missing")
    require("tools/verify_nw23_download_queue.py" in source["workflow"],
            "CI must enforce NW23 source gate")
    for k in ("queue", "identity", "worker", "screen", "catalog"):
        imports = re.findall(r"^import\s+(\S+)", source[k], re.M)
        require(len(imports) == len(set(imports)), "Duplicate imports in " + k)
    return errors

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        fixture = {key: "" for key in FILES}
        assert check(fixture), "Empty repository must fail closed"
        print("PASS: NW23 source gate fails closed on incomplete source")
        return 0
    try:
        src = {key: (ROOT / path).read_text(encoding="utf-8")
               for key, path in FILES.items()}
    except (OSError, UnicodeError):
        print("FAIL: NW23 source ZIP missing or corrupted")
        return 1
    errors = check(src)
    for error in errors:
        print("FAIL: " + error)
    if errors:
        return 1
    print("PASS: NW23 WorkManager queue integration source contracts")
    print("UNVERIFIED: complete Gradle build, foreground behavior and device downloads")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
