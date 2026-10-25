#!/usr/bin/env python3
"""NW48 source gate: downloads and offline health must stay non-destructive.

This is a source contract check with deliberate negative fixtures, NOT an
Android compiler result or end-to-end test of HTTP range resume.
"""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
OFFLINE = ROOT / "app/src/main/java/com/nightwave/player/offline"
TESTS = ROOT / "app/src/test/java/com/nightwave/player/offline"


def check(files: dict[str, str]) -> None:
    required = {
        "DownloadCenterViewPolicy.kt": [
            "enum class Filter { ALL, ACTIVE, READY, ATTENTION }",
            'workState == "SUCCEEDED" && availableFile',
            '(workState == "SUCCEEDED" && !availableFile)',
            "fun percent(receivedBytes: Long, expectedBytes: Long)",
            "receivedBytes.toDouble() * 100.0",
            "fun recentRate(",
            "nowMs - lastByteChangeMs > 8_000L"
        ],
        "CatalogDownloadsActivity.kt": [
            "DownloadCenterViewPolicy.Filter.entries",
            "DownloadCenterViewPolicy.matches(",
            "OfflineAudioLibrary(this).availableIds(allSavedIds)",
            "DownloadCenterViewPolicy.percent(received, total)",
            "DownloadCenterViewPolicy.recentRate(",
            "No new bytes · Waiting for source",
            "uiHandler.removeCallbacks(stagnantTransferRefresh)",
            "SearchOfflinePauseState.mark(this, info.id)",
            "SearchOfflinePauseState.clear(this@CatalogDownloadsActivity, info.id)",
            "confirmSearchRetry(retryUrl, title,",
            "DownloadHistoryVisibility.hide(this@CatalogDownloadsActivity, info.id)",
            "Downloaded",
            "onClick",
        ],
        "OfflineLibraryViewPolicy.kt": [
            "enum class Filter { ALL, READY, ATTENTION }",
            "health == OfflineFileHealth.AVAILABLE",
            "health != OfflineFileHealth.AVAILABLE",
            "No verified playable audio",
        ],
        "OfflineLibraryActivity.kt": [
            "OfflineLibraryViewPolicy.Filter.entries",
            "OfflineLibraryViewPolicy.includes(",
            "OfflineLibraryViewPolicy.emptyMessage(",
            "library.healthReport()",
            "library.delete(song)",
            "if (!report.indexReadable)",
            "onResume()",
            "Play all visible offline tracks",
            "OfflineSessionStore(this).read()",
            "LinearLayout.LayoutParams(dp(48), dp(48))",
        ],
        "CatalogDownloadWorker.kt": [
            "received < lastBytes",
            "transfer.download(",
            "FreeAudioIntegrity",
        ],
        "DirectAudioDownloadWorker.kt": [
            "received < lastProgress",
            "downloader.download(source)",
        ],
        "SearchTrackSaveWorker.kt": [
            "received < lastReported",
            "SearchTrackOfflineIndex.linkVerified",
            "downloadResolvedSearchAudio(",
        ],
        "PermittedAudioDownloader.kt": [
            "require(!Files.isSymbolicLink(staging.toPath()))",
            "inspectAudioOnly(pending)",
            "pending.delete()",
            ".followRedirects(false)",
            "isPublicAddress",
            "PermittedAudioDownloadPolicy.MAX_BYTES",
        ],
        "DownloadCenterViewPolicyTest.kt": [
            "statusDependsOnActualFileNotWorkManagerSuccessAlone",
            "percentCannotOverflowOrMisreportInvalidTotals",
            "frozenThroughputIsNotMisreportedAsCurrentSpeed",
        ],
        "OfflineLibraryViewPolicyTest.kt": [
            "onlyAvailableFilesCanAppearAsReady",
            "emptyMessagesDistinguishLostFilesFromNoSavedSongs",
        ],
    }
    for name, markers in required.items():
        body = files[name]
        for marker in markers:
            if marker not in body:
                raise AssertionError(f"{name}: missing {marker!r}")
    center = files["CatalogDownloadsActivity.kt"]
    if center.index("DownloadCenterViewPolicy.matches(") > center.index(
            "}.take(60)"):
        raise AssertionError("Filter must run before the visible-job limit")
    if "Resume starts from saved partial bytes" in center:
        raise AssertionError("NW48 must not falsely promise byte-range resume")
    worker = files["SearchTrackSaveWorker.kt"]
    if worker.index("received < lastReported") > worker.index(
            "setProgressAsync(Data.Builder()"):
        raise AssertionError("Reset on clean retries must precede progress reporting")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    names = [
        "DownloadCenterViewPolicy.kt", "CatalogDownloadsActivity.kt",
        "OfflineLibraryViewPolicy.kt", "OfflineLibraryActivity.kt",
        "CatalogDownloadWorker.kt", "DirectAudioDownloadWorker.kt",
        "SearchTrackSaveWorker.kt", "PermittedAudioDownloader.kt",
        "DownloadCenterViewPolicyTest.kt", "OfflineLibraryViewPolicyTest.kt",
    ]
    files = {
        name: ((TESTS if name.endswith("Test.kt") else OFFLINE) / name)
            .read_text(encoding="utf-8")
        for name in names
    }
    check(files)
    if args.self_test:
        def rejected(file: str, search: str, replacement: str) -> None:
            tampered = dict(files)
            if search not in tampered[file]:
                raise AssertionError("Fixture lost target: " + search)
            tampered[file] = tampered[file].replace(search, replacement, 1)
            try:
                check(tampered)
            except AssertionError:
                return
            raise AssertionError("Unsafe NW48 fixture was accepted")
        rejected("DownloadCenterViewPolicy.kt",
                 'workState == "SUCCEEDED" && availableFile',
                 'workState == "SUCCEEDED"')
        rejected("CatalogDownloadWorker.kt", "received < lastBytes", "false")
        rejected("OfflineLibraryViewPolicy.kt",
                 "health == OfflineFileHealth.AVAILABLE", "true")
        rejected("OfflineLibraryActivity.kt",
                 "library.healthReport()", "library.songs()")
        rejected("SearchTrackSaveWorker.kt",
                 "received < lastReported", "false")
        print("NW48 negative fixtures: PASS")
    print("NW48 source guard: PASS (does not certify Gradle build or true byte resume)")


if __name__ == "__main__":
    main()
