#!/usr/bin/env python3
"""NW51 source regression with mutation checks; no claim of Android builds."""
from __future__ import annotations
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt"
UI_POLICY = ROOT / "app/src/main/java/com/nightwave/player/v2/ui/LibraryUiRefreshPolicy.kt"
UI_TEST = ROOT / "app/src/test/java/com/nightwave/player/v2/ui/LibraryUiRefreshPolicyTest.kt"
BRIDGE = ROOT / "app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt"
SERVICE = ROOT / "app/src/main/java/com/nightwave/player/playback/PlaybackService.kt"
LISTEN_POLICY = ROOT / "app/src/main/java/com/nightwave/player/playback/ListeningDispatchPolicy.kt"
LISTEN_TEST = ROOT / "app/src/test/java/com/nightwave/player/playback/ListeningDispatchPolicyTest.kt"


def verify(files: dict[str, str]) -> None:
    rules = {
        "activity": (
            "private var offlineResumeReadJob: Job? = null",
            "private var recentRefreshJob: Job? = null",
            "offlineResumeReadJob?.cancel()",
            "recentRefreshJob?.cancel()",
            "refreshOfflineResume()",
            "private fun refreshRecentForTrackChange()",
            "withContext(Dispatchers.IO)",
            "lib.fileFor(it) != null",
            "LibraryUiRefreshPolicy.accepts(",
            "StudioTab.SEARCH -> {",
            "val indexedAudio by produceState(",
            "OfflineAudioLibrary(studioContext).songs().take(400)",
            "offlineSongs = indexedAudio",
            "selectedPlaylist = LIKED_COLLECTION",
            "onPrimarySearch",
            "onCloudBackup",
            "onUndoPlaylistEdit",
        ),
        "bridge": (
            "private val queueEntriesRefreshEarly = Runnable {",
            "private val queueEntriesRefreshLate = Runnable {",
            "private val queueFullRefreshEarly = Runnable {",
            "private val queueFullRefreshLate = Runnable {",
            "main.removeCallbacks(queueEntriesRefreshEarly)",
            "main.removeCallbacks(queueEntriesRefreshLate)",
            "main.removeCallbacks(queueFullRefreshEarly)",
            "main.removeCallbacks(queueFullRefreshLate)",
            "scheduleQueueEntriesRefresh()",
            "cancelQueueUiRefreshes()",
            "if (!released && ticking)",
            "PlaybackService.ACTION_ENQUEUE",
            "PlaybackService.ACTION_PLAY_NEXT",
            "refreshQueueEntriesOnly()",
            "MediaController.releaseFuture",
        ),
        "service": (
            "ArrayBlockingQueue<Runnable>(ListeningDispatchPolicy.MAX_PENDING_WRITES)",
            "ThreadPoolExecutor.AbortPolicy()",
            "catch (_: java.util.concurrent.RejectedExecutionException)",
            "ListeningDispatchPolicy.shouldWarn(",
            "listeningMemory.record(pulse, revision)",
            "listeningWriter.shutdown()",
            "resolver.shutdownNow()",
            "normalizationWorker.shutdownNow()",
            "PlaybackRecoveryPolicy.acceptsPostedRetry(",
            "CrossfadePolicy.canPrepare(",
            "OfflineGaplessPolicy.transitionIndex(",
            "MediaSession",
        ),
        "uiPolicy": (
            "submittedGeneration == currentGeneration",
            "!isFinishing && !isDestroyed",
        ),
        "listenPolicy": (
            "const val MAX_PENDING_WRITES = 32",
            "const val WARN_INTERVAL_MS = 60_000L",
            "fun shouldWarn(",
        ),
        "uiTest": (
            "onlyCurrentJobMayPublishUiSnapshot",
            "stoppedOrDestroyedScreensRejectLateIoResults",
        ),
        "listenTest": (
            "boundedQueueNeverAllowsAnUnlimitedTelemetryBacklog",
            "warningsAreRateLimitedWhenStorageIsStalled",
        ),
    }
    for name, markers in rules.items():
        missing = [marker for marker in markers if marker not in files[name]]
        if missing:
            raise AssertionError(name + ": missing " + repr(missing))

    activity = files["activity"]
    # No direct path verification on the main-thread full refresh.
    segment = activity.split("private fun refresh() {", 1)[1].split(
        "private fun refreshOfflineResume()", 1)[0]
    if "OfflineAudioLibrary(this).fileFor(" in segment:
        raise AssertionError("Main-thread offline resume file check returned")
    track_effect = activity.split("LaunchedEffect(playback.state.videoUrl) {", 1)[1].split(
        "}", 1)[0]
    if "refresh()" in track_effect:
        raise AssertionError("Full library reloaded on every track change")
    search = activity.split("StudioTab.SEARCH -> {", 1)[1].split(
        "StudioTab.LIBRARY ->", 1)[0]
    if "offlineSongs = remember(libraryRevision)" in search:
        raise AssertionError("Synchronous Compose offline index scan restored")
    if search.index("OfflineAudioLibrary(studioContext).songs().take(400)") < search.index(
            "withContext(Dispatchers.IO)"):
        raise AssertionError("Search index read is no longer delegated to IO")

    bridge = files["bridge"]
    if "main.postDelayed({ if (!released) refreshQueueEntriesOnly() }" in bridge:
        raise AssertionError("Per-tap queue refresh lambdas can pile up")
    if "main.postDelayed({ if (!released) refreshQueue() }" in bridge:
        raise AssertionError("Per-tap full queue refresh lambdas can pile up")

    service = files["service"]
    if "Executors.newSingleThreadExecutor()" in service:
        raise AssertionError("Unbounded listening insights executor was restored")
    if "listeningWriter.execute" not in service or "sampleListening(" not in service:
        raise AssertionError("Optional memory stats or sampling removed")
    for name in ("activity", "bridge", "service"):
        im = [line for line in files[name].splitlines() if line.startswith("import ")]
        if len(im) != len(set(im)):
            raise AssertionError("Duplicate Kotlin import in " + name)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    files = {
        "activity": ACTIVITY.read_text(encoding="utf-8"),
        "bridge": BRIDGE.read_text(encoding="utf-8"),
        "service": SERVICE.read_text(encoding="utf-8"),
        "uiPolicy": UI_POLICY.read_text(encoding="utf-8"),
        "listenPolicy": LISTEN_POLICY.read_text(encoding="utf-8"),
        "uiTest": UI_TEST.read_text(encoding="utf-8"),
        "listenTest": LISTEN_TEST.read_text(encoding="utf-8"),
    }
    verify(files)
    if args.self_test:
        def mutate(name: str, old: str, new: str) -> None:
            if old not in files[name]:
                raise AssertionError("Lost mutation fixture: " + old)
            changed = dict(files)
            changed[name] = changed[name].replace(old, new, 1)
            try:
                verify(changed)
            except AssertionError:
                return
            raise AssertionError("Regressed fixture was accepted")
        mutate("activity", "offlineSongs = indexedAudio",
               "offlineSongs = emptyList()")
        mutate("activity", "LibraryUiRefreshPolicy.accepts(",
               "true")
        mutate("bridge", "main.removeCallbacks(queueFullRefreshLate)",
               "main.removeCallbacks(ticker)")
        mutate("service", "ThreadPoolExecutor.AbortPolicy()",
               "ThreadPoolExecutor.CallerRunsPolicy()")
        mutate("service", "ListeningDispatchPolicy.shouldWarn(",
               "true")
        print("NW51 negative fixtures: PASS")
    print("NW51 source contracts: PASS; no Android runtime assertion")


if __name__ == "__main__":
    main()
