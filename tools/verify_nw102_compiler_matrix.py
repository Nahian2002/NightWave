#!/usr/bin/env python3
"""NW102 source matrix and workflow static checks (NOT a compiler result)."""
from pathlib import Path
import ast
root=Path(__file__).resolve().parents[1]
script=(root/"tools/nw102_kotlin_junit_gate.py").read_text(encoding="utf-8")
ast.parse(script)
ci=(root/".github/workflows/android.yml").read_text(encoding="utf-8")
canary=(root/".github/workflows/nightwave-runner-canary.yml").read_text(encoding="utf-8")
readme=(root/"README.md").read_text(encoding="utf-8")
assert script.count('"app/src/test/java/com/nightwave/player/')==8
assert script.count('"app/src/main/java/com/nightwave/player/')==9
for mark in ('assert TOTAL == 53','"kotlinc"','"javac"',
             'NW102_REAL_TEST_SOURCES_PASS cases=53',
             'NW102_NEGATIVE_ASSERTION_CONTROL_PASS','official_gradle_junit',
             'source_sha256'):
    assert mark in script,mark
assert 'tools/nw102_kotlin_junit_gate.py --self-test' in ci
assert 'tools/nw102_kotlin_junit_gate.py --report' in ci
assert 'tools/verify_nw102_compiler_matrix.py' in ci
assert 'feature/nightwave-nw102-jvm-test-source-compiler-gate' in ci
assert 'feature/nightwave-nw102-jvm-test-source-compiler-gate' in canary
assert 'feature/nightwave-nw102-jvm-test-source-compiler-gate' in readme
print("NW102_REAL_TEST_SOURCE_MATRIX_WIRING_PASS")
