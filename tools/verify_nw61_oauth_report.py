#!/usr/bin/env python3
"""NW61: source gate for safe, reproducible Google OAuth APK diagnostics.

Does not inspect actual certificates, connect to Google, build an APK,
or read accounts/tokens. These require the Windows script and device.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools/check-google-oauth.ps1"
LAUNCHER = ROOT / "check-google-oauth.cmd"
BUILD = ROOT / "tools/build-windows.ps1"
SYNTAX = ROOT / "tools/check-builder-syntax.ps1"


def verify(script: str, launcher: str, builder: str, syntax: str) -> None:
    needed = {
        "powershell": (
            "[string]$ApkPath = ''",
            "[switch]$SelfTest",
            "function Test-VerifiedApkStatus",
            "^SUCCESS - NightWave Android debug APK locally verified",
            "SHA256:",
            "Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256",
            "if (-not (Test-VerifiedApkStatus $status $apkSha256))",
            "function Find-NightWaveBuildTools",
            "Get-ChildItem -LiteralPath $parent -Directory",
            "Sort-Object -Property",
            "Test-Path -LiteralPath $signer -PathType Leaf",
            "Test-Path -LiteralPath $aapt -PathType Leaf",
            "verify --min-sdk-version 26 --print-certs",
            "Signer #1 certificate SHA-1 digest",
            "Signer #1 certificate SHA-256 digest",
            "if ($package -ne 'com.nightwave.player')",
            "INCONCLUSIVE - no current verified APK",
            "check-device-update.cmd",
            "Google Drive API",
            "never",  # see placeholder replacement below
        ),
        "launcher": (
            'check-google-oauth.ps1" -SelfTest',
            'check-google-oauth.ps1"',
            "if errorlevel 1 goto :failed",
        ),
        "builder": (
            "'SUCCESS - NightWave Android debug APK locally verified'",
            '"SHA256: $hash"',
        ),
        "syntax": (
            "'check-google-oauth.ps1'",
        ),
    }
    sources = {
        "powershell": script, "launcher": launcher,
        "builder": builder, "syntax": syntax,
    }
    for name, words in needed.items():
        for token in words:
            if token == "never":
                continue
            if token not in sources[name]:
                raise AssertionError(name + " missing " + repr(token))

    if "build-tools\\35.0.0" in script:
        raise AssertionError("Hard-coded Build Tools 35.0.0 returned")
    if "'app\\build\\outputs\\apk\\debug\\app-debug.apk'" in script:
        raise AssertionError("Unverified Gradle output was accepted")
    if "if (Test-VerifiedApkStatus $good ('B' * 64))" not in script:
        raise AssertionError("Stale APK negative fixture absent")
    if "Test-VerifiedApkStatus ($good.Replace('SUCCESS -', 'FAILED -'))" not in script:
        raise AssertionError("Failed-build negative fixture absent")
    if "Set-Content -LiteralPath $report -Encoding UTF8" not in script:
        raise AssertionError("Old OAuth report must be overwritten on failure")
    if "INCONCLUSIVE" not in launcher:
        raise AssertionError("Launcher needs explicit inconclusive state")
    if any(ord(ch) > 127 for ch in script):
        raise AssertionError("Windows PowerShell 5.1 requires ASCII scripts")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    inputs = [p.read_text(encoding="utf-8")
              for p in (SCRIPT, LAUNCHER, BUILD, SYNTAX)]
    verify(*inputs)
    if args.self_test:
        mutations = (
            (0, "Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256",
             "Get-Date"),
            (0, "if (-not (Test-VerifiedApkStatus $status $apkSha256))",
             "if ($false)"),
            (0, "Get-ChildItem -LiteralPath $parent -Directory",
             "'build-tools\\35.0.0'"),
            (0, "if (Test-VerifiedApkStatus $good ('B' * 64))",
             "if ($false)"),
            (1, 'check-google-oauth.ps1" -SelfTest',
             'check-google-oauth.ps1"'),
            (2, "'SUCCESS - NightWave Android debug APK locally verified'",
             "'UNVERIFIED debug APK'"),
        )
        for index, old, new in mutations:
            broken = inputs[:]
            assert old in broken[index], old
            broken[index] = broken[index].replace(old, new, 1)
            try:
                verify(*broken)
            except AssertionError:
                pass
            else:
                raise AssertionError("Unsafe OAuth report fixture accepted: " + old)
        print("NW61 OAuth diagnostic negative fixtures: PASS")
    print("NW61 verified APK identity and source contracts: PASS")
    print("NOT VERIFIED: Google Cloud OAuth client, phone sign-in, actual APK signer.")


if __name__ == "__main__":
    main()
