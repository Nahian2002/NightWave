#!/usr/bin/env python3
"""NW105 original offline JUnit test executor (not full Android or public queue)."""
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

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (
    "app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt",
    "app/src/main/java/com/nightwave/player/offline/OfflineGaplessPolicy.kt",
    "tools/NW105OfflineRepeatAdapter.kt",
    "app/src/test/java/com/nightwave/player/offline/OfflineManualNextAvailabilityTest.kt",
    "app/src/test/java/com/nightwave/player/offline/OfflineShuffleCycleIntegrityTest.kt",
)
BRIDGE_ASSERT = """package org.junit;
public final class Assert {
 private Assert() {}
 public static void assertTrue(boolean x) { if (!x) throw new AssertionError("true expected"); }
 public static void assertFalse(boolean x) { if (x) throw new AssertionError("false expected"); }
 public static void assertNull(Object x) { if (x!=null) throw new AssertionError("null expected"); }
 public static void assertNotNull(Object x) { if (x==null) throw new AssertionError("non-null expected"); }
 public static void assertEquals(Object x,Object y) {
  if (!java.util.Objects.equals(x,y)) throw new AssertionError("Expected "+x+" got "+y);
 }
}"""
BRIDGE_TEST = """package org.junit;
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
public @interface Test {}"""

def invoke(argv: list[str], cwd: Path) -> str:
    result = subprocess.run(argv, cwd=cwd, text=True, errors="replace",
                            capture_output=True, timeout=180)
    if result.returncode:
        print((result.stdout + "\n" + result.stderr)[-7000:], file=sys.stderr)
        raise RuntimeError("NW105 subprocess failed: " + argv[0])
    return result.stdout

def hashes(paths: list[Path]) -> dict[str, str]:
    return {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in paths}

def receipt(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name + ".writing")
    temp.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n",
                    encoding="utf-8")
    os.replace(temp, path)

def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--self-test", action="store_true")
    p.add_argument("--report", default="build/nightwave-nw105-offline-junit.json")
    args = p.parse_args()
    target = ROOT / args.report
    if not args.self_test:
        try:
            receipt(target, {"status": "NOT_VERIFIED", "executed_tests": 0,
                             "android_compilation": "NOT_VERIFIED"})
        except OSError as e:
            print("NW105_BLOCKED unable to invalidate old result: "+str(e),
                  file=sys.stderr)
            return 2
    if any(shutil.which(cmd) is None for cmd in ("javac","java")) or (
            not args.self_test and shutil.which("kotlinc") is None):
        print("NW105_BLOCKED missing JDK/kotlinc", file=sys.stderr)
        return 2
    try:
        with tempfile.TemporaryDirectory(prefix="nw105-real-offline-") as d:
            tmp = Path(d)
            package = tmp / "org/junit"
            package.mkdir(parents=True)
            (package / "Assert.java").write_text(BRIDGE_ASSERT, encoding="utf-8")
            (package / "Test.java").write_text(BRIDGE_TEST, encoding="utf-8")
            classes = tmp / "classes"
            classes.mkdir()
            invoke(["javac","-d",str(classes),str(package/"Assert.java"),
                    str(package/"Test.java")],tmp)
            if args.self_test:
                print("NW105_JUNIT_BRIDGE_SELFTEST_PASS")
                return 0
            paths = [ROOT / name for name in SOURCE]
            if any(not x.is_file() for x in paths):
                raise RuntimeError("NW105 offline production/test files missing")
            for path, expected in zip(paths[-2:], (6, 8)):
                code = path.read_text(encoding="utf-8")
                if len(re.findall(r"@Test\b",code)) != expected or "import org.junit" not in code:
                    raise RuntimeError("NW105 actual JUnit source count drift")
            before = hashes(paths)
            runner = ROOT / "tools/NW105OriginalOfflineJUnitRunner.kt"
            if not runner.is_file():
                raise RuntimeError("NW105 real test runner missing")
            jar = tmp / "offline-tests.jar"
            invoke(["kotlinc","-classpath",str(classes),
                    *[str(x) for x in paths],str(runner),
                    "-include-runtime","-d",str(jar)],tmp)
            result = invoke(["java","-classpath",str(jar)+os.pathsep+str(classes),
                             "NW105OriginalOfflineJUnitRunnerKt"],tmp)
            if "NW105_REAL_OFFLINE_JUNIT_SOURCE_PASS cases=14" not in result:
                raise RuntimeError("NW105 completed-case receipt missing")
            if hashes(paths) != before:
                raise RuntimeError("NW105 source changed during compiler/tests")
            receipt(target,{"status":"OFFLINE_JVM_ONLY_PASS","test_cases":14,
                "real_offline_policies":2,
                "public_playback_queue":"NOT_TESTED_ENUM_ADAPTER_ONLY",
                "official_gradle_junit":"NOT_RUN",
                "android_compilation":"NOT_VERIFIED",
                "source_sha256":before})
            print(result.rstrip())
            print("NW105_SOURCE_HASH_RECEIPT "+str(target))
            return 0
    except (OSError,RuntimeError,subprocess.TimeoutExpired) as error:
        print("NW105_JUNIT_GATE_FAIL: "+str(error),file=sys.stderr)
        return 2
if __name__ == "__main__":
    raise SystemExit(main())
