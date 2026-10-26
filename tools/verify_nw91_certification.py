#!/usr/bin/env python3
"""NW91 regression fixtures for the cumulative NW90 certification machinery.

Exercises real parser/report functions using synthetic temporary receipts.
Never touches installed-app data, Google auth, Android media or signers.
"""
from __future__ import annotations

import csv
import os
from pathlib import Path
import tempfile

from nw90_certification import evidence_junit, matrix_status, report, MATRIX


def write_matrix(root: Path, wrong_id: bool = False) -> Path:
    path = root / MATRIX
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream)
        writer.writerow(("case_id", "area", "scenario", "expected",
                         "severity", "status", "evidence"))
        for i in range(1, 140):
            n = 999 if (wrong_id and i == 139) else i
            writer.writerow((f"NW90-{n:03}", "Fixture", f"Case {i}",
                             "Expected state", "P0", "NOT_RUN", ""))
    return path


def junit(path: Path, skipped: bool) -> Path:
    path.mkdir(parents=True, exist_ok=True)
    suffix = '<skipped/>' if skipped else ''
    xml = ('<testsuite tests="2" failures="0" errors="0" skipped="' +
           ("1" if skipped else "0") +
           '"><testcase name="one"/><testcase name="two">' +
           suffix + '</testcase></testsuite>')
    report_path = path / "TEST-nw91.xml"
    report_path.write_text(xml, encoding="utf-8")
    return report_path


def test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-nw91-") as d:
        root = Path(d)
        matrix = write_matrix(root)
        status, counts = matrix_status(matrix)
        assert status["status"] == "PASS" and counts["NOT_RUN"] == 139
        write_matrix(root, wrong_id=True)
        assert matrix_status(matrix)[0]["status"] == "FAIL"
        write_matrix(root)
        suite = root / "test-results"
        xml = junit(suite, skipped=True)
        assert evidence_junit(suite, minimum=2)[0]["status"] == "FAIL"
        assert evidence_junit(suite, minimum=1)[0]["status"] == "PASS"
        xml = junit(suite, skipped=False)
        assert evidence_junit(suite, minimum=2)[0]["status"] == "PASS"

        log = root / "gradle.log"
        marker = root / "started.marker"
        log.write_text("BUILD SUCCESSFUL in 8s\n", encoding="utf-8")

        # A successful log without an attempt marker must never validate XML.
        result = report(root, suite, None, log, None)
        assert result["gates"]["jvm_unit_tests"]["status"] == "NOT_VERIFIED"
        marker.touch()
        started = marker.stat().st_mtime_ns
        earlier = started - 2_000_000_000
        later = started + 2_000_000_000
        os.utime(xml, ns=(earlier, earlier))
        os.utime(log, ns=(later, later))
        assert report(root, suite, None, log, marker)["gates"]["jvm_unit_tests"]["status"] == "NOT_VERIFIED"
        os.utime(xml, ns=(later, later))
        assert report(root, suite, None, log, marker)["gates"]["jvm_unit_tests"]["status"] == "PASS"
        log.write_text("BUILD SUCCESSFUL in 8s\nBUILD FAILED in 9s\n", encoding="utf-8")
        os.utime(log, ns=(later, later))
        assert report(root, suite, None, log, marker)["gates"]["jvm_unit_tests"]["status"] == "NOT_VERIFIED"
        assert report(root, suite, None, log, marker)["public_release_approved"] is False

    print("NW91_CERTIFICATION_REGRESSION_FIXTURES_PASS")
    print("Synthetic receipts only: Android compile, device and rights remain unverified.")


if __name__ == "__main__":
    test()
