#!/usr/bin/env python3
"""Current structural gates; Gradle/JUnit/device validation are separate steps.

Historical per-wave source-text audits remain available under tools/ and in
the manual historical workflow. Their exact old strings/hashes are not a
substitute for compiling and executing the current app's regression tests.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
PYTHON_CHECKS = (
    ("audit_repo_nw22.py", "--self-test"),
    ("audit_repo_nw22.py",),
    ("verify_source.py",),
    ("check-kotlin-internal-imports.py",),
    ("check_kotlin_bytearray_fixtures.py", "--self-test"),
    ("check_kotlin_bytearray_fixtures.py",),
    ("triage_github_actions.py", "--self-test"),
    ("verify_nw109_android36_privacy.py", "--self-test"),
    ("verify_nw109_android36_privacy.py",),
    ("verify_nw101_attestation.py", "--self-test"),
    ("nw103_receipt_selftest.py",),
    ("nw90_certification.py", "--self-test"),
    ("nw110_final_certification.py", "--self-test"),
    ("public_release_preflight.py", "--self-test"),
    ("compare_nw122_performance.py", "--self-test"),
    ("nw125_release_candidate_gate.py", "--self-test"),
    ("nw125_release_candidate_gate.py", "--json"),
    ("nw126_acceptance.py", "--self-test"),
    ("verify_nw128_shell.py", "--self-test"),
    ("verify_nw129_search.py", "--self-test"),
    ("verify_nw130_public_search.py", "--self-test"),
    ("verify_nw131_home.py", "--self-test"),
    ("verify_nw132_library.py", "--self-test"),
    ("verify_nw133_player.py", "--self-test"),
    ("verify_matte_design.py", "--self-test"),
    ("verify_theme_expression.py", "--self-test"),
    ("verify_content_finish.py", "--self-test"),
    ("verify_pager_artwork_recovery.py", "--self-test"),
    ("verify_editorial_finish.py", "--self-test"),
    ("verify_composition_finish.py", "--self-test"),
)

# Same safety preflights as the one-click Windows builder, including negative
# fixtures where that builder requires them. Never invoke SDK license prompts
# or install an APK from this read-only check runner.
WINDOWS_CHECKS = (
    ("check-builder-syntax.ps1",),
    ("check-kotlin-compat.ps1",),
    ("check-nw27-integrity.ps1", "-SelfTest"),
    ("check-nw27-integrity.ps1",),
    ("capture-nightwave-crash.ps1", "-SelfTest"),
    ("diagnose-build.ps1", "-SelfTest"),
    ("check-phase6p-integration.ps1",),
    ("check-phase6v-integration.ps1",),
    ("check-phase6v4-integration.ps1",),
    ("check-phase6v5-integration.ps1",),
    ("check-phase6v5b-integration.ps1",),
    ("check-phase6v5c-integration.ps1",),
    ("check-phase6v6a-integration.ps1",),
    ("check-phase6v6b-integration.ps1",),
    ("check-phase6v6c-integration.ps1",),
    ("check-phase6v6d-integration.ps1",),
    ("check-phase6v6e-integration.ps1",),
    ("check-phase6v6f-integration.ps1",),
    ("check-kotlin-bytearray-fixtures.ps1", "-SelfTest"),
    ("check-kotlin-bytearray-fixtures.ps1",),
    ("check-phase6v7a-integration.ps1", "-SelfTest"),
    ("check-phase6v7a-integration.ps1",),
    ("check-phase6v7b-integration.ps1",),
    ("check-nw07-source-gate.ps1", "-SelfTest"),
    ("check-nw07-source-gate.ps1",),
    ("check-nw34-offline-integrity.ps1", "-SelfTest"),
    ("check-nw34-offline-integrity.ps1",),
    ("check-nw40-stability.ps1", "-SelfTest"),
    ("check-nw40-stability.ps1",),
    ("check-nw52-integration.ps1", "-SelfTest"),
    ("check-nw52-integration.ps1",),
    ("check-nw59-integration.ps1", "-SelfTest"),
    ("check-nw59-integration.ps1",),
    ("nw101-source-attestation.ps1", "-SelfTest"),
    ("nw76-junit-proof.ps1", "-SelfTest"),
    ("verify-build-provenance.ps1", "-SelfTest"),
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path, default=ROOT / "build/current-source-checks.json")
    args = parser.parse_args()
    args.report.parent.mkdir(parents=True, exist_ok=True)
    # Invalidate any older PASS before launching a check.
    args.report.write_text(json.dumps({"status": "NOT_VERIFIED"}) + "\n", encoding="utf-8")
    commands = [(sys.executable, "tools/" + entry[0], *entry[1:]) for entry in PYTHON_CHECKS]
    if os.name == "nt":
        commands.extend(("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                         "-File", "tools/" + entry[0], *entry[1:]) for entry in WINDOWS_CHECKS)
    results = []
    for command in commands:
        try:
            proc = subprocess.run(command, cwd=ROOT, capture_output=True, text=True,
                                  encoding="utf-8", errors="replace", timeout=120)
            code = proc.returncode
            output = proc.stdout + proc.stderr
        except (OSError, subprocess.TimeoutExpired) as exc:
            code, output = 2, type(exc).__name__
        # Report project-relative check names; absolute interpreter paths and
        # provider/user data never belong in a shareable diagnostic receipt.
        label = " ".join(command[1:] if command[0] == sys.executable else command[5:])
        results.append({"check": label, "exit_code": code})
        print(("PASS " if code == 0 else "FAIL ") + label, flush=True)
        if code:
            print(output.replace(str(ROOT), "<workspace>")[-4000:], flush=True)
    passed = all(result["exit_code"] == 0 for result in results)
    report = {"status": "PASS_SOURCE_ONLY" if passed else "FAIL", "checks": results,
              "windows_preflights": "EXECUTED" if os.name == "nt" else "NOT_RUN",
              "android_build": "NOT_RUN", "device_tests": "NOT_RUN"}
    args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
