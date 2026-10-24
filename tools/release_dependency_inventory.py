#!/usr/bin/env python3
"""Read-only inventory of explicit Gradle dependencies and build plugins.

No Gradle downloads, no app changes, no network and no vulnerability claims.
This is NOT a transitive SBOM: resolve actual Gradle dependencies separately.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def inventory(root: Path) -> dict:
    project = root / "build.gradle.kts"
    app = root / "app" / "build.gradle.kts"
    if not project.is_file() or not app.is_file():
        raise FileNotFoundError("Requires full NightWave Gradle project source")
    app_lines = app.read_text(encoding="utf-8-sig").splitlines()
    root_lines = project.read_text(encoding="utf-8-sig").splitlines()
    dependency_re = re.compile(
        r'^\s*(implementation|debugImplementation|testImplementation|androidTestImplementation|ksp|coreLibraryDesugaring)\s*\(\s*(?:platform\()?\"([^"]+)\"'
    )
    plugin_re = re.compile(
        r'^\s*id\(\"([^"]+)\"\)\s+version\s+\"([^"]+)\"'
    )
    dependencies = []
    for number, line in enumerate(app_lines, 1):
        found = dependency_re.match(line)
        if found:
            dependencies.append({"scope": found.group(1), "coordinate": found.group(2),
                                 "source_line": number})
    plugins = []
    for number, line in enumerate(root_lines, 1):
        match = plugin_re.match(line)
        if match:
            plugins.append({"id": match.group(1), "version": match.group(2),
                            "source_line": number})
    if not dependencies or not plugins:
        raise ValueError("Gradle dependency or plugin declarations not found")
    return {"dependencies": dependencies, "plugins": plugins,
            "counts": {"dependencies": len(dependencies), "plugins": len(plugins)},
            "status": "DECLARATIONS_ONLY_UNREVIEWED",
            "limitations": ("No transitive dependencies, lockfile, CVE scanner, license "
                            "approval or network dependency resolution were run.")}

def self_test() -> None:
    import tempfile
    with tempfile.TemporaryDirectory(prefix="nw-dep-fixture-") as td:
        root = Path(td)
        (root / "app").mkdir()
        (root / "build.gradle.kts").write_text(
            'plugins {\n id("com.android.application") version "8.9.2" apply false\n}\n',
            encoding="utf-8")
        (root / "app" / "build.gradle.kts").write_text(
            'dependencies {\n implementation("a:b:1.0")\n'
            ' implementation(platform("a:bom:1"))\n'
            ' testImplementation("junit:junit:4.13.2")\n}\n', encoding="utf-8")
        report = inventory(root)
        assert report["counts"] == {"dependencies": 3, "plugins": 1}, report
        assert report["dependencies"][1]["coordinate"] == "a:bom:1"
        assert report["status"] == "DECLARATIONS_ONLY_UNREVIEWED"
    print("PASS: Gradle dependency inventory fixture")

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        self_test()
        return 0
    report = inventory(ROOT)
    if args.json:
        print(json.dumps(report, indent=2, sort_keys=True))
    else:
        print("NightWave dependency manifest:", report["status"])
        print("Dependencies:", report["counts"]["dependencies"],
              "plugins:", report["counts"]["plugins"])
        for item in report["dependencies"]:
            print(item["scope"], item["coordinate"])
        print("NOT verified: transitive dependency CVEs or licenses")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
