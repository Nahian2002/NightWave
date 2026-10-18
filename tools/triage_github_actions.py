#!/usr/bin/env python3
"""Diagnose whether GitHub Actions reached a runner or compiler.

Offline and read-only. Accepts exported JSON from the repository's GitHub
Actions jobs API. Never authenticates, fetches, logs tokens, modifies a repo,
or treats an unstarted job as a Kotlin/Gradle failure.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import tempfile


def classify(payload: dict) -> dict:
    jobs = payload.get("jobs")
    if not isinstance(jobs, list):
        return {"status": "INVALID", "reason": "No valid jobs array"}
    if not jobs:
        return {"status": "NO_JOBS", "reason": "No job records; execution not proven"}

    latest: dict[str, dict] = {}
    for entry in jobs:
        if not isinstance(entry, dict):
            continue
        name = entry.get("name", "unnamed")
        if not isinstance(name, str) or len(name) > 150:
            name = "unnamed"
        attempt = entry.get("run_attempt", 0)
        if not isinstance(attempt, int):
            attempt = 0
        if name not in latest or attempt >= latest[name].get("run_attempt", 0):
            latest[name] = entry
    if not latest:
        return {"status": "INVALID", "reason": "No valid job objects"}

    all_unstarted = True
    categories: list[str] = []
    for job in latest.values():
        runner = job.get("runner_id")
        steps = job.get("steps")
        if not isinstance(steps, list):
            steps = []
        status = job.get("status", "unknown")
        conclusion = job.get("conclusion", "")
        has_runner = isinstance(runner, int) and runner > 0
        has_steps = bool(steps)
        if has_runner or has_steps:
            all_unstarted = False
        # No steps + runner_id 0 is authoritative about missing build
        # execution, but NOT a diagnosis of billing, quotas or policies.
        if (runner in (0, None) and not has_steps and
                status == "completed" and conclusion == "failure"):
            categories.append("RUNNER_UNASSIGNED")
        elif has_runner or has_steps:
            categories.append("RUNNER_STARTED")
        else:
            categories.append("PENDING_OR_UNKNOWN")

    if all_unstarted and categories and all(x == "RUNNER_UNASSIGNED" for x in categories):
        return {
            "status": "RUNNER_UNASSIGNED",
            "reason": "Every failed job finished without an assigned runner or steps.",
            "compilation": "NOT_ATTEMPTED",
            "next": "Check GitHub Actions run banner, account billing/spending limits, repository Actions policies, and runner availability."
        }
    if "RUNNER_STARTED" in categories:
        return {
            "status": "RUNNER_STARTED",
            "reason": "At least one job reached a runner or recorded execution steps.",
            "compilation": "INSPECT_STEPS",
            "next": "Read the first failed step and sanitized Gradle/compile summary; do not infer the cause from run conclusion alone."
        }
    return {
        "status": "PENDING_OR_UNKNOWN",
        "reason": "Job states do not establish build execution.",
        "compilation": "UNKNOWN",
        "next": "Recheck job steps and runner assignment later."
    }


def self_test() -> None:
    def job(runner: int | None, steps: list, status="completed", conclusion="failure"):
        return {"name": "Compile, JVM tests and Android test-source check",
                "run_attempt": 1, "runner_id": runner, "steps": steps,
                "status": status, "conclusion": conclusion}
    assert classify({"jobs": [job(0, [])]})["status"] == "RUNNER_UNASSIGNED"
    assert classify({"jobs": [job(None, [])]})["compilation"] == "NOT_ATTEMPTED"
    assert classify({"jobs": [job(123, [{"name": "Compile", "conclusion": "failure"}])]})[
        "status"] == "RUNNER_STARTED"
    assert classify({"jobs": [job(0, [{"name": "Checkout", "conclusion": "failure"}])]})[
        "status"] == "RUNNER_STARTED"
    assert classify({"jobs": [job(0, [], "queued", None)])["status"] == "PENDING_OR_UNKNOWN"
    assert classify({"jobs": []})["status"] == "NO_JOBS"
    assert classify({})["status"] == "INVALID"
    with tempfile.TemporaryDirectory(prefix="nw-ci-triage-") as folder:
        f = Path(folder) / "jobs.json"
        f.write_text(json.dumps({"jobs": [job(0, [])]}), encoding="utf-8")
        assert classify(json.loads(f.read_text(encoding="utf-8")))["status"] == "RUNNER_UNASSIGNED"
    print("PASS: CI triage separates unassigned runners from executed compile failures")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--jobs-json", type=Path, help="Local exported jobs API JSON file")
    args = ap.parse_args()
    if args.self_test:
        self_test()
        return 0
    if not args.jobs_json:
        ap.error("Use --jobs-json PATH, or --self-test")
    try:
        data = json.loads(args.jobs_json.read_text(encoding="utf-8"))
        report = classify(data)
    except (OSError, ValueError) as ex:
        print("FAIL: cannot read a valid jobs JSON file (" + type(ex).__name__ + ")")
        return 2
    for key in ("status", "reason", "compilation", "next"):
        if key in report:
            print(key.upper() + ": " + report[key])
    return 2 if report["status"] == "INVALID" else 0


if __name__ == "__main__":
    raise SystemExit(main())
