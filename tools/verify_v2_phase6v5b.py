#!/usr/bin/env python3
"""6V5B source wiring and regression-discovery gate.

Not a Kotlin compiler or an Android/Google Drive runtime test.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
BASE = "app/src/main/java/com/nightwave/player/v2/locker/"
TEST = "app/src/test/java/com/nightwave/player/v2/locker/"
ANDROID = "app/src/androidTest/java/com/nightwave/player/v2/locker/"

REQUIRED = {
    BASE + "LockerTransferHistoryPolicy.kt": (
        "enum class LockerTransferKind", "enum class LockerTransferState",
        "MAX_ENTRIES = 30", "fun displayTitle(", "fun finish(",
        "Unconfirmed", "fun valid(",
    ),
    BASE + "LockerTransferHistoryStore.kt": (
        "AtomicFile(file)", "fun read(): LockerTransferHistorySnapshot",
        "private fun readLocked()", "fun start(", "fun finish(",
        "fun clear()", "LockerTransferHistoryPolicy.capped(",
        "require(!previous.unreadable)", "atomic.finishWrite(stream)",
    ),
    BASE + "LockerCheckpointReadiness.kt": (
        "enum class LockerCheckpointState", "NEEDS_REPAIR", "ACCOUNT_MISMATCH",
        "MISSING_DATA", "SHORT_DATA", "UNSAFE_STAGING",
        "HASH_CHECK_PENDING", "fun classify(", "val canResume:",
    ),
    BASE + "LockerDownloadCheckpointStore.kt": (
        "fun inspect(connectedAccount: String): LockerCheckpointPreview",
        "LockerCheckpointReadiness.classify(", "Files.isSymbolicLink(",
        "LockerCheckpointState.ACCOUNT_MISMATCH",
    ),
    BASE + "NightWaveLockerActivity.kt": (
        "history = LockerTransferHistoryStore(this)",
        "checkpoints.inspect(state.account())",
        "if (!readiness.canResume || saved == null)",
        "history.start(LockerTransferKind.UPLOAD",
        "history.start(LockerTransferKind.DOWNLOAD",
        "history.finish(journalId, resultState)",
        "renderTransferHistory()",
        "history.clear()",
        "Local transfer history cleared; music and checkpoints preserved.",
        "LockerTransferState.NOT_VERIFIED",
        "is CloudOfflineImportResult ->",
        "client.downloadWithRecovery(",
        "checkpoints.discard()",
    ),
    "tools/build-windows.ps1": (
        "check-phase6v5-integration.ps1", "check-phase6v5b-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ),
    "tools/check-builder-syntax.ps1": (
        "check-phase6v5b-integration.ps1",
    ),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v5b.py --self-test",
        "python3 tools/verify_v2_phase6v5b.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
}

TESTS = {
    TEST + "LockerTransferHistoryPolicyTest.kt": (
        8, ("missingJournalEntryNeverFabricatesSuccessfulTransfer",
            "historyIsCappedAndDuplicateIdsAreDeduplicated")),
    TEST + "LockerCheckpointReadinessTest.kt": (
        10, ("committedMissingFileCannotResume",
             "completedByteCountStillRequiresChecksumVerification")),
    ANDROID + "LockerTransferHistoryStoreInstrumentedTest.kt": (
        5, ("finishedTransferSurvivesStoreRecreation",
            "clearingHistoryDoesNotTouchIndependentDownloadFixture")),
}

def audit(root: Path) -> list[str]:
    errors = []
    for rel, terms in REQUIRED.items():
        file = root / rel
        if not file.is_file():
            errors.append("Missing " + rel)
            continue
        source = file.read_text(encoding="utf-8")
        for term in terms:
            if term not in source:
                errors.append(rel + ": missing " + repr(term))
    for rel, (count, names) in TESTS.items():
        file = root / rel
        if not file.is_file():
            errors.append("Missing test " + rel)
            continue
        body = file.read_text(encoding="utf-8")
        if body.count("@Test fun ") < count:
            errors.append(rel + f": fewer than {count} test methods")
        for name in names:
            if "fun " + name + "(" not in body:
                errors.append(rel + ": missing test " + name)
    return errors

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-6v5b-gate-") as temp:
        root = Path(temp)
        for rel, terms in REQUIRED.items():
            file = root / rel
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("\n".join(terms), encoding="utf-8")
        for rel, (count, names) in TESTS.items():
            file = root / rel
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("\n".join(
                [f"@Test fun test{i}() {{}}" for i in range(count)] +
                [f"fun {name}() {{}}" for name in names]), encoding="utf-8")
        assert not audit(root), audit(root)
        target = root / (BASE + "NightWaveLockerActivity.kt")
        target.write_text(target.read_text(encoding="utf-8").replace(
            "history.finish(journalId, resultState)", "unsafe_history_completion()"),
            encoding="utf-8")
        assert any("history.finish" in issue for issue in audit(root))
    print("PASS: 6V5B synthetic source checker rejects disconnected history outcomes")

def main() -> int:
    cmd = argparse.ArgumentParser()
    cmd.add_argument("--root", type=Path, default=ROOT)
    cmd.add_argument("--self-test", action="store_true")
    args = cmd.parse_args()
    if args.self_test:
        self_test()
        return 0
    errors = audit(args.root)
    for issue in errors:
        print("FAIL: " + issue)
    if errors:
        return 1
    print("PASS: 6V5B privacy journal, checkpoint preview and regressions present")
    print("NOT VERIFIED: Kotlin/JVM/Android runtime, Google OAuth and signer")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
