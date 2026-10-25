#!/usr/bin/env python3
"""NW62 read-only source contract for accurate WorkManager retry history.

Does not inspect private audio, invoke WorkManager, or verify an APK.
"""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OFFLINE = ROOT / "app/src/main/java/com/nightwave/player/offline"
TEST = ROOT / "app/src/test/java/com/nightwave/player/offline"


def verify(policy: str, activity: str, tests: str) -> None:
    for token in (
        "object DownloadHistoryProjectionPolicy",
        "data class Row(",
        "fun select(rows: List<Row>, filter: DownloadCenterViewPolicy.Filter)",
        "filter { DownloadCenterViewPolicy.matches(filter, it.state, it.availableFile) }",
        ".sortedWith(compareBy<Row>",
        "row.retryKey == null || seen.add(row.retryKey)",
        "fun count(rows: List<Row>, filter: DownloadCenterViewPolicy.Filter)",
        "select(rows, filter).size",
    ):
        if token not in policy:
            raise AssertionError("History projection policy missing " + token)
    positions = (
        policy.index("filter { DownloadCenterViewPolicy.matches("),
        policy.index(".sortedWith(compareBy<Row>"),
        policy.index("seen.add(row.retryKey)"),
    )
    if positions != tuple(sorted(positions)):
        raise AssertionError("History must filter BEFORE deduplicating retries")
    for token in (
        "val projectedRows = displayInfos.map { info ->",
        "availableFile = isVerified(info)",
        "OfflineSaveRetryPolicy.retryUrl(info.tags)",
        "requestedAtMs = OfflineSaveRetryPolicy.requestedAt(info.tags)",
        "DownloadHistoryProjectionPolicy.count(",
        "DownloadHistoryProjectionPolicy.select(",
        ".take(60).mapNotNull { workById[it] }",
        "OfflineAudioLibrary(this).availableIds(allSavedIds)",
        "OfflineDownloadStatusPolicy.isReady(",
        "SearchOfflinePauseState.isMarked(this, info.id)",
        "manager.cancelWorkById(info.id)",
        "confirmSearchRetry(retryUrl, title,",
    ):
        if token not in activity:
            raise AssertionError("Download Center wiring missing " + token)
    if "seenSearchUrls" in activity:
        raise AssertionError("Old before-filter retry deduplication is back")
    for test in (
        "laterFailedRetryCannotHideOlderVerifiedFileInReadyTab",
        "currentActiveAttemptDoesNotEraseValidEarlierSavedFile",
        "failuresWithSameSourceDoNotCreateDuplicateIssueRows",
        "missingFileIsNotReadyEvenWhenWorkerSucceeded",
        "separateDirectAndCatalogRequestsKeepTheirOwnHistory",
        "activeFirstThenTimestampThenIdIsDeterministic",
        "noHistoriesAndNoEligibleResultsStayEmpty",
    ):
        if test not in tests:
            raise AssertionError("Missing NW62 JVM test " + test)
    if "private val f = DownloadCenterViewPolicy.Filter" in tests or "p.Filter." in tests:
        raise AssertionError("Kotlin enum class must not be treated as an instance value")


def main() -> None:
    cli = argparse.ArgumentParser()
    cli.add_argument("--self-test", action="store_true")
    args = cli.parse_args()
    sources = [
        (OFFLINE / "DownloadHistoryProjectionPolicy.kt").read_text(encoding="utf-8"),
        (OFFLINE / "CatalogDownloadsActivity.kt").read_text(encoding="utf-8"),
        (TEST / "DownloadHistoryProjectionPolicyTest.kt").read_text(encoding="utf-8"),
    ]
    verify(*sources)
    if args.self_test:
        for index, original, broken in (
            (0, "filter { DownloadCenterViewPolicy.matches(filter, it.state, it.availableFile) }",
             "filter { true }"),
            (0, "row.retryKey == null || seen.add(row.retryKey)", "true"),
            (1, "availableFile = isVerified(info)", "availableFile = true"),
            (1, "DownloadHistoryProjectionPolicy.select(", "listOf("),
            (2, "laterFailedRetryCannotHideOlderVerifiedFileInReadyTab",
             "noReadyRegressionTest"),
        ):
            modified = sources[:]
            assert original in modified[index]
            modified[index] = modified[index].replace(original, broken, 1)
            try:
                verify(*modified)
            except AssertionError:
                pass
            else:
                raise AssertionError("NW62 gate accepted broken fixture: " + original)
        print("NW62 download history negative fixtures: PASS")
    print("NW62 WorkManager history UI/source contracts: PASS")
    print("UNVERIFIED: Android build, live WorkManager state and device rendering.")


if __name__ == "__main__":
    main()
