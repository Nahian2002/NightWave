#!/usr/bin/env python3
"""Read-only NightWave source/security release preflight.

No Android app changes, no network, no credentials, no uploads.
--history requires a complete git clone and an installed Gitleaks v8 CLI.
Only rule IDs, relative paths and line numbers are displayed, not secrets.
A clean static scan NEVER establishes a public release GO decision.
"""
from __future__ import annotations

import argparse
import ast
import json
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IGNORED = {".git", ".gradle", ".idea", "dist", "build", "__pycache__",
           ".local-build", ".venv", "node_modules"}
EXTENSIONS = {".kt", ".kts", ".py", ".xml", ".json", ".yml", ".yaml",
              ".ps1", ".cmd", ".properties", ".md", ".txt", ".toml", ".pro"}
MAX_BYTES = 3_000_000
SECRET_RULES = (
    ("possible-private-key", re.compile(r"(?m)^-----BEGIN (?:RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY-----")),
    ("possible-google-api-key", re.compile(r"AIza[0-9A-Za-z_-]{35}")),
    ("possible-github-token", re.compile(r"(?:ghp_|gho_|ghu_|ghs_|ghr_)[A-Za-z0-9]{30,}")),
    ("possible-github-fine-grained-token", re.compile(r"github_pat_[A-Za-z0-9_]{50,}")),
    ("possible-aws-access-key", re.compile(r"\b(?:AKIA|ASIA)[A-Z0-9]{16}\b")),
    ("possible-stripe-live-secret", re.compile(r"\bsk_live_[0-9a-zA-Z]{20,}\b")),
)
SENSITIVE_NAMES = (
    re.compile(r"\.(?:jks|keystore|p12|pfx|pem)$", re.I),
    re.compile(r"^google-services\.json$", re.I),
    re.compile(r"^service-account.*\.json$", re.I),
    re.compile(r"^play-service-account.*\.json$", re.I),
    re.compile(r"^credentials\.json$", re.I),
    re.compile(r"^\.env(?:\..+)?$", re.I),
)


def check_tree(root: Path) -> dict:
    findings = []
    counts = Counter()
    if not (root / "app" / "build.gradle.kts").is_file():
        findings.append({"rule": "missing-android-build", "path": "app/build.gradle.kts"})
    for path in sorted(root.rglob("*")):
        if any(segment in IGNORED for segment in path.relative_to(root).parts):
            continue
        if not path.is_file():
            continue
        rel = path.relative_to(root).as_posix()
        counts["files"] += 1
        if any(pat.match(path.name) for pat in SENSITIVE_NAMES):
            findings.append({"rule": "possible-sensitive-file", "path": rel})
        if path.suffix.lower() not in EXTENSIONS:
            counts["non_text_files"] += 1
            continue
        counts["source_files"] += 1
        if path.stat().st_size > MAX_BYTES:
            findings.append({"rule": "source-too-large-to-scan", "path": rel})
            continue
        try:
            data = path.read_text(encoding="utf-8-sig")
        except (UnicodeError, OSError):
            findings.append({"rule": "source-unreadable", "path": rel})
            continue
        for rule, pattern in SECRET_RULES:
            for match in list(pattern.finditer(data))[:3]:
                findings.append({
                    "rule": rule, "path": rel,
                    "line": data.count("\n", 0, match.start()) + 1
                })
        if "\0" in data:
            findings.append({"rule": "unexpected-nul", "path": rel})
        if path.suffix.lower() == ".py":
            try:
                ast.parse(data, filename=rel)
            except SyntaxError as exc:
                findings.append({"rule": "python-syntax", "path": rel,
                                 "line": exc.lineno})
        if path.suffix.lower() == ".xml":
            try:
                ET.fromstring(data)
            except ET.ParseError as exc:
                findings.append({"rule": "xml-syntax", "path": rel,
                                 "line": exc.position[0]})
        if path.suffix.lower() in (".kt", ".kts"):
            imports = re.findall(r"(?m)^import\s+([\w.*]+)(?:\s+as\s+\w+)?\s*$", data)
            if any(value > 1 for value in Counter(imports).values()):
                findings.append({"rule": "duplicate-kotlin-import", "path": rel})
        if re.search(r"(?m)^(?:<<<<<<< |>>>>>>> )", data):
            findings.append({"rule": "unresolved-git-conflict", "path": rel})
    gradle = root / "app" / "build.gradle.kts"
    if gradle.is_file():
        body = gradle.read_text(encoding="utf-8-sig")
        target = re.search(r"\btargetSdk\s*=\s*(\d+)", body)
        if not target or int(target.group(1)) < 36:
            findings.append({"rule": "play-target-sdk-below-36-or-unknown",
                             "path": "app/build.gradle.kts"})
    for required in ("LICENSE", "NOTICE", "SECURITY.md", "CONTRIBUTING.md",
                     "PRIVACY_POLICY_DRAFT.md",
                     "docs/PUBLIC_RELEASE_EXECUTION_PLAN.md"):
        if not (root / required).is_file():
            findings.append({"rule": "missing-public-release-document", "path": required})
    return {"inventory": dict(counts), "findings": findings,
            "history_scan": "NOT_RUN", "release_decision": "NO_GO",
            "warning": ("This is a bounded static source check, not Android "
                        "compilation, complete secret detection or publication approval.")}


def history_scan(root: Path) -> dict:
    if not (root / ".git").exists():
        return {"status": "NOT_RUN", "reason": "Use a complete git clone, not ZIP"}
    tool = shutil.which("gitleaks")
    if not tool:
        return {"status": "NOT_RUN", "reason": "Gitleaks CLI not installed"}
    output = root / "dist" / "NightWave-private-history-leaks.json"
    output.parent.mkdir(exist_ok=True)
    # Gitleaks v8 git mode scans git patches and --all traverses local refs.
    command = [tool, "git", "--source", str(root),
               "--log-opts=--all --full-history",
               "--redact", "--no-banner", "--report-format", "json",
               "--report-path", str(output)]
    try:
        completed = subprocess.run(command, cwd=root, capture_output=True,
                                   text=True, timeout=1200, check=False)
    except (OSError, subprocess.TimeoutExpired):
        return {"status": "SCANNER_ERROR", "reason": "Unable to execute full scan"}
    if completed.returncode == 0:
        return {"status": "NO_MATCHES", "report": "dist/NightWave-private-history-leaks.json",
                "limitations": "Gitleaks scan is not proof of no secrets"}
    if completed.returncode == 1:
        return {"status": "POSSIBLE_SECRETS", "report": "dist/NightWave-private-history-leaks.json"}
    return {"status": "SCANNER_ERROR", "code": completed.returncode,
            "reason": "Review local Gitleaks v8 setup; scanner output was not copied"}


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-release-selftest-") as d:
        root = Path(d)
        (root / "app").mkdir()
        (root / "app" / "build.gradle.kts").write_text(
            "android { targetSdk = 35 }\n", encoding="utf8")
        (root / "bad.py").write_text("def bad(:\n", encoding="utf8")
        (root / "bad.xml").write_text("<a><b></a>", encoding="utf8")
        (root / "dup.kt").write_text("import kotlin.String\nimport kotlin.String\n",
                                     encoding="utf8")
        report = check_tree(root)
        rules = {item["rule"] for item in report["findings"]}
        assert {"python-syntax", "xml-syntax", "duplicate-kotlin-import",
                "play-target-sdk-below-36-or-unknown"} <= rules, rules
        assert report["release_decision"] == "NO_GO"
    print("PASS: source preflight synthetic positive/negative checks")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--history", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    report = check_tree(ROOT)
    if args.history:
        report["history_scan"] = history_scan(ROOT)
    if args.json:
        print(json.dumps(report, indent=2, sort_keys=True))
    else:
        print("NightWave public release preflight:", report["release_decision"])
        print("Reviewed", report["inventory"].get("files", 0), "files.")
        for issue in report["findings"]:
            print("BLOCKER:", issue["rule"], issue["path"],
                  issue.get("line", ""))
        print("History scan:", report["history_scan"])
        print("LIMIT: Not a release certification or Android runtime test.")
    history = report["history_scan"]
    failed_history = args.history and (not isinstance(history, dict) or
                                       history.get("status") != "NO_MATCHES")
    return 1 if report["findings"] or failed_history else 0


if __name__ == "__main__":
    raise SystemExit(main())
