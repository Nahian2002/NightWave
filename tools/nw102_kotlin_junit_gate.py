#!/usr/bin/env python3
"""NW102: compile and execute original policy @Test sources without Android SDK.

The temporary Java JUnit-API bridge is NOT the actual JUnit/Gradle runtime and
never enters the application source. Official Android/JUnit tests are separate.
"""
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
PRODUCTION = (
    "app/src/main/java/com/nightwave/player/playback/ShuffleRecoveryPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt",
    "app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt",
    "app/src/main/java/com/nightwave/player/offline/OfflineGaplessPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/AudioRouteTransportPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/EqualizerSelectionPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/PlaybackResolutionPolicy.kt",
    "app/src/main/java/com/nightwave/player/widget/WidgetPlaybackIntentPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/MediaSessionQueueControlPolicy.kt",
)
TESTS = (
    ("app/src/test/java/com/nightwave/player/playback/AudioRouteTransportPolicyTest.kt",
     "com.nightwave.player.playback.AudioRouteTransportPolicyTest", 6),
    ("app/src/test/java/com/nightwave/player/playback/EqualizerSelectionPolicyTest.kt",
     "com.nightwave.player.playback.EqualizerSelectionPolicyTest", 6),
    ("app/src/test/java/com/nightwave/player/offline/OfflineShuffleCycleIntegrityTest.kt",
     "com.nightwave.player.offline.OfflineShuffleCycleIntegrityTest", 8),
    ("app/src/test/java/com/nightwave/player/playback/PlaybackResolutionPolicyTest.kt",
     "com.nightwave.player.playback.PlaybackResolutionPolicyTest", 8),
    ("app/src/test/java/com/nightwave/player/widget/WidgetPlaybackIntentPolicyTest.kt",
     "com.nightwave.player.widget.WidgetPlaybackIntentPolicyTest", 5),
    ("app/src/test/java/com/nightwave/player/playback/MediaSessionQueueControlPolicyTest.kt",
     "com.nightwave.player.playback.MediaSessionQueueControlPolicyTest", 8),
    ("app/src/test/java/com/nightwave/player/playback/PlaybackQueueManualNextAvailabilityTest.kt",
     "com.nightwave.player.playback.PlaybackQueueManualNextAvailabilityTest", 6),
    ("app/src/test/java/com/nightwave/player/offline/OfflineManualNextAvailabilityTest.kt",
     "com.nightwave.player.offline.OfflineManualNextAvailabilityTest", 6),
)
TOTAL = sum(item[2] for item in TESTS)
assert TOTAL == 53

ASSERT_JAVA = """
package org.junit;
public final class Assert {
    private Assert() {}
    public static void assertTrue(boolean v) {
        if (!v) throw new AssertionError("Expected true");
    }
    public static void assertFalse(boolean v) {
        if (v) throw new AssertionError("Expected false");
    }
    public static void assertNull(Object v) {
        if (v != null) throw new AssertionError("Expected null");
    }
    public static void assertNotNull(Object v) {
        if (v == null) throw new AssertionError("Expected non-null");
    }
    public static void assertEquals(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual))
            throw new AssertionError("Expected " + expected + ", actual " + actual);
    }
}
"""
TEST_JAVA = """
package org.junit;
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
public @interface Test {}
"""
NEGATIVE_JAVA = """
public final class NW102NegativeControl {
  public static void main(String[] args) {
    boolean failed = false;
    try { org.junit.Assert.assertTrue(false); }
    catch (AssertionError expected) { failed = true; }
    if (!failed) throw new IllegalStateException("False assertion was accepted");
    org.junit.Assert.assertTrue(true);
    org.junit.Assert.assertEquals("same", "same");
    org.junit.Assert.assertNotNull("value");
    org.junit.Assert.assertNull(null);
    org.junit.Assert.assertFalse(false);
    System.out.println("NW102_NEGATIVE_ASSERTION_CONTROL_PASS");
  }
}
"""
RUNNER = """
import org.junit.Test
import java.lang.reflect.InvocationTargetException

fun main() {
    val suites = listOf(
__SUITES__
    )
    var executed = 0
    for ((name, expected) in suites) {
        val clazz = Class.forName(name)
        val methods = clazz.declaredMethods.filter {
            it.isAnnotationPresent(Test::class.java)
        }.sortedBy { it.name }
        check(methods.size == expected) {
            "NW102 method coverage drift " + name + " expected " + expected +
                " found " + methods.size
        }
        val instance = clazz.getDeclaredConstructor().newInstance()
        for (method in methods) {
            try {
                method.invoke(instance)
                executed++
            } catch (e: InvocationTargetException) {
                System.err.println("NW102_CASE_FAIL " + name + "." + method.name)
                throw (e.cause ?: e)
            }
        }
        println("NW102_SUITE_PASS name=" + name + " count=" + methods.size)
    }
    check(executed == 53)
    println("NW102_REAL_TEST_SOURCES_PASS cases=" + executed)
}
"""

def execute(args: list[str], cwd: Path) -> str:
    result = subprocess.run(args, cwd=cwd, capture_output=True,
                            text=True, errors="replace", timeout=240)
    if result.returncode:
        print((result.stdout + "\n" + result.stderr)[-10000:], file=sys.stderr)
        raise RuntimeError("NW102 tool returned nonzero: " + Path(args[0]).name)
    return result.stdout

def compile_bridge(tmp: Path) -> Path:
    src = tmp / "org" / "junit"
    src.mkdir(parents=True)
    (src / "Assert.java").write_text(ASSERT_JAVA, encoding="utf-8")
    (src / "Test.java").write_text(TEST_JAVA, encoding="utf-8")
    classes = tmp / "classes"
    classes.mkdir()
    execute(["javac", "-d", str(classes),
             str(src / "Assert.java"), str(src / "Test.java")], tmp)
    control = tmp / "NW102NegativeControl.java"
    control.write_text(NEGATIVE_JAVA, encoding="utf-8")
    execute(["javac", "-classpath", str(classes), "-d", str(classes),
             str(control)], tmp)
    result = execute(["java", "-classpath", str(classes),
                      "NW102NegativeControl"], tmp)
    if "NW102_NEGATIVE_ASSERTION_CONTROL_PASS" not in result:
        raise RuntimeError("NW102 failed assertion negative control")
    return classes

def exact_sources() -> list[Path]:
    sources = [ROOT / path for path in PRODUCTION]
    sources += [ROOT / row[0] for row in TESTS]
    for path in sources:
        if not path.is_file():
            raise RuntimeError("NW102 missing real source: " + str(path.relative_to(ROOT)))
    for rel, _, count in TESTS:
        text = (ROOT / rel).read_text(encoding="utf-8")
        if len(re.findall(r"@Test\b", text)) != count or "import org.junit" not in text:
            raise RuntimeError("NW102 missing/modified real JUnit cases: " + rel)
    return sources

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--report", type=Path,
                        default=Path("build/nightwave-nw102-jvm-source-tests.json"))
    args = parser.parse_args()
    if not shutil.which("javac") or not shutil.which("java"):
        print("NW102_BLOCKED missing JDK tools", file=sys.stderr)
        return 2
    if not args.self_test and not shutil.which("kotlinc"):
        print("NW102_BLOCKED missing kotlinc (never mark SKIP as PASS)",
              file=sys.stderr)
        return 2
    try:
        with tempfile.TemporaryDirectory(prefix="nightwave-nw102-") as name:
            tmp = Path(name)
            classes = compile_bridge(tmp)
            if args.self_test:
                print("NW102_ASSERTION_BRIDGE_SELFTEST_PASS")
                return 0
            source_files = exact_sources()
            entries = ",\n".join(
                '        Pair("' + class_name + '", ' + str(count) + ")"
                for _, class_name, count in TESTS
            )
            runner = tmp / "NW102TestSourceRunner.kt"
            runner.write_text(RUNNER.replace("__SUITES__", entries), encoding="utf-8")
            jar = tmp / "real-policy-tests.jar"
            execute(["kotlinc", "-classpath", str(classes),
                     *[str(path) for path in source_files], str(runner),
                     "-include-runtime", "-d", str(jar)], tmp)
            output = execute(["java", "-classpath",
                              os.pathsep.join((str(classes), str(jar))),
                              "NW102TestSourceRunnerKt"], tmp)
            if not re.search(r"(?m)^NW102_REAL_TEST_SOURCES_PASS cases=53$", output):
                raise RuntimeError("NW102 test completion marker missing")
            report = {
                "status": "POLICY_JVM_TESTS_PASS_ONLY",
                "executed_tests": TOTAL,
                "test_classes": len(TESTS),
                "source_file_count": len(source_files),
                "source_sha256": {
                    str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
                    for path in source_files
                },
                "android_compilation": "NOT_VERIFIED",
                "official_gradle_junit": "NOT_RUN",
                "device_instrumentation": "NOT_RUN",
            }
            target = args.report if args.report.is_absolute() else ROOT / args.report
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n",
                              encoding="utf-8")
            print(output.rstrip())
            print("NW102_SOURCE_HASH_REPORT " + str(target))
            return 0
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as ex:
        print("NW102_COMPILER_OR_TEST_FAIL: " + str(ex), file=sys.stderr)
        return 2

if __name__ == "__main__":
    raise SystemExit(main())
