#!/usr/bin/env python3
"""Detect a Kotlin test fixture type trap before Gradle compilation.

Bad: parse("line1" + "line2".toByteArray())
The '+' expression is a String, not the ByteArray expected by parse().
Good: parse(("line1" + "line2").toByteArray())

This is a narrowly scoped source regression check, NOT Kotlin compilation.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import re
import tempfile

ROOT = Path(__file__).resolve().parents[1]
TEST_ROOTS = ("app/src/test", "app/src/androidTest")
QUOTED = r'"(?:\\.|[^"\\])*"'
PATTERN = re.compile(
    rf"(?:{QUOTED}\s*\+\s*)+{QUOTED}\s*\.toByteArray\s*\(",
    re.MULTILINE,
)


def find_issues(root: Path) -> list[str]:
    problems = []
    for folder in TEST_ROOTS:
        base = root / folder
        if not base.is_dir():
            problems.append("Missing test source directory: " + folder)
            continue
        for file in sorted(base.rglob("*.kt")):
            body = file.read_text(encoding="utf-8")
            for match in PATTERN.finditer(body):
                line = body.count("\n", 0, match.start()) + 1
                # Source locations only; never print literal test payloads.
                problems.append(f"{file.relative_to(root)}:{line}: "
                                "join all Kotlin String segments BEFORE .toByteArray()")
    return problems


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-bytes-fixture-") as folder:
        root = Path(folder)
        unit = root / TEST_ROOTS[0] / "Broken.kt"
        android = root / TEST_ROOTS[1] / "Screen.kt"
        unit.parent.mkdir(parents=True, exist_ok=True)
        android.parent.mkdir(parents=True, exist_ok=True)
        unit.write_text(
            'parse("[00:01]First\\n" +\n    "[00:02]Second".toByteArray())\n',
            encoding="utf-8",
        )
        android.write_text(
            'parse(("[00:01]First\\n" +\n    "[00:02]Second").toByteArray())\n',
            encoding="utf-8",
        )
        problems = find_issues(root)
        assert len(problems) == 1 and "Broken.kt" in problems[0], problems
        unit.write_text(
            'parse(("[00:01]First\\n" +\n    "[00:02]Second").toByteArray())\n'
            'parse("single".toByteArray())\n'
            'val x = "not converted" + "either"\n',
            encoding="utf-8",
        )
        assert find_issues(root) == [], find_issues(root)
        android.write_text(
            'parse("a" + "b".toByteArray())\n', encoding="utf-8"
        )
        problems = find_issues(root)
        assert len(problems) == 1 and "Screen.kt" in problems[0], problems
    print("PASS: byte-array fixture regression catches grouped String conversion")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    issues = find_issues(args.root)
    for issue in issues:
        print("FAIL:", issue)
    if issues:
        return 1
    print("PASS: no ungrouped Kotlin string-to-ByteArray fixture concatenations")
    print("NOT VERIFIED: Android or JVM compilation / runtime")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
