#!/usr/bin/env python3
"""NW62: Downloads must verify real offline files without blocking UI.

Source contracts and negative fixtures only: does NOT certify device latency,
real file readiness, animation smoothness or Android compilation.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/nightwave/player/offline"
TESTS = ROOT / "app/src/test/java/com/nightwave/player/offline"


def verify(activity: str, policy: str, test: str) -> None:
    activity_markers = (
        "import androidx.lifecycle.lifecycleScope",
        "import kotlinx.coroutines.Dispatchers",
        "import kotlinx.coroutines.Job",
        "import kotlinx.coroutines.withContext",
        "private var healthRefreshJob: Job? = null",
        "private var healthReadGeneration = 0L",
        "private var uiPaused = false",
        "val generation = ++healthReadGeneration",
        "healthRefreshJob?.cancel()",
        "withContext(Dispatchers.IO) {",
        "OfflineAudioLibrary(applicationContext).availableIds(savedIds)",
        "DownloadHealthRefreshPolicy.mayPublish(",
        "render(snapshot, available)",
        "private fun render(infos: List<WorkInfo>, availableIds: Set<String>)",
        "override fun onPause() {",
        "uiPaused = true",
        "++healthReadGeneration",
        "override fun onResume() {",
        "uiPaused = false",
        "OfflineDownloadStatusPolicy.isReady(",
        "DownloadCenterViewPolicy.matches(",
        "DownloadHistoryVisibility.hide(",
        "confirmSearchPause(info)",
        "confirmSearchRetry(retryUrl, title,",
    )
    for marker in activity_markers:
        if marker not in activity:
            raise AssertionError("Activity missing " + marker)

    refresh = activity.split("private fun refreshJobs() {", 1)[1].split(
        "override fun onResume()", 1
    )[0]
    if refresh.find("withContext(Dispatchers.IO)") < 0 or (
        refresh.find("OfflineAudioLibrary(applicationContext).availableIds(savedIds)")
            < refresh.find("withContext(Dispatchers.IO)")
    ):
        raise AssertionError("Physical file validation is no longer on Dispatchers.IO")

    render = activity.split(
        "private fun render(infos: List<WorkInfo>, availableIds: Set<String>)", 1
    )[1].split("private fun confirmSearchPause(", 1)[0]
    for forbidden in ("OfflineAudioLibrary(this).availableIds(",
                      "OfflineAudioLibrary(applicationContext).availableIds(",
                      "library.healthReport()"):
        if forbidden in render:
            raise AssertionError("Main-thread Download cards access private disk metadata")
    if "info.state == WorkInfo.State.SUCCEEDED, savedId(info), availableIds)" not in render:
        raise AssertionError("Ready offline must be validated against physical files")

    for x in (
        "fun mayPublish(",
        "submittedGeneration == currentGeneration",
        "!paused && !finishing && !destroyed",
    ):
        if x not in policy:
            raise AssertionError("Generation fence missing: " + x)
    for x in (
        "onlyLatestDiskHealthSnapshotMayUpdateScreen",
        "pausedOrDestroyedScreenRejectsLateFileResults",
        "resumeMustUseNewGenerationRatherThanOldCachedRead",
    ):
        if x not in test:
            raise AssertionError("Download IO result fence test missing " + x)


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--self-test", action="store_true")
    args = p.parse_args()
    sources = [
        (SRC / "CatalogDownloadsActivity.kt").read_text(encoding="utf-8"),
        (SRC / "DownloadHealthRefreshPolicy.kt").read_text(encoding="utf-8"),
        (TESTS / "DownloadHealthRefreshPolicyTest.kt").read_text(encoding="utf-8"),
    ]
    verify(*sources)
    if args.self_test:
        mutations = (
            (0, "withContext(Dispatchers.IO)", "withContext(Dispatchers.Main)"),
            (0, "render(snapshot, available)", "render(snapshot, emptySet())"),
            (0, "++healthReadGeneration", "// removed stop fence"),
            (0, "info.state == WorkInfo.State.SUCCEEDED, savedId(info), availableIds)",
             "info.state == WorkInfo.State.SUCCEEDED, savedId(info), emptySet())"),
            (1, "submittedGeneration == currentGeneration", "true"),
            (1, "!paused && !finishing && !destroyed", "true"),
            (2, "pausedOrDestroyedScreenRejectsLateFileResults", "testRemoved"),
        )
        for i, old, new in mutations:
            broken = list(sources)
            assert old in broken[i], old
            broken[i] = broken[i].replace(old, new, 1)
            try:
                verify(*broken)
            except AssertionError:
                continue
            raise AssertionError("Unsafe NW62 fixture accepted: " + old)
        print("NW62 stale-disk-result negative fixtures: PASS")
    print("NW62 async download health and verified readiness source contracts: PASS")
    print("UNVERIFIED: Gradle/JUnit execution and device frame-time improvements.")


if __name__ == "__main__":
    main()
