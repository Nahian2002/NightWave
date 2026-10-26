#!/usr/bin/env python3
"""NW107 independent JVM gate for the ORIGINAL LyricsReadingPolicyTest source."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

import nw102_kotlin_junit_gate as junit

ROOT = Path(__file__).resolve().parents[1]
SOURCES = (
    "app/src/main/java/com/nightwave/player/v2/lyrics/TimedLyrics.kt",
    "app/src/main/java/com/nightwave/player/v2/lyrics/LyricsSyncPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/lyrics/LyricsReadingPolicy.kt",
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsReadingPolicyTest.kt",
)

def fingerprints(paths: list[Path]) -> dict[str, str]:
    return {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in paths}

def receipt(target: Path, content: dict) -> None:
    junit.write_atomic_receipt(target, content)

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path,
                        default=Path("build/nightwave-nw107-lyrics-junit.json"))
    args = parser.parse_args()
    target = args.report if args.report.is_absolute() else ROOT / args.report
    try:
        receipt(target, {"status": "NOT_VERIFIED", "methods": 0,
                         "android_compilation": "NOT_VERIFIED",
                         "official_gradle_junit": "NOT_RUN"})
    except OSError as exc:
        print("NW107_FAIL unable to invalidate old report: " + str(exc), file=sys.stderr)
        return 2
    if any(shutil.which(tool) is None for tool in ("java", "javac", "kotlinc")):
        print("NW107_BLOCKED missing javac/java/kotlinc", file=sys.stderr)
        return 2
    try:
        paths = [ROOT / relative for relative in SOURCES]
        if any(not p.is_file() for p in paths):
            raise RuntimeError("NW107 exact production/test source missing")
        test = paths[-1].read_text(encoding="utf-8")
        if len(re.findall(r"@Test\b", test)) != 8 or "import org.junit" not in test:
            raise RuntimeError("NW107 original JUnit test count changed")
        prior = fingerprints(paths)
        with tempfile.TemporaryDirectory(prefix="nightwave-nw107-") as location:
            tmp = Path(location)
            classes = junit.compile_bridge(tmp)
            runner = ROOT / "tools/NW107OriginalLyricsJUnitRunner.kt"
            jar = tmp / "lyrics-junit.jar"
            junit.execute(["kotlinc", "-classpath", str(classes),
                           *map(str, paths), str(runner),
                           "-include-runtime", "-d", str(jar)], tmp)
            output = junit.execute(["java", "-classpath",
                                    os.pathsep.join((str(jar), str(classes))),
                                    "NW107OriginalLyricsJUnitRunnerKt"], tmp)
            if "NW107_ORIGINAL_LYRICS_JUNIT_PASS methods=8" not in output:
                raise RuntimeError("NW107 expected eight original JUnit cases missing")
            if fingerprints(paths) != prior:
                raise RuntimeError("NW107 source changed during execution")
            receipt(target, {"status": "LYRICS_POLICY_JVM_PASS_ONLY",
                             "methods": 8, "input_sha256": prior,
                             "android_compilation": "NOT_VERIFIED",
                             "official_gradle_junit": "NOT_RUN",
                             "device_talkback": "NOT_RUN"})
            print(output.rstrip())
            print("NW107_EXACT_SOURCE_JVM_RECEIPT " + str(target))
            return 0
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as exc:
        print("NW107_JUNIT_GATE_FAIL " + str(exc), file=sys.stderr)
        return 2

if __name__ == "__main__":
    raise SystemExit(main())
