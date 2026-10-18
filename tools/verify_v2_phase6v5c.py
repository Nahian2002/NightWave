#!/usr/bin/env python3
"""NightWave 6V5C source integration: cursor browsing and SHA-targeted uploads.

Offline text checks only. Compilation, live Drive, signer and device tests are
independent and must not be claimed from this result.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "app/src/main/java/com/nightwave/player/v2/locker/"
REQ = {
    PREFIX + "LockerVaultBrowsePolicy.kt": [
        "MAX_TOTAL_PAGES = 40", "MAX_DUPLICATE_QUERY_PAGES = 20",
        "fun canLoadMore(", "cursor in inventory.seenTokens",
        "val nextPageToken: String?", "val seenTokens: Set<String>",
        "val verifiedBytes: Long", "val needsAttentionBytes: Long",
        "val formatCounts: List<Pair<String, Int>>",
    ],
    PREFIX + "LockerVaultPagingPolicy.kt": [
        "fun acceptNextToken(", "previouslySeen.add(next)",
        "fun uploadChecksumQuery(", "LockerAudioPolicy.validSha256(sha256)",
        "appProperties has { key='nightwave_sha256'",
        "fun hasUploadMatch(", "remote.md5.equals(md5, ignoreCase = true)",
    ],
    PREFIX + "NightWaveMusicDriveClient.kt": [
        "fun inventoryMore(previous: LockerCloudInventory)",
        "folder == previous.folderId",
        "repeat(minOf(LockerVaultBrowsePolicy.MAX_PAGES, remaining))",
        "LockerVaultPagingPolicy.acceptNextToken(next, seenTokens)",
        "nextPageToken = token.takeIf",
        "fun findMatchingUpload(",
        "LockerVaultPagingPolicy.uploadChecksumQuery(folder, sha256)",
        "LockerVaultPagingPolicy.hasUploadMatch(",
        "MAX_DUPLICATE_QUERY_PAGES",
        "findMatchingUpload(parent, song, sha256, md5)",
        "Cloud duplicate search incomplete; upload not started.",
        "require(current == selected &&",
        "require(fresh == selected",
        'metadata.optJSONArray("parents")',
        'optBoolean("canDownload") == true',
        "LockerAudioPolicy.hashes(partial)",
        "LockerDriveReceiptPolicy.requireCompletedUpload(",
    ],
    PREFIX + "NightWaveLockerActivity.kt": [
        "nextCloudPageButton", 'else if (action == "more") client.inventoryMore(inventory)',
        "LockerVaultBrowsePolicy.canLoadMore(inventory)",
        "if (action == \"more\")",
        "shownCount + LockerVaultBrowsePolicy.PAGE_INCREMENT",
        "MAX_TOTAL_PAGES", "summary.verifiedBytes",
        "summary.needsAttentionBytes", "summary.formatCounts",
        "This is not your total Google Drive usage.",
        'client.downloadWithRecovery(', "is CloudOfflineImportResult ->",
    ],
    "tools/build-windows.ps1": [
        "check-phase6v5c-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ],
    "tools/check-builder-syntax.ps1": ["check-phase6v5c-integration.ps1"],
    ".github/workflows/android.yml": [
        "python3 tools/verify_v2_phase6v5c.py --self-test",
        "python3 tools/verify_v2_phase6v5c.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ],
}
TESTS = {
    "app/src/test/java/com/nightwave/player/v2/locker/LockerVaultPagingPolicyTest.kt": (
        9, ["repeatedCursorFailsClosedInsteadOfLooping",
            "checksumQueryIsScopedToFolderAndPrivateShaKey",
            "mismatchedTargetedChecksumFailsClosed",
            "incompleteInventoryCanContinueOnlyWithSafeInMemoryCursor"]),
    "app/src/test/java/com/nightwave/player/v2/locker/LockerVaultBrowsePolicyTest.kt": (
        8, ["summaryReportsOnlyListedFilesNotFullDriveCapacity",
            "visiblePageIsBoundedEvenWithHugeRequest"]),
}

def audit(root: Path) -> list[str]:
    errors = []
    for rel, needles in REQ.items():
        file = root / rel
        if not file.is_file():
            errors.append("Missing: " + rel)
            continue
        src = file.read_text(encoding="utf-8")
        for needle in needles:
            if needle not in src:
                errors.append(rel + ": absent " + repr(needle))
    for rel, (minimum, methods) in TESTS.items():
        file = root / rel
        if not file.is_file():
            errors.append("Missing tests: " + rel)
            continue
        src = file.read_text(encoding="utf-8")
        if src.count("@Test fun ") < minimum:
            errors.append(rel + f": fewer than {minimum} tests")
        for name in methods:
            if "fun " + name + "(" not in src:
                errors.append(rel + ": missing " + name)
    client = root / (PREFIX + "NightWaveMusicDriveClient.kt")
    if client.is_file():
        src = client.read_text(encoding="utf-8")
        for forbidden in (
            "listInFolder(parent).firstOrNull",
            "require(!cloud.hasMore)",
            '.method("DELETE"', '.method("PATCH"',
            '.addQueryParameter("spaces", "appDataFolder")',
        ):
            if forbidden in src:
                errors.append("Unsafe old scan or remote mutation: " + forbidden)
    return errors

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-6v5c-") as tmp:
        root = Path(tmp)
        for rel, needles in REQ.items():
            f = root / rel
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("\n".join(needles), encoding="utf-8")
        for rel, (count, methods) in TESTS.items():
            f = root / rel
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("\n".join(
                [f"@Test fun test{i}() {{}}" for i in range(count)]
                + [f"fun {name}() {{}}" for name in methods]), encoding="utf-8")
        assert not audit(root), audit(root)
        target = root / (PREFIX + "NightWaveMusicDriveClient.kt")
        target.write_text(target.read_text(encoding="utf-8").replace(
            "findMatchingUpload(parent, song, sha256, md5)",
            "unsafe_global_list_upload()"), encoding="utf-8")
        assert any("findMatchingUpload(parent" in e for e in audit(root))
        target.write_text(target.read_text(encoding="utf-8") +
                          "\nlistInFolder(parent).firstOrNull", encoding="utf-8")
        assert any("Unsafe old scan" in e for e in audit(root))
    print("PASS: synthetic 6V5C gate rejects old scan and missing targeted duplicate guard")

def main() -> int:
    cli = argparse.ArgumentParser()
    cli.add_argument("--root", type=Path, default=ROOT)
    cli.add_argument("--self-test", action="store_true")
    args = cli.parse_args()
    if args.self_test:
        self_test()
        return 0
    problems = audit(args.root)
    for p in problems:
        print("FAIL: " + p)
    if problems:
        return 1
    print("PASS: 6V5C cursor vault, targeted dedup and regression declarations present")
    print("UNVERIFIED: Gradle, Android UI, Google Drive API and signed APK")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
