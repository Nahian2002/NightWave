#!/usr/bin/env python3
"""NightWave internal Kotlin import linkage audit (no Android compiler).

Checks only project-owned symbols. External Compose/Media3/Google imports must
be resolved by the real Gradle build. Includes self-test to avoid false confidence.
"""
from __future__ import annotations
import argparse
from collections import defaultdict
from pathlib import Path
import re
import tempfile

PKG = re.compile(r"^\s*package\s+([\w.]+)\s*$", re.M)
IMPORT = re.compile(r"^\s*import\s+([\w.*]+)(?:\s+as\s+\w+)?\s*$", re.M)
DECL = re.compile(r"^(?:(?:public|private|internal|protected|open|final|abstract|sealed|data|enum|annotation|value|inline|expect|actual|suspend|tailrec|operator|infix|external|const|lateinit|override)\s+)*(?:class|interface|object|typealias|fun|val|var)\s+((?:[\w<>?.]+\.)?[\w]+)(?=\s|[:(<={]|$)")
FILE_TYPES = (".kt",)
SOURCE_DIRS = ("app/src/main/java", "app/src/test/java",
               "app/src/androidTest/java", "app/src/debug/java")
GENERATED = {
    "com.nightwave.player.R",
    "com.nightwave.player.BuildConfig",
    "com.nightwave.player.databinding",
}

def declarations(body: str, package: str) -> set[str]:
    """Collect top-level declarations, without mistaking nested class members."""
    found = set()
    depth = 0
    for line in body.splitlines():
        if depth == 0:
            match = DECL.match(line)
            if match:
                name = match.group(1).split(".")[-1]
                if re.fullmatch(r"[A-Za-z_]\w*", name):
                    found.add(package + "." + name)
        # This is a conservative source check, not a Kotlin lexer/compiler.
        # Brace characters in text and comments may affect this heuristic.
        depth = max(0, depth + line.count("{") - line.count("}"))
    return found

def audit(root: Path) -> tuple[int, int, list[str]]:
    units: list[tuple[Path, str, str]] = []
    for directory in SOURCE_DIRS:
        for path in sorted((root / directory).rglob("*.kt")):
            body = path.read_text(encoding="utf-8")
            package = PKG.search(body)
            if package is None:
                return 0, 0, [f"{path.relative_to(root)}: missing Kotlin package"]
            units.append((path, package.group(1), body))
    all_symbols: set[str] = set()
    for _, package, body in units:
        all_symbols.update(declarations(body, package))
    errors: list[str] = []
    imports_checked = 0
    for path, _, body in units:
        relative = path.relative_to(root)
        for m in IMPORT.finditer(body):
            name = m.group(1)
            if not name.startswith("com.nightwave.player."):
                continue
            imports_checked += 1
            if (name in all_symbols or name in GENERATED or
                any(name.startswith(x + ".") for x in GENERATED)):
                continue
            # Kotlin nested classes, imported companion members and
            # wildcard imports have a declared project-owned parent.
            if any(name.startswith(x + ".") for x in all_symbols):
                continue
            if name.endswith(".*"):
                prefix = name[:-2]
                if any(x.startswith(prefix + ".") for x in all_symbols):
                    continue
            errors.append(f"{relative}: unresolved project import {name}")
    return len(units), imports_checked, errors

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-kotlin-linkage-") as tmp:
        base = Path(tmp) / "app/src/main/java/test"
        base.mkdir(parents=True)
        (base / "A.kt").write_text(
            "package com.nightwave.player.example\n"
            "data class Track(val id: String)\n"
            "fun findTrack(): Track = Track(\"x\")\n",
            encoding="utf-8")
        other = base / "B.kt"
        other.write_text(
            "package com.nightwave.player.example.other\n"
            "import com.nightwave.player.example.Track\n"
            "import com.nightwave.player.example.findTrack\n"
            "import com.nightwave.player.R\n"
            "val test = findTrack()\n", encoding="utf-8")
        units, count, issues = audit(Path(tmp))
        assert units == 2 and count == 3 and not issues, (units, count, issues)
        other.write_text(
            "package com.nightwave.player.example.other\n"
            "import com.nightwave.player.example.MissingSong\n", encoding="utf-8")
        _, _, errors = audit(Path(tmp))
        assert len(errors) == 1 and "MissingSong" in errors[0], errors
    print("PASS: synthetic valid Kotlin project imports accepted; missing symbol rejected")

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--strict", action="store_true",
                        help="Fail if project-owned Kotlin imports cannot be linked")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    units, count, errors = audit(args.root)
    print(f"NightWave linkage audit: {units} Kotlin sources, {count} internal imports")
    for line in errors[:60]:
        print("UNRESOLVED: " + line)
    if len(errors) > 60:
        print(f"... and {len(errors)-60} more unresolved imports")
    if errors and args.strict:
        print("FAIL: unresolved project-owned Kotlin imports; Gradle has not run")
        return 1
    print(("WARNING" if errors else "PASS") +
          ": source-level import linkage; external APIs and Kotlin compilation unverified")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
