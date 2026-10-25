#!/usr/bin/env python3
"""NW62: read-only static regression gate for the Downloads health UI.

It verifies source contracts only, not Android file IO, Gradle or phone UI.
"""
from __future__ import annotations
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/nightwave/player/offline/CatalogDownloadsActivity.kt"
POLICY = ROOT / "app/src/main/java/com/nightwave/player/offline/DownloadFileVerificationPolicy.kt"
TEST = ROOT / "app/src/test/java/com/nightwave/player/offline/DownloadFileVerificationPolicyTest.kt"


def verify(ui: str, policy: str, tests: str) -> None:
    markers = {
        "ui": (
            "private val fileVerifier = ThreadPoolExecutor(",
            "ArrayBlockingQueue<Runnable>(1)",
            "ThreadPoolExecutor.DiscardOldestPolicy()",
            "private fun savedIds(infos: List<WorkInfo>)",
            "private fun requestFileVerification(infos: List<WorkInfo>)",
            "OfflineAudioLibrary(applicationContext).availableIds(ids)",
            "DownloadFileVerificationPolicy.needsRead(ids, requestedSavedIds)",
            "DownloadFileVerificationPolicy.canPublish(",
            "verificationGeneration++",
            "fileVerifier.shutdownNow()",
            "private fun refreshJobs()",
            "requestFileVerification(latest)",
            "private var verifyingFiles = false",
            "private var fileCheckFailed = false",
            "private var verifiedAvailableIds: Set<String> = emptySet()",
            "private fun savedIdFromOutput(info: WorkInfo)",
            "DownloadFileVerificationPolicy.ready(",
            "DownloadFileVerificationPolicy.needsAttention(",
            "Checking saved audio files",
            "Could not verify saved audio",
            "OfflineDownloadStatusPolicy.statusForCompleted(actuallySaved)",
            "OfflineAudioLibrary",
            "manager.getWorkInfosByTagLiveData(SearchTrackSaveQueue.TAG)",
            "confirmSearchPause(info)",
            "confirmSearchRetry(retryUrl, title,",
            "manager.cancelWorkById(info.id)",
        ),
        "policy": (
            "lastRequested == null || ids != lastRequested",
            "generation == newestGeneration",
            "!isFinishing && !isDestroyed",
            "!checking && !verificationFailed",
            "OfflineDownloadStatusPolicy.isReady",
            'workState == "SUCCEEDED" && (checking || verificationFailed)',
        ),
        "tests": (
            "repeatedProgressDoesNotRescanUnchangedSavedFileIds",
            "staleAndDestroyedActivitiesCannotApplyFileResults",
            "succeededIsNotReadyUntilRealFilesHaveBeenVerified",
            "unknownFileStatusIsNotFalselyClassifiedMissingOrBroken",
        )
    }
    for name, source in (("ui", ui), ("policy", policy), ("tests", tests)):
        for token in markers[name]:
            if token not in source:
                raise AssertionError(f"{name}: missing {token!r}")
    if "OfflineAudioLibrary(this).availableIds(allSavedIds)" in ui:
        raise AssertionError("Synchronous file verification returned to UI thread")
    if "val availableIds = OfflineAudioLibrary(" in ui:
        raise AssertionError("Downloads.render() must consume only a background snapshot")
    if "return OfflineDownloadStatusPolicy.isReady(" in ui:
        raise AssertionError("Ready must deny pending/stale/failed verification")
    code = ui.split("private fun render(infos: List<WorkInfo>)", 1)[1]
    if ".availableIds(" in code:
        raise AssertionError("Downloads.render() must not stat indexed files on the UI thread")
    if "if (fileCheckFailed)" not in ui and "fileCheckFailed ->" not in ui:
        raise AssertionError("Unverified file state must not be shown as broken audio")
    if "Render saved file as available" in ui:
        raise AssertionError("Mocked ready state")


def main() -> None:
    cli = argparse.ArgumentParser()
    cli.add_argument("--self-test", action="store_true")
    args = cli.parse_args()
    files = [
        UI.read_text(encoding="utf-8"),
        POLICY.read_text(encoding="utf-8"),
        TEST.read_text(encoding="utf-8"),
    ]
    verify(*files)
    if args.self_test:
        changes = (
            (0, "ArrayBlockingQueue<Runnable>(1)", "java.util.LinkedList<Runnable>()"),
            (0, "DownloadFileVerificationPolicy.canPublish(",
             "true // unsafe stale result"),
            (0, "DownloadFileVerificationPolicy.ready(", "true // unsafe ready"),
            (0, "fileVerifier.shutdownNow()", "/* leak worker */"),
            (1, "!checking && !verificationFailed", "true"),
            (1, "generation == newestGeneration", "true"),
            (2, "unknownFileStatusIsNotFalselyClassifiedMissingOrBroken",
             "noUnknownStateTest"),
        )
        for i, old, new in changes:
            mutated = list(files)
            assert old in mutated[i], old
            mutated[i] = mutated[i].replace(old, new, 1)
            try:
                verify(*mutated)
            except AssertionError:
                pass
            else:
                raise AssertionError("NW62 source guard missed " + old)
        print("NW62 negative fixtures: PASS")
    print("NW62 background Downloads file-health contracts: PASS")
    print("UNVERIFIED: Android compilation, WorkManager runtime and device UI.")


if __name__ == "__main__":
    main()
