#!/usr/bin/env python3
"""NW103 full-source wiring check; NOT a proof of Gradle build success."""
from pathlib import Path
import ast

root = Path(__file__).resolve().parents[1]
read = lambda path: (root / path).read_text(encoding="utf-8")
script = read("tools/nw102_kotlin_junit_gate.py")
fixture = read("tools/nw103_receipt_selftest.py")
ci = read(".github/workflows/android.yml")
canary = read(".github/workflows/nightwave-runner-canary.yml")
readme = read("README.md")
doc = read("docs/NW103_BUILD_PIPELINE_EVIDENCE.md")

ast.parse(script)
ast.parse(fixture)
for expected in (
    'def begin_attempt(path: Path) -> None:',
    '"status": "NOT_VERIFIED"',
    '"executed_tests": 0',
    'begin_attempt(target)',
    'source_hashes = source_fingerprints(source_files)',
    'ensure_unchanged(source_files, source_hashes)',
    'write_atomic_receipt(target, report)',
    'os.replace(pending, path)',
    '"source_sha256": source_hashes',
    'NW102_REAL_TEST_SOURCES_PASS cases=53',
):
    assert expected in script, expected
for expected in (
    "nw103_receipt_selftest.py",
    "verify_nw103_build_pipeline.py",
    "nightwave-nw102-jvm-source-tests.json",
    "if: always()",
    "NightWave-NW103-JVM-EVIDENCE-NOT-ANDROID-APPROVAL",
    "feature/nightwave-nw103-build-receipt-final-gates",
):
    assert expected in ci, expected
assert "feature/nightwave-nw103-build-receipt-final-gates" in canary
assert "feature/nightwave-nw103-build-receipt-final-gates" in readme
assert "Release status: NO-GO" in doc
print("NW103_BUILD_PIPELINE_SOURCE_WIRING_PASS checks=18")
