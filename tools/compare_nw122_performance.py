#!/usr/bin/env python3
"""Compare opt-in real Kotlin/host-JVM receipts without flaky speed thresholds."""
import argparse
import copy
import json
import math
from pathlib import Path
from statistics import median


def compare(before, after):
    if len(before) < 3 or len(after) < 3:
        raise ValueError("At least three fresh JVM processes per version are required")
    first = before[0]
    scenarios = [r["scenario"] for r in first["measurements"]]
    if len(scenarios) != 9 or len(set(scenarios)) != 9:
        raise ValueError("Expected nine distinct benchmark scenarios")
    identity = ("java", "vm", "os", "processors", "heapArguments", "warmupsPerScenario",
                "libraryRows", "harnessSha256", "outputSha256", "scope")
    for group in (before, after):
        for receipt in group:
            if any(receipt[k] != first[k] for k in identity):
                raise ValueError("Environment, harness, dataset or ordered output mismatch")
            if receipt["sourceSha256"] != group[0]["sourceSha256"]:
                raise ValueError("Source changed between measurement processes")
            if [r["scenario"] for r in receipt["measurements"]] != scenarios:
                raise ValueError("Scenario mismatch")
            for expected, row in zip(first["measurements"], receipt["measurements"]):
                if row["samples"] != 40 or row["resultCount"] != expected["resultCount"]:
                    raise ValueError("Sample or result count mismatch")
                if any(not math.isfinite(row[k]) or row[k] < 0 for k in ("medianMs", "p95Ms", "maxMs")):
                    raise ValueError("Invalid timing")
                if not row["medianMs"] <= row["p95Ms"] <= row["maxMs"]:
                    raise ValueError("Unordered timing percentiles")
    rows = []
    for index, scenario in enumerate(scenarios):
        row = {"scenario": scenario}
        for name, group in (("before", before), ("after", after)):
            p95 = [r["measurements"][index]["p95Ms"] for r in group]
            row[name] = {"medianOfProcessP95Ms": median(p95), "processP95RangeMs": [min(p95), max(p95)],
                         "medianOfProcessMedianMs": median([r["measurements"][index]["medianMs"] for r in group])}
        rows.append(row)
    return {"scope": first["scope"], "processes": {"before": len(before), "after": len(after)},
            "orderedOutputMatch": True, "harnessSha256": first["harnessSha256"],
            "outputSha256": first["outputSha256"], "measurements": rows,
            "interpretation": "Median of independent-process p95s, not pooled p95. No device/soak or speed-threshold verdict."}


def self_test():
    fixture = dict.fromkeys(("java", "vm", "os", "processors", "heapArguments", "warmupsPerScenario",
                            "libraryRows", "harnessSha256", "outputSha256", "scope"), "fixture")
    fixture["sourceSha256"] = {"file": "hash"}
    fixture["measurements"] = [dict(scenario=str(i), samples=40, resultCount=1, medianMs=1.0,
                                    p95Ms=2.0, maxMs=3.0) for i in range(9)]
    before = [copy.deepcopy(fixture) for _ in range(3)]
    assert compare(before, before)["orderedOutputMatch"]
    for key in ("java", "harnessSha256", "outputSha256", "sourceSha256"):
        bad = copy.deepcopy(before)
        bad[1][key] = "changed"
        try:
            compare(before, bad)
        except ValueError:
            pass
        else:
            raise AssertionError(key + " mismatch accepted")
    for field, value in (("p95Ms", float("nan")), ("medianMs", -1), ("samples", 39), ("resultCount", 2)):
        bad = copy.deepcopy(before)
        bad[1]["measurements"][0][field] = value
        try:
            compare(before, bad)
        except ValueError:
            pass
        else:
            raise AssertionError(field + " mismatch accepted")
    try:
        compare(before[:1], before)
    except ValueError:
        pass
    else:
        raise AssertionError("Insufficient processes accepted")
    print("PASS: comparison positive/negative fixtures")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--before", nargs="+", type=Path)
    parser.add_argument("--after", nargs="+", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        if not args.before or not args.after or not args.output:
            parser.error("--before, --after and --output are required")
        if args.output.resolve() in {p.resolve() for p in args.before + args.after}:
            parser.error("Output must be separate from raw measurement receipts")
        try:
            # Invalidate an older successful comparison before parsing any input.
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text('{"status":"NOT_VERIFIED"}\n', encoding="utf-8")
            report = compare([json.loads(p.read_text(encoding="utf-8")) for p in args.before],
                             [json.loads(p.read_text(encoding="utf-8")) for p in args.after])
            args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False, allow_nan=False) + "\n", encoding="utf-8")
            print("PASS: identical ordered outputs and comparable receipts; timing report written")
        except (OSError, ValueError, KeyError, TypeError) as error:
            parser.exit(1, "FAIL: " + str(error) + "\n")
