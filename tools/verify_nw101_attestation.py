#!/usr/bin/env python3
"""NW101 offline source-snapshot regression and wiring verification.

This self-test is portable and does NOT replace PowerShell 5.1 execution,
Android compilation, fresh JUnit evidence, APK signer checks or phone QA.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest().upper()


def validate_manifest(root: Path, recorded: list[str], tracked: list[str]) -> bool:
    """Recreate each PowerShell fingerprint line from today's actual files."""
    if not tracked or len(tracked) != len(recorded) or len(set(tracked)) != len(tracked):
        return False
    root = root.resolve()
    for record, name in zip(recorded, tracked):
        p = Path(name)
        if p.is_absolute() or "\\" in name or ".." in p.parts or not name:
            return False
        full = root / p
        if not full.is_file() or full.is_symlink() or not full.resolve().is_relative_to(root):
            return False
        actual = f"{sha256(full.read_bytes())}  {name}"
        if record != actual:
            return False
    return True


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nw101-attestation-") as d:
        root = Path(d)
        (root / "app/src/main").mkdir(parents=True)
        (root / "tools").mkdir()
        one = root / "app/src/main/A.kt"
        two = root / "tools/check.ps1"
        one.write_bytes(b"package example\n")
        two.write_bytes(b"# build script\n")
        tracked = ["app/src/main/A.kt", "tools/check.ps1"]
        records = [f"{sha256((root / x).read_bytes())}  {x}" for x in tracked]
        old_manifest_sha = sha256(("\r\n".join(records)).encode("utf-8"))
        assert validate_manifest(root, records, tracked), "fresh source accepted"
        one.write_bytes(b"package mutated\n")
        # Before NW101, the unchanged manifest's SHA still matches the status,
        # although actual source was modified after a successful build.
        assert old_manifest_sha == sha256(("\r\n".join(records)).encode("utf-8"))
        assert not validate_manifest(root, records, tracked), "mutated source rejected"
        one.write_bytes(b"package example\n")
        assert not validate_manifest(root, records, tracked + ["tools/new.kt"])
        assert not validate_manifest(root, records, tracked[:1])
        assert not validate_manifest(root, records, tracked[::-1])
        assert not validate_manifest(root, [records[0], records[0]], tracked)
        two.unlink()
        assert not validate_manifest(root, records, tracked), "removed source rejected"
        (root / "tools/check.ps1").write_bytes(b"# build script\n")
        assert validate_manifest(root, records, tracked)
    print("NW101_SOURCE_SNAPSHOT_REGRESSION_PASS cases=8")


def verify_wiring() -> None:
    att = (ROOT / "tools/nw101-source-attestation.ps1").read_text()
    gate = (ROOT / "tools/nw90-windows-gate.ps1").read_text()
    syntax = (ROOT / "tools/check-builder-syntax.ps1").read_text()
    ci = (ROOT / ".github/workflows/android.yml").read_text()
    canary = (ROOT / ".github/workflows/nightwave-runner-canary.yml").read_text()
    assert "Assert-FingerprintLines -Recorded $recorded -Actual $actual" in att
    assert "Get-FileHash -LiteralPath $full -Algorithm SHA256" in att
    assert "get-nw08-source-inputs.ps1" in att
    assert "NW101_SOURCE_SNAPSHOT_MATCH files=" in att
    assert "NW101_SOURCE_ATTESTATION_SELFTEST_PASS cases=6" in att
    assert "-File $nw101Verifier -SelfTest" in gate
    assert "-File $nw101Verifier -ProjectRoot $root -ManifestPath $manifest" in gate
    assert "if ($LASTEXITCODE -ne 0)" in gate
    assert "NW101 current source file contents/inventory: EXACT BUILD SNAPSHOT" in gate
    assert "nw101-source-attestation.ps1" in syntax
    assert "nw90-windows-gate.ps1" in syntax
    assert "verify_nw101_attestation.py --self-test" in ci
    assert "verify_nw101_attestation.py" in ci
    assert "nw101-source-attestation.ps1 -SelfTest" in ci
    assert "feature/nightwave-nw101-build-attestation-recovery" in ci
    assert "feature/nightwave-nw101-build-attestation-recovery" in canary
    print("NW101_BUILD_EVIDENCE_WIRING_PASS checks=16")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        verify_wiring()


if __name__ == "__main__":
    main()
