#!/usr/bin/env python3
"""NW90 fail-closed release evidence report. Python standard library only.

This report is NOT an APK signer, device QA, rights clearance, or a build.
It checks source/read-only evidence on the exact current checkout. It NEVER
marks missing build, release key, device, OAuth or 139-case results as PASS.

Examples:
    python3 tools/nw90_certification.py --self-test
    python3 tools/nw90_certification.py --project-root . \
      --report build/NightWave-NW90-candidate-gates.json
    python3 tools/nw90_certification.py --project-root . \
      --junit-dir app/build/test-results/testDebugUnitTest \
      --apk app/build/outputs/apk/debug/app-debug.apk \
      --report build/NightWave-NW90-after-build.json
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET

MATRIX = "docs/NW90_EXPANDED_139_CASE_MATRIX.csv"
MILESTONES = [f"tools/verify_nw{i}_{name}.py" for i, name in (
    (81, "titles"), (82, "search"), (83, "premium_visual"),
    (84, "home"), (85, "now_playing"), (86, "library"),
    (87, "transfer_offline"), (88, "sound_studio"), (89, "lyrics"),
)]
GATES = ("source_matrix", "jvm_unit_tests", "android_debug_apk",
         "android_test_compilation", "update_signer_compatibility",
         "p0_device_tests", "p1_device_tests", "google_drive_roundtrip",
         "android_audio_routes", "source_rights", "privacy_and_security",
         "play_store_requirements", "release_aab_and_signing")

def gate(status: str, explanation: str) -> dict:
    assert status in ("PASS", "FAIL", "NOT_VERIFIED", "BLOCKED")
    return {"status": status, "reason": explanation}

def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def evidence_junit(path: Path, minimum: int = 1000) -> tuple[dict, dict]:
    files = sorted(path.glob("**/TEST-*.xml")) if path.is_dir() else []
    if not files:
        return gate("NOT_VERIFIED", "No fresh JUnit XML test receipt supplied."), {}
    counts = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    try:
        for file in files:
            xml = file.read_bytes()
            # ElementTree must not be allowed to resolve malicious DTDs or
            # entities from test XML; treat even an internal DTD as invalid.
            if b"<!DOCTYPE" in xml.upper() or b"<!ENTITY" in xml.upper():
                return gate("FAIL", "JUnit XML contains prohibited DTD/entity."), {}
            node = ET.fromstring(xml)
            if node.tag not in ("testsuite", "testsuites"):
                return gate("FAIL", "JUnit document has unexpected root."), {}
            suites = ([node] if node.tag == "testsuite" else
                      list(node.iter("testsuite")))
            for suite in suites:
                for field in counts:
                    value = int(suite.attrib.get(field, "0"))
                    if value < 0:
                        raise ValueError("negative result counter")
                    counts[field] += value
                testcase = list(suite.iter("testcase"))
                if int(suite.attrib.get("tests", "0")) != len(testcase):
                    return gate("FAIL", "JUnit suite count and testcase list differ."), counts
                for field, child in (("failures", "failure"),
                                     ("errors", "error"), ("skipped", "skipped")):
                    if int(suite.attrib.get(field, "0")) != sum(
                        1 for item in testcase for sub in item if sub.tag == child
                    ):
                        return gate("FAIL", "JUnit suite counter mismatch: " + field), counts
    except (ET.ParseError, ValueError, OSError):
        return gate("FAIL", "JUnit XML could not be safely validated."), {}
    if counts["tests"] < minimum:
        return gate("FAIL", f"Only {counts['tests']} JUnit tests; need >= {minimum}."), counts
    if counts["failures"] or counts["errors"]:
        return gate("FAIL", "JUnit recorded failures or errors."), counts
    return gate("PASS", "Fresh JUnit XML counters valid (skips are reported)."), counts

def matrix_status(path: Path) -> tuple[dict, dict]:
    if not path.is_file():
        return gate("FAIL", "Expanded 139-case matrix missing."), {}
    with path.open(encoding="utf-8", newline="") as stream:
        data = list(csv.DictReader(stream))
    ids = [r["case_id"] for r in data]
    statuses = ("NOT_RUN", "PASS", "FAIL", "BLOCKED")
    if len(data) != 139 or len(set(ids)) != 139 or any(
        not re.fullmatch(r"NW90-[0-9]{3}", id) for id in ids
    ) or any(
        r.get("status") not in statuses or not r.get("area") or
        not r.get("scenario") or not r.get("expected") or
        r.get("severity") not in ("P0", "P1", "P2") or
        (r["status"] == "PASS" and not r.get("evidence", "").strip())
        for r in data
    ):
        return gate("FAIL", "Matrix must contain 139 unique valid cases with truthful evidence."), {}
    summary = {s: sum(r["status"] == s for r in data) for s in statuses}
    summary["total"] = len(data)
    summary["p0_total"] = sum(r["severity"] == "P0" for r in data)
    summary["p1_total"] = sum(r["severity"] == "P1" for r in data)
    return gate("PASS", "139 unique defined cases; unexecuted cases remain NOT_RUN."), summary

def report(root: Path, junit_dir: Path | None, apk: Path | None) -> dict:
    matrix_gate, counts = matrix_status(root / MATRIX)
    checks = {name: gate("NOT_VERIFIED", "No current-source execution evidence.")
              for name in GATES}
    checks["source_matrix"] = matrix_gate
    junit_summary = {}
    if junit_dir is not None:
        checks["jvm_unit_tests"], junit_summary = evidence_junit(junit_dir)
    if apk is not None and apk.is_file() and apk.stat().st_size > 100_000:
        checks["android_debug_apk"] = gate(
            "NOT_VERIFIED", "APK exists; signer, package identity and successful build receipt not yet verified.")
        apk_digest = sha256_file(apk)
    else:
        apk_digest = None
    # This is an explicit product-level rights blocker, not a local-build
    # failure. It persists even after an Android/JVM compilation succeeds.
    checks["source_rights"] = gate(
        "BLOCKED", "NW77 YouTube standalone/background and offline extraction paths "
                   "need compliant redesign or applicable authorization and music rights.")
    # Android phone apps submitted to Google Play from 31 Aug 2026
    # generally need API 36+. Do not silently raise the target/compile SDK
    # without migration and a successful Android 16/device regression run.
    gradle = root / "app/build.gradle.kts"
    if gradle.is_file():
        build_text = gradle.read_text(encoding="utf-8")
        match = re.search(r"(?m)^\s*targetSdk\s*=\s*(\d+)", build_text)
        current_sdk = int(match.group(1)) if match else None
        if current_sdk is None or current_sdk < 36:
            checks["play_store_requirements"] = gate(
                "BLOCKED", f"targetSdk={current_sdk!s}; Play phone app submission "
                "requires API 36+ unless applicable Play Console extension. "
                "Migrate and retest the entire app before publication.")
        else:
            checks["play_store_requirements"] = gate(
                "NOT_VERIFIED", f"targetSdk={current_sdk}; Play target value "
                "is present, but store compliance and Android 16 runtime not certified.")
    else:
        checks["play_store_requirements"] = gate(
            "BLOCKED", "No app/build.gradle.kts available to inspect target API.")

    # A known risky code path and missing real-device evidence require NO_GO.
    try:
        commit = subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=root,
            text=True, timeout=5, stderr=subprocess.DEVNULL
        ).strip()
    except (OSError, subprocess.SubprocessError):
        commit = "UNKNOWN"
    return {
        "phase": "NW90",
        "commit": commit,
        "release_decision": "NO_GO",
        "public_release_approved": False,
        "source_branch_note": "Cumulative NW76–NW89 development source; NW90 certification pending",
        "gates": checks,
        "acceptance_139": counts,
        "junit": junit_summary,
        "debug_apk_sha256_unverified_signer": apk_digest,
        "source_contract_paths": MILESTONES,
        "privacy_note": "No account addresses, tokens, library titles or absolute paths in this report.",
    }

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-nw90-gate-") as tmp:
        r = Path(tmp)
        matrix = r / MATRIX
        matrix.parent.mkdir(parents=True)
        with matrix.open("w", encoding="utf-8", newline="") as stream:
            w = csv.writer(stream)
            w.writerow(("case_id", "area", "scenario", "expected",
                        "severity", "status", "evidence"))
            for i in range(1, 140):
                w.writerow((f"NW90-{i:03}", "Smoke", f"Case {i}",
                            "Tested behavior", "P0", "NOT_RUN", ""))
        state, summary = matrix_status(matrix)
        assert state["status"] == "PASS" and summary["NOT_RUN"] == 139
        with matrix.open("a", encoding="utf-8") as stream:
            stream.write('NW90-001,Smoke,duplicate,invalid,P0,PASS,\n')
        assert matrix_status(matrix)[0]["status"] == "FAIL"
        suite = r / "junit"
        suite.mkdir()
        (suite / "TEST-a.xml").write_text(
            '<testsuite tests="2" failures="0" errors="0" skipped="1">'
            '<testcase name="good"/><testcase name="skip"><skipped/></testcase></testsuite>'
        )
        state, results = evidence_junit(suite, minimum=2)
        assert state["status"] == "PASS" and results["skipped"] == 1
        state, _ = evidence_junit(suite, minimum=1000)
        assert state["status"] == "FAIL"
        (suite / "TEST-a.xml").write_text(
            '<!DOCTYPE testsuite [<!ENTITY x "hijack">]><testsuite tests="0"/>'
        )
        assert evidence_junit(suite, minimum=0)[0]["status"] == "FAIL"
    print("NW90_FAIL_CLOSED_REPORT_SELF_TEST_PASS")

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", default=".")
    parser.add_argument("--report", default="build/NightWave-NW90-candidate-gates.json")
    parser.add_argument("--junit-dir")
    parser.add_argument("--apk")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    root = Path(args.project_root).resolve()
    def in_root(path: str | None) -> Path | None:
        if path is None:
            return None
        p = Path(path)
        return p if p.is_absolute() else root / p
    result = report(root, in_root(args.junit_dir), in_root(args.apk))
    dest = in_root(args.report)
    assert dest is not None
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("NW90 source matrix:", result["gates"]["source_matrix"]["status"])
    print("NW90 public release:", result["release_decision"])
    print("NW90 confirmed JVM:", result["gates"]["jvm_unit_tests"]["status"])
    print("NW90 device cases:", result["acceptance_139"])
    return 0 if result["gates"]["source_matrix"]["status"] == "PASS" else 2

if __name__ == "__main__":
    raise SystemExit(main())
