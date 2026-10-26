#!/usr/bin/env python3
"""NW103 fail-closed receipt lifecycle tests. No Android or network required."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import tempfile

import nw102_kotlin_junit_gate as gate


def refuse(callback) -> None:
    try:
        callback()
    except RuntimeError:
        return
    raise AssertionError("NW103 negative fixture was incorrectly accepted")


def main() -> None:
    checks = 0

    def require(value: bool, label: str) -> None:
        nonlocal checks
        assert value, label
        checks += 1

    with tempfile.TemporaryDirectory(prefix="nw103-receipt-") as directory:
        root = Path(directory)
        old_root = gate.ROOT
        try:
            gate.ROOT = root
            report = root / "build" / "nw102-source-tests.json"
            report.parent.mkdir()
            report.write_text(
                json.dumps({"status": "POLICY_JVM_TESTS_PASS_ONLY",
                            "executed_tests": 53}), encoding="utf-8")
            gate.begin_attempt(report)
            current = json.loads(report.read_text(encoding="utf-8"))
            require(current["status"] == "NOT_VERIFIED", "stale PASS invalidated")
            require(current["executed_tests"] == 0, "case count reset")
            require(not report.with_name(report.name + ".writing").exists(),
                    "no unfinished receipt")
            source = root / "Source.kt"
            source.write_text("old", encoding="utf-8")
            before = gate.source_fingerprints([source])
            gate.ensure_unchanged([source], before)
            require(before["Source.kt"] ==
                    hashlib.sha256(b"old").hexdigest(), "actual hash")
            source.write_text("mutated", encoding="utf-8")
            refuse(lambda: gate.ensure_unchanged([source], before))
            require(json.loads(report.read_text(encoding="utf-8"))["status"] ==
                    "NOT_VERIFIED", "failed attempt cannot inherit PASS")
            source.write_text("old", encoding="utf-8")
            gate.write_atomic_receipt(report, {
                "status": "POLICY_JVM_TESTS_PASS_ONLY",
                "executed_tests": 53,
            })
            final = json.loads(report.read_text(encoding="utf-8"))
            require(final["executed_tests"] == 53, "full success writes receipt")
            require(final["status"] == "POLICY_JVM_TESTS_PASS_ONLY",
                    "success label scoped to JVM")
            require(not report.with_name(report.name + ".writing").exists(),
                    "successful atomic cleanup")
            gate.begin_attempt(report)
            require(json.loads(report.read_text(encoding="utf-8"))["status"] ==
                    "NOT_VERIFIED", "next run invalidates old success again")
        finally:
            gate.ROOT = old_root
    require(checks == 9, "fixture case count")
    print("NW103_FAIL_CLOSED_RECEIPT_SELFTEST_PASS cases=10")


if __name__ == "__main__":
    main()
