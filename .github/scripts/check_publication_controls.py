"""Repository-only publication controls. Never installs, builds or edits the app."""
from pathlib import Path
import argparse
import copy
import json
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = "fb8f0455c6903249ab85299a76566153a2b8d2a7"
PRIVATE = "github.event.repository.private == true && github.event_name != 'pull_request'"
UPLOAD_CONDITIONS = {"${{ " + status + " && " + PRIVATE + " }}" for status in ("always()", "success()", "failure()")}


def verify(workflows, pins, ignores, reviews):
    known = {row["action"]: row["commit"] for row in pins["actions"]}
    assert len(known) == 6 and all(re.fullmatch(r"[a-f0-9]{40}", value) for value in known.values())
    upload_count = 0
    for name, body in workflows.items():
        assert not re.search(r"(?m)^\s*(?:pull_request_target|workflow_run):", body), name + ": privileged trigger needs separate review"
        assert not re.search(r"(?m)^\s*(?:permissions:\s*write-all|[\w-]+:\s*write)\s*$", body), name + ": write permission"
        assert "secrets." not in body, name + ": credential-bearing workflow needs separate review"
        assert re.search(r"(?m)^permissions:\s*(?:\{\}|\n\s+contents: read)\s*$", body), name + ": missing explicit token boundary"
        blocks = re.split(r"(?m)(?=^      - )", body)
        for block in blocks:
            use = re.search(r"(?m)^ {6,8}(?:- )?uses:\s*([^#\n]+)", block)
            if not use:
                continue
            value = use.group(1).strip()
            match = re.fullmatch(r"([\w.-]+/[\w.-]+)(/[\w./-]+)?@([a-f0-9]{40})", value)
            assert match and known.get(match.group(1)) == match.group(3), name + ": unapproved action revision"
            if match.group(1) == "actions/checkout":
                assert "persist-credentials: false" in block, name + ": checkout credentials retained"
            if match.group(1) == "actions/upload-artifact":
                upload_count += 1
                condition = re.search(r"(?m)^        if:\s*(.+)$", block)
                assert condition and condition.group(1).strip() in UPLOAD_CONDITIONS, name + ": public/PR artifact upload"
                assert not re.search(r"(?m)^\s+path:\s*(?:\.|\*\*|~/|/home|C:)", block), name + ": broad artifact selection"
    assert upload_count >= 2, "Existing private artifact verification paths unexpectedly disappeared"
    fingerprints = {r["fingerprint"] for r in reviews["matches"]}
    assert len(fingerprints) == 13
    assert all(r["disposition"] == "FALSE_POSITIVE" and r["reason"] for r in reviews["matches"])
    assert set(ignores) == fingerprints and len(ignores) == len(fingerprints), "Only the exact reviewed immutable matches may be ignored"
    for row in reviews["matches"]:
        assert row["fingerprint"] == f'{row["commit"]}:{row["file"]}:{row["rule"]}:{row["line"]}'
        assert re.fullmatch(r"[a-f0-9]{40}", row["commit"]) and row["line"] > 0
    publication = workflows["source-publication.yml"]
    for marker in ("--ignore-gitleaks-allow", "--log-opts='--all --full-history'", "fetch-depth: 0",
                   "551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb"):
        assert marker in publication, "Missing scanner control: " + marker


def inputs():
    workflows = {p.name: p.read_text(encoding="utf-8") for p in (ROOT / ".github/workflows").glob("*.yml")}
    pins = json.loads((ROOT / ".github/security/actions-lock.json").read_text(encoding="utf-8"))
    reviews = json.loads((ROOT / ".github/security/gitleaks-reviewed-matches.json").read_text(encoding="utf-8"))
    ignores = [line.strip() for line in (ROOT / ".gitleaksignore").read_text(encoding="utf-8").splitlines() if line.strip() and not line.startswith("#")]
    return workflows, pins, ignores, reviews


def app_unchanged(parent=BASE):
    protected = ["app/", "tools/", "gradle/", "build.gradle.kts", "settings.gradle.kts",
                 "gradle.properties", "build-windows.cmd", "gradlew", "gradlew.bat", ".gitattributes"]
    changed = subprocess.check_output(["git", "diff", "--name-only", parent, "--", *protected], cwd=ROOT, text=True)
    assert not changed.strip(), "Protected app/build inputs changed: " + changed
    added = subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard", "--", *protected], cwd=ROOT, text=True)
    assert not added.strip(), "Untracked app/build additions: " + added


def self_test(data):
    workflows, pins, ignores, reviews = data
    verify(*data)
    for old, new in ((PRIVATE, "true"), ("persist-credentials: false", "persist-credentials: true"),
                     ("permissions:\n  contents: read", "permissions:\n  contents: write"),
                     ("pull_request:", "pull_request_target:")):
        broken = dict(workflows)
        assert old in broken["current-android.yml"]
        broken["current-android.yml"] = broken["current-android.yml"].replace(old, new)
        try:
            verify(broken, pins, ignores, reviews)
        except AssertionError:
            continue
        raise AssertionError("Accepted unsafe workflow: " + old)
    changed_pins = copy.deepcopy(pins); changed_pins["actions"][0]["commit"] = "0" * 40
    for sample in ((workflows, changed_pins, ignores, reviews), (workflows, pins, ignores + ["*"], reviews)):
        try:
            verify(*sample)
        except AssertionError:
            continue
        raise AssertionError("Accepted unreviewed action or broad secret exclusion")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--verify-app-unchanged", action="store_true")
    parser.add_argument("--protected-parent", choices=(BASE, "736f014e6ff38d73f29e014057f3c398a83a63d3"), default=BASE)
    args = parser.parse_args(); data = inputs()
    if args.self_test:
        self_test(data)
    else:
        verify(*data)
    if args.verify_app_unchanged:
        app_unchanged(args.protected_parent)
    print("PASS repository publication controls; no public APK upload, approved action pins and exact secret-match exclusions. App/security/rights certification is separate.")
