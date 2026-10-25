#!/usr/bin/env python3
"""Reproducible, non-Android NW40 Kotlin smoke for two pure production classes.

Use when kotlinc and Java are already provisioned. No network, music data,
temporary APK signing, or Gradle. It NEVER substitutes for the Android build.
"""
import argparse
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = [
    ROOT / "app/src/main/java/com/nightwave/player/v2/ui/SearchHistoryPolicy.kt",
    ROOT / "app/src/main/java/com/nightwave/player/v2/library/PlaylistReorderGesturePolicy.kt",
]
HARNESS = r'''
package com.nightwave.player.v2.ui
import com.nightwave.player.v2.library.PlaylistReorderGesturePolicy as Reorder

fun main() {
    val history = SearchHistoryPolicy
    check(history.normalize("  Blue    Moon  ") == "Blue Moon")
    check(history.normalize("\u0000bad") == null)
    check(history.record(emptyList(), "    ").isEmpty())
    check(history.record(listOf("Blue Moon", "Night"), "blue moon") ==
        listOf("blue moon", "Night"))
    check(history.remove(listOf("Blue Moon", "Night"), "BLUE MOON") == listOf("Night"))
    check((1..30).fold(emptyList<String>()) { prev, index ->
        history.record(prev, "Song $index")
    }.size == 12)
    check(history.normalize("a".repeat(200))!!.length == 160)
    check(Reorder.offsetToSteps(10f, 100) == 0)
    check(Reorder.offsetToSteps(80f, 100) == 1)
    check(Reorder.offsetToSteps(Float.NaN, 100) == 0)
    check(Reorder.target(0, 99, 3) == 2)
    check(Reorder.target(2, -99, 3) == 0)
    check(Reorder.target(0, 0, 3) == null)
    println("PASS: NW40 13 standalone Kotlin assertions (not Android Gradle)")
}
'''

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        assert HARNESS.count("    check(") == 11  # multiline checks count once
        assert len(SOURCES) == 2
        print("PASS: NW40 Kotlin smoke harness self-test (not compiled)")
        return 0
    for file in SOURCES:
        if not file.is_file():
            print("FAIL: missing Kotlin source", file.relative_to(ROOT))
            return 1
    compiler = shutil.which("kotlinc")
    java = shutil.which("java")
    if not compiler or not java:
        print("SKIP: kotlinc or java missing. Android Gradle compile remains required.")
        return 2
    with tempfile.TemporaryDirectory(prefix="nightwave-nw40-smoke-") as temp:
        out = Path(temp)
        harness = out / "NW40Smoke.kt"
        jar = out / "nw40-smoke.jar"
        harness.write_text(HARNESS, encoding="utf-8")
        for command in ([compiler, *map(str, SOURCES), str(harness),
                         "-include-runtime", "-d", str(jar)], [java, "-jar", str(jar)]):
            result = subprocess.run(command, cwd=ROOT, capture_output=True,
                                    text=True, timeout=120)
            if result.returncode:
                print("FAIL: NW40 Kotlin policy smoke")
                print((result.stdout + result.stderr)[-4000:])
                return result.returncode
            if result.stdout.strip():
                print(result.stdout.strip())
    print("Standalone classes compiled. NO claim of Android app compilation.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
