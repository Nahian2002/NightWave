#!/usr/bin/env python3
"""Phase 6V4D cross-phase source contract and regression-test discovery.

Not a Kotlin compiler, JVM test runner, emulator or APK signer. The actual
Gradle gates remain mandatory; no APK is published on this script alone.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]

# Check both the core implementation AND that it is called by runtime code.
REQUIRED: dict[str, tuple[str, ...]] = {
    "app/src/main/java/com/nightwave/player/playback/QueueRestorePolicy.kt": (
        "fun selectedIndex(", "isCanonicalPublicWatchUrl", "originalUrls[savedIndex]",
    ),
    "app/src/main/java/com/nightwave/player/playback/QueueStateStore.kt": (
        'put("selected_url",', "QueueRestorePolicy.selectedIndex(originalUrls,",
        "QueueRestorePolicy.isCanonicalPublicWatchUrl", "state.optString(\"selected_url\")",
        "prefs.edit().putString(\"snapshot\", state.toString()).commit()",
    ),
    "app/src/main/java/com/nightwave/player/offline/OfflineLibraryHealth.kt": (
        "enum class OfflineFileHealth", "SIZE_CHANGED", "UNSAFE_METADATA",
        "fun classify(", "val attentionCount:", "val playableCount:",
    ),
    "app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt": (
        "fun healthReport(): OfflineLibraryHealthReport",
        "inspectFile(song).first != OfflineFileHealth.AVAILABLE",
        "Files.isSymbolicLink(", "file.length()",
        "OfflineLibraryHealthPolicy.classify(",
        "return OfflineLibraryHealthReport(false, emptyList())",
    ),
    "app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt": (
        "val report = library.healthReport()", "report.attentionCount",
        "No files or entries changed", "Entry retained",
        "it.id in playableIds", "library.fileFor(song)",
    ),
    "app/src/main/java/com/nightwave/player/playback/PlaybackRecoveryPolicy.kt": (
        "fun mayAutoRetry(", "fun acceptsPostedRetry(",
        "fun acceptsResolvedTrack(", "wantsPlayback", "timerPaused",
        "scheduledGeneration == currentGeneration",
    ),
    "app/src/main/java/com/nightwave/player/playback/PlaybackService.kt": (
        "PlaybackRecoveryPolicy.mayAutoRetry(",
        "PlaybackRecoveryPolicy.acceptsPostedRetry(",
        "PlaybackRecoveryPolicy.acceptsResolvedTrack(entry.url, video.url)",
        "ex oPlayer?.playWhenReady = false".replace("ex o", "exo"),
        "savedQueue.restore(queue)",
        "PlaybackTransitionPolicy.shouldAdvance(",
    ),
    "app/src/main/java/com/nightwave/player/v2/playback/MediaReconnectPolicy.kt": (
        "fun mayAttempt(", "!released && foregroundActive && oneShotAvailable",
    ),
    "app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt": (
        "MediaReconnectPolicy.mayAttempt(",
        "controller !== player", "disconnected.removeListener(listener)",
        "scheduleForegroundReconnect()", "foregroundReconnectAvailable = false",
    ),
    # Keep fixes from earlier failed Windows builds in the integration baseline.
    "app/src/main/java/com/nightwave/player/v2/locker/NightWaveMusicDriveClient.kt": (
        "request(builder.build().toString()).get().build()",
    ),
    "app/src/test/java/com/nightwave/player/v2/cloud/CloudRecoveryPolicyTest.kt": (
        "fun reportCountsEveryMissingPlaylistWithoutModifyingInputs()",
        "assertEquals(2, after.localPlaylistsNotInCloud)",
    ),
    # The real compiler, test runner and Android test-source gate remain mandatory.
    "tools/build-windows.ps1": (
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "Select-String -LiteralPath $buildLog -Pattern '^BUILD SUCCESSFUL' -Quiet",
        "verify-apk-signature.ps1", "check-phase6v4-integration.ps1",
    ),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v4d.py --self-test",
        "python3 tools/verify_v2_phase6v4d.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "actions/upload-artifact@v4",
    ),
}

TEST_FILES: dict[str, tuple[int, tuple[str, ...]]] = {
    "app/src/test/java/com/nightwave/player/playback/QueueRestorePolicyTest.kt": (
        6, ("legacySelectionSurvivesInvalidPrecedingRow",
            "mismatchedSavedSelectedUrlFailsClosed")),
    "app/src/test/java/com/nightwave/player/offline/OfflineLibraryHealthPolicyTest.kt": (
        6, ("marksMissingFilesWithoutAlteringIndex",
            "marksSizeChanges")),
    "app/src/test/java/com/nightwave/player/playback/PlaybackRecoveryPolicyTest.kt": (
        9, ("userPauseAfterErrorButBeforePostedCallbackPreventsRestart",
            "sleepTimerExpiryBlocksBothSchedulingAndDispatch",
            "onlyExactCanonicalResolvedVideoMayReplaceSelection")),
    "app/src/test/java/com/nightwave/player/v2/playback/MediaReconnectPolicyTest.kt": (
        4, ("leavingScreenOrDestroyingActivityCancelsReconnect",
            "activeControllerOrPendingRequestBlocksDuplicates")),
    "app/src/androidTest/java/com/nightwave/player/playback/QueueStateBookmarkInstrumentedTest.kt": (
        15, ("legacySnapshotKeepsSelectedSongAfterInvalidRowIsRemoved",
             "signedWatchLinkCannotLeakIntoBookmarkOrLookupMetadata")),
    "app/src/androidTest/java/com/nightwave/player/offline/OfflineLibraryHealthInstrumentedTest.kt": (
        3, ("missingAndChangedFilesNeverLoseTheirIndexRows",
            "unreadableIndexHasAReportButKeepsOriginalBytes")),
}


def audit(root: Path) -> list[str]:
    errors: list[str] = []
    for rel, tokens in REQUIRED.items():
        path = root / rel
        if not path.is_file():
            errors.append(f"Missing required source: {rel}")
            continue
        body = path.read_text(encoding="utf-8")
        for token in tokens:
            if token not in body:
                errors.append(f"{rel}: absent source contract {token!r}")
    for rel, (minimum, names) in TEST_FILES.items():
        path = root / rel
        if not path.is_file():
            errors.append(f"Missing regression test file: {rel}")
            continue
        body = path.read_text(encoding="utf-8")
        count = body.count("@Test fun ")
        if count < minimum:
            errors.append(f"{rel}: only {count} tests, expected at least {minimum}")
        for name in names:
            if f"fun {name}(" not in body:
                errors.append(f"{rel}: missing regression test {name}")
    return errors


def self_test() -> None:
    # Construct a minimal synthetic project so the gate itself is tested.
    # A removed runtime hook or missing test must turn a PASS into a FAIL.
    with tempfile.TemporaryDirectory(prefix="nightwave-v4d-gate-") as tmp:
        root = Path(tmp)
        for rel, tokens in REQUIRED.items():
            path = root / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("\n".join(tokens), encoding="utf-8")
        for rel, (minimum, names) in TEST_FILES.items():
            path = root / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            body = "\n".join(f"@Test fun case{i}() {{}}" for i in range(minimum))
            body += "\n" + "\n".join(f"fun {n}() {{}}" for n in names)
            path.write_text(body, encoding="utf-8")
        assert not audit(root), audit(root)
        target = root / "app/src/main/java/com/nightwave/player/playback/PlaybackService.kt"
        target.write_text(target.read_text(encoding="utf-8").replace(
            "PlaybackRecoveryPolicy.acceptsPostedRetry(", "BROKEN("), encoding="utf-8")
        assert any("acceptsPostedRetry" in issue for issue in audit(root))
        target = root / "app/src/test/java/com/nightwave/player/playback/PlaybackRecoveryPolicyTest.kt"
        target.write_text("", encoding="utf-8")
        assert any("only 0 tests" in issue for issue in audit(root))
    print("PASS: synthetic good project accepted; missing runtime hook and tests rejected")


def main() -> int:
    cli = argparse.ArgumentParser()
    cli.add_argument("--root", type=Path, default=ROOT)
    cli.add_argument("--self-test", action="store_true")
    args = cli.parse_args()
    if args.self_test:
        self_test()
        return 0
    errors = audit(args.root)
    for error in errors:
        print("FAIL: " + error)
    if errors:
        print(f"FAIL: {len(errors)} Phase 6V4D integration source gaps")
        return 1
    print(f"PASS: Phase 6V4A/B/C hooks and {len(TEST_FILES)} regression suites present")
    print("UNVERIFIED: actual Kotlin/Android compile, JVM tests, phone behavior and APK signer")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
