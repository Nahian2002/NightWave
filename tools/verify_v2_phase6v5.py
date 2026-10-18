#!/usr/bin/env python3
"""6V5 Drive Music Vault UI/metadata integration contracts (not Android build).

No network, Google OAuth, user data, file mutation, APK generation or
instrumentation. The real Gradle and device-signing gates remain mandatory.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]

REQUIRED = {
    "app/src/main/java/com/nightwave/player/v2/locker/LockerVaultBrowsePolicy.kt": (
        "data class LockerCloudInventory(", "val hasMore: Boolean",
        "enum class LockerVaultFilter", "enum class LockerVaultSort",
        "fun downloadable(", "LockerAudioPolicy.requireDownloadable(track)",
        "fun browse(", "fun summary(", "fun visible(", "fun bytesLabel(",
        "const val MAX_PAGES = 5", "const val PAGE_SIZE = 100",
        "partialListing = inventory.hasMore",
    ),
    "app/src/main/java/com/nightwave/player/v2/locker/NightWaveMusicDriveClient.kt": (
        "fun inventory(): LockerCloudInventory",
        "fun list(): List<LockerRemoteTrack> = inventory().tracks",
        "repeat(LockerVaultBrowsePolicy.MAX_PAGES)",
        'addQueryParameter("pageSize", LockerVaultBrowsePolicy.PAGE_SIZE.toString())',
        "if (next.isBlank()) return LockerCloudInventory(",
        "hasMore = true", "fun inventoryMore(previous: LockerCloudInventory)",
        "findMatchingUpload(parent, song, sha256, md5)",
        "LockerVaultPagingPolicy.uploadChecksumQuery(folder, sha256)",
        "LockerVaultPagingPolicy.hasUploadMatch(", "upload not started.",
        "fun downloadWithRecovery(", "LockerAudioPolicy.hashes(partial)",
        "LockerDriveReceiptPolicy.requireCompletedUpload(",
    ),
    "app/src/main/java/com/nightwave/player/v2/locker/NightWaveLockerActivity.kt": (
        "client.inventory()", "is LockerCloudInventory ->",
        "val report = LockerVaultBrowsePolicy.summary(inventory)",
        "LockerVaultBrowsePolicy.browse(",
        "LockerVaultBrowsePolicy.visible(",
        "LockerVaultBrowsePolicy.downloadable(song)",
        "LockerVaultFilter.NEEDS_ATTENTION",
        "LockerVaultSort.LARGEST",
        "shownCount + LockerVaultBrowsePolicy.PAGE_INCREMENT",
        "This is not your total Google Drive usage.",
        "More cloud files exist beyond this scan.",
        'begin("download", song)',
        'client.upload(song, file)',
        'client.downloadWithRecovery(',
        "is CloudOfflineImportResult ->",
        "verified before being added",
        "checkpoints.discard()",
    ),
    "tools/build-windows.ps1": (
        "check-phase6v4-integration.ps1",
        "check-phase6v5-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ),
    "tools/check-builder-syntax.ps1": (
        "check-phase6v5-integration.ps1",
    ),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v5.py --self-test",
        "python3 tools/verify_v2_phase6v5.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
    "tools/verify_v2_phase6r1.py": (
        "assert 'client.inventory()' in ui",
        "assert 'repeat(LockerVaultBrowsePolicy.MAX_PAGES)' in client",
    ),
    "tools/verify_v2_phase6r2.py": (
        'assert "client.inventory()" in ui',
    ),
}

TESTS = {
    "app/src/test/java/com/nightwave/player/v2/locker/LockerVaultBrowsePolicyTest.kt": (
        8,
        ("caseInsensitiveSearchNeverMutatesCloudInventory",
         "verifiedFilterRequiresBothHashes",
         "summaryReportsOnlyListedFilesNotFullDriveCapacity",
         "visiblePageIsBoundedEvenWithHugeRequest"),
    ),
}


def audit(root: Path) -> list[str]:
    errors: list[str] = []
    for relative, terms in REQUIRED.items():
        path = root / relative
        if not path.is_file():
            errors.append("Missing: " + relative)
            continue
        body = path.read_text(encoding="utf-8")
        for term in terms:
            if term not in body:
                errors.append(relative + ": missing contract " + repr(term))
    for relative, (minimum, names) in TESTS.items():
        path = root / relative
        if not path.is_file():
            errors.append("Missing test: " + relative)
            continue
        body = path.read_text(encoding="utf-8")
        count = body.count("@Test fun ")
        if count < minimum:
            errors.append(relative + f": {count} tests, require {minimum}")
        for name in names:
            if f"fun {name}(" not in body:
                errors.append(relative + ": missing " + name)
    client_path = root / "app/src/main/java/com/nightwave/player/v2/locker/NightWaveMusicDriveClient.kt"
    if client_path.is_file():
        client = client_path.read_text(encoding="utf-8")
        for forbidden in ('.method("DELETE"', '.method("PATCH"',
                          '.addQueryParameter("spaces", "appDataFolder")'):
            if forbidden in client:
                errors.append("Unsafe Drive mutation or backup scope: " + forbidden)
    return errors


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-6v5-audit-") as tmp:
        root = Path(tmp)
        for relative, terms in REQUIRED.items():
            p = root / relative
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("\n".join(terms), encoding="utf-8")
        for relative, (minimum, names) in TESTS.items():
            p = root / relative
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("\n".join(
                [f"@Test fun case{i}() {{}}" for i in range(minimum)] +
                [f"fun {n}() {{}}" for n in names]), encoding="utf-8")
        assert not audit(root), audit(root)
        target = root / "app/src/main/java/com/nightwave/player/v2/locker/NightWaveMusicDriveClient.kt"
        target.write_text(target.read_text(encoding="utf-8").replace(
            "findMatchingUpload(parent, song, sha256, md5)",
            "unsafe_allow_all_uploads()"), encoding="utf-8")
        assert any("findMatchingUpload(parent, song, sha256, md5)" in e
                   for e in audit(root))
        target.write_text(target.read_text(encoding="utf-8") +
                          "\n.method(\"DELETE\"", encoding="utf-8")
        assert any("Unsafe Drive mutation" in e for e in audit(root))
    print("PASS: 6V5 synthetic contracts accept safe hooks and reject removed guards")


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
        print(f"FAIL: {len(errors)} Phase 6V5 integration errors")
        return 1
    print("PASS: NightWave 6V5 cloud browser, partial-inventory safety and tests present")
    print("NOT VERIFIED: Gradle compilation, unit tests, Drive access, APK or phone behavior")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
