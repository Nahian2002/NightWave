#!/usr/bin/env python3
"""Inventory and statically scan every tracked source file; no release certification.

Run in an extracted full NightWave ZIP. Does not read keys, hidden files,
build artifacts, installed apps or user music; does not connect to the internet.
Exits nonzero on confirmed structural errors, with bounded relative paths.
"""
from __future__ import annotations
import argparse
import ast
import collections
import json
from pathlib import Path
import py_compile
import re
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SOURCE_EXT = {".kt", ".kts", ".py", ".ps1", ".cmd", ".xml", ".yml",
              ".yaml", ".properties", ".pro", ".md"}
EXCLUDE_PARTS = {".git", ".gradle", ".idea", "build", "dist", "__pycache__",
                 ".venv", "node_modules", ".nightwave", "cache"}
MAX_SOURCE_BYTES = 3_000_000
MAX_FINDINGS = 100
PRIVATE_MARKERS = ("BEGIN PRIVATE KEY", "BEGIN RSA PRIVATE KEY")

def inventory(root: Path) -> tuple[dict[str, int], list[str], list[str]]:
    counts: dict[str, int] = collections.Counter()
    findings: list[str] = []
    warnings: list[str] = []
    for path in sorted(root.rglob("*")):
        if not path.is_file() or any(part in EXCLUDE_PARTS for part in path.relative_to(root).parts):
            continue
        rel = path.relative_to(root).as_posix()
        counts["files"] += 1
        counts["bytes"] += path.stat().st_size
        counts["lines"] += 0
        ext = path.suffix.lower()
        counts["ext_" + (ext or "other")] += 1
        if ext not in SOURCE_EXT:
            continue
        counts["source_files"] += 1
        if path.is_symlink():
            findings.append(rel + ": source path may not be a symlink")
            continue
        size = path.stat().st_size
        if size > MAX_SOURCE_BYTES:
            warnings.append(rel + ": not scanned; exceeds source size bound")
            continue
        try:
            data = path.read_text(encoding="utf-8-sig")
        except (UnicodeError, OSError):
            findings.append(rel + ": unreadable UTF-8 source")
            continue
        counts["lines"] += data.count("\n") + bool(data and not data.endswith("\n"))
        if "\0" in data:
            findings.append(rel + ": binary NUL in text source")
            continue
        if any(marker in data for marker in PRIVATE_MARKERS):
            findings.append(rel + ": possible committed private signing/key material")
        if ext == ".py":
            try:
                ast.parse(data, filename=rel)
            except SyntaxError as exc:
                findings.append(rel + ": Python parse failure at line " + str(exc.lineno))
        elif ext == ".xml":
            try:
                ET.fromstring(data)
            except ET.ParseError as exc:
                findings.append(rel + ": XML parse failure at line " + str(exc.position[0]))
        elif ext in {".kt", ".kts"}:
            imports = re.findall(r"^import\s+([\w.]+(?:\.\*)?)\s*$", data, re.M)
            duplicated = {imp for imp, freq in collections.Counter(imports).items() if freq > 1}
            if duplicated:
                findings.append(rel + ": duplicate exact Kotlin imports (" + str(len(duplicated)) + ")")
            if re.search(r"(?m)^\s*<<<<<<< |^\s*=======\s*$|^\s*>>>>>>> ", data):
                findings.append(rel + ": unresolved Git conflict marker")
        if ext != ".md" and re.search(r"(?m)^<<<<<<< |^>>>>>>> ", data):
            findings.append(rel + ": unresolved merge conflict marker")
    return dict(counts), findings[:MAX_FINDINGS], warnings[:MAX_FINDINGS]

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nw22-static-audit-") as td:
        root = Path(td)
        (root / "ok.py").write_text("x = 1\n", encoding="utf-8")
        (root / "bad.py").write_text("def broken(:\n", encoding="utf-8")
        (root / "bad.xml").write_text("<root><a></root>", encoding="utf-8")
        (root / "Dup.kt").write_text("import kotlin.String\nimport kotlin.String\n",
                                      encoding="utf-8")
        counts, findings, warnings = inventory(root)
        assert counts["source_files"] == 4
        assert len(findings) == 3, findings
        assert not warnings
        (root / "bad.py").write_text("x = 2\n", encoding="utf-8")
        (root / "bad.xml").write_text("<root/>", encoding="utf-8")
        (root / "Dup.kt").write_text("import kotlin.String\nimport kotlin.Int\n",
                                      encoding="utf-8")
        counts, findings, warnings = inventory(root)
        assert counts["source_files"] == 4 and not findings and not warnings
    print("PASS: NW22 full-tree static audit positive and negative fixtures")

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    counts, findings, warnings = inventory(ROOT)
    result = {
        "status": "STATIC_FINDINGS" if findings else "STATIC_SCAN_PASSED",
        "inventory": counts,
        "findings": findings,
        "warnings": warnings,
        "limitations": ("Python syntax, XML wellformedness, UTF-8, exact Kotlin imports "
                        "and conflict/key markers only. Not Kotlin/Gradle compilation, "
                        "PowerShell parsing, execution or Android device QA.")
    }
    if args.json:
        print(json.dumps(result, indent=2, sort_keys=True))
    else:
        print("NW22 repository static scan:", result["status"])
        print("Files:", counts.get("files", 0), "source:", counts.get("source_files", 0),
              "source lines:", counts.get("lines", 0))
        for finding in findings:
            print("FAIL:", finding)
        for warning in warnings:
            print("WARN:", warning)
        print("LIMIT:", result["limitations"])
    return 1 if findings else 0

if __name__ == "__main__":
    raise SystemExit(main())
