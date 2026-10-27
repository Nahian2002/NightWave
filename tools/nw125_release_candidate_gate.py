#!/usr/bin/env python3
"""NW125 deterministic repository/packaging preflight, NOT an Android build.

This script is read-only, works with both a full git checkout and ZIP source,
and fails on unsafe tracked/source packaging. It cannot certify device,
signing continuity, music-rights, Google OAuth or Play production readiness.
"""
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_PACKAGE = "com.nightwave.player"
EXPECTED_CASES = 139

ESSENTIAL = (
    "build-windows.cmd",
    "check-device-update.cmd",
    "tools/build-windows.ps1",
    "tools/check-builder-syntax.ps1",
    "tools/verify-apk-signature.ps1",
    "tools/verify_current.py",
    ".github/workflows/current-android.yml",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWavePremiumChrome.kt",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveAccessibilityPolicy.kt",
    "app/src/main/java/com/nightwave/player/offline/OfflineDiscoveryReviewPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/social/PlaylistDocumentPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveSleepTimerDialog.kt",
    "app/src/test/java/com/nightwave/player/offline/OfflineDiscoveryReviewPolicyTest.kt",
    "app/src/test/java/com/nightwave/player/v2/social/PlaylistDocumentPolicyTest.kt",
    "app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveSleepTimerAccessibilityInstrumentedTest.kt",
    "docs/NW90_EXPANDED_139_CASE_MATRIX.csv",
    "docs/NW111_NW125_SPOTIFY_QUALITY_ROADMAP.md",
)

BANNED_EXT = {".apk", ".aab", ".apks", ".idsig", ".jks", ".keystore",
              ".p12", ".pfx", ".pem", ".key", ".log", ".zip", ".7z",
              ".rar", ".hprof", ".stackdump", ".heapdump"}
BANNED_NAMES = {"google-services.json", "credentials.json",
                "keystore.properties", "release-signing.properties",
                "local.properties", ".env"}
BANNED_DIRS = {".gradle", ".idea", "__pycache__", "node_modules",
               ".cxx", ".externalnativebuild"}
ALLOWED_FIXTURES = ("app/src/main/res/", "app/src/test/", "app/src/androidTest/")


def source_names(root: Path) -> list[str]:
    if (root.joinpath(".git").exists():
        process = subprocess.run(["git", "-C", str(root), "ls-files", "-z"],
                                 stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                                 timeout=20, check=False)
        if process.returncode == 0:
            return sorted({x.decode("utf-8", "replace").replace("\\", "/")
                           for x in process.stdout.split(b"\0") if x})
    ignored = {"build", "dist", ".local-build", *BANNED_DIRS, ".git"}
    return sorted(p.relative_to(root).as_posix()
                  for p in root.rglob("*")
                  if p.is_file() and not any(part.lower() in ignored
                                              for part in p.relative_to(root).parts))


def violations(paths: list[str]) -> list[str]:
    issues = []
    for item in paths:
        p = Path(item)
        parts = [part.lower() for part in p.parts]
        low = p.name.lower()
        if any(x in BANNED_DIRS or x in {"build", "dist"} for x in parts[:-1]):
            issues.append("generated directory tracked: " + item)
        if p.suffix.lower() in BANNED_EXT or low in BANNED_NAMES or low.startswith(".env."):
            issues.append("binary/credential/generated file tracked: " + item)
        if any(part in {".", ".."} for part in p.parts):
            issues.append("unsafe path traversal: " + item)
    return sorted(set(issues))


def inspect(root: Path, names: list[str] | None = None) -> dict:
    names = source_names(root) if names is None else names
    findings = violations(names)
    for relative in ESSENTIAL:
        if not (root / relative).is_file():
            findings.append("missing required source: " + relative)
    gradle = (root / "app/build.gradle.kts")
    if gradle.is_file():
        text = gradle.read_text(encoding="utf-8-sig")
        for pattern, label in (
            (r'applicationId\s*=\s*"com\.nightwave\.player"', "package ID"),
            (r'compileSdk\s*=\s*36\b', "compile SDK 36"),
            (r'targetSdk\s*=\s*36\b', "target SDK 36"),
            (r'minSdk\s*=\s*26\b', "minimum SDK 26"),
            (r'versionCode\s*=\s*(?:35|36)\b', "expected development version code"),
        ):
            if re.search(pattern, text) is None:
                findings.append("Android build identity mismatch: " + label)
    workflow = root / ".github/workflows/current-android.yml"
    if workflow.is_file():
        text = workflow.read_text(encoding="utf-8")
        for task in (":app:testDebugUnitTest", ":app:assembleDebug",
                     ":app:compileDebugAndroidTestKotlin", ":app:lintDebug",
                     "verify-apk-signature.ps1"):
            if task not in text:
                findings.append("current workflow missing task: " + task)
    matrix = root / "docs/NW90_EXPANDED_139_CASE_MATRIX.csv"
    number = 0
    p0 = 0
    if matrix.is_file():
        try:
            with matrix.open(encoding="utf-8-sig", newline="") as stream:
                rows = list(csv.DictReader(stream))
            number = len(rows)
            ids = [x.get("case_id", "") for x in rows]
            p0 = sum(x.get("severity") == "P0" for x in rows)
            if number != EXPECTED_CASES or len(set(ids)) != number:
                findings.append("device matrix cases or IDs changed without review")
            if any(not x.get("area") or not x.get("expected") or
                   not x.get("status") for x in rows):
                findings.append("device matrix missing evidence fields")
        except (OSError, UnicodeError, csv.Error):
            findings.append("device matrix unreadable")
    return {
        "gate": "NW125_SOURCE_PACKAGE_ONLY",
        "status": "PASS_SOURCE_ONLY" if not findings else "FAIL_SOURCE",
        "release_decision": "NO_GO_PENDING_BUILD_DEVICE_RIGHTS_AND_OAUTH",
        "tracked_or_source_files": len(names),
        "device_matrix_cases": number,
        "device_matrix_p0_cases": p0,
        "findings": findings,
        "does_not_certify": ["Android compilation", "JVM tests", "Android instrumentation",
                             "APK signer continuity", "Google Drive", "phone behavior",
                             "music rights", "public distribution"],
    }


def self_test() -> None:
    assert violations(["app/src/main/assets/production.jks"]) == [
        "binary/credential/generated file tracked: app/src/main/assets/production.jks"]
    assert set(violations(["build/stale.apk", "app/build/output.bin"])) == {
        "generated directory tracked: app/build/output.bin",
        "generated directory tracked: build/stale.apk",
        "binary/credential/generated file tracked: build/stale.apk"}
    assert violations(["app/src/main/res/drawable/logo.png",
                       "app/schemas/test/1.json",
                       "docs/NW122_EVIDENCE.md"]) == []
    with tempfile.TemporaryDirectory() as d:
        root = Path(d)
        first = inspect(root, names=[])
        assert first["status"] == "FAIL_SOURCE"
        assert first["release_decision"].startswith("NO_GO")
        assert any("missing required source" in issue for issue in first["findings"])


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--self-test", action="store_true")
    p.add_argument("--json", action="store_true")
    p.add_argument("--root", type=Path, default=ROOT)
    opts = p.parse_args()
    if opts.self_test:
        self_test()
        print("NW125 preflight self-test PASS (synthetic fixtures only)")
        return 0
    report = inspect(opts.root)
    if opts.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print("NW125_SOURCE_PACKAGE:", report["status"])
        print("Tracked/source files:", report["tracked_or_source_files"])
        print("Device matrix cases:", report["device_matrix_cases"])
        print("Public release:", report["release_decision"])
        for issue in report["findings"]:
            print("FAIL:", issue)
    return 0 if report["status"] == "PASS_SOURCE_ONLY" else 1


if __name__ == "__main__":
    raise SystemExit(main())
