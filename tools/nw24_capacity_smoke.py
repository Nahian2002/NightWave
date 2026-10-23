#!/usr/bin/env python3
"""Compile/run the exact production NW24 capacity policy on a standalone JVM."""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = [
    ROOT / "app/src/main/java/com/nightwave/player/offline/OfflineLibraryCapacity.kt",
    ROOT / "tools/kotlin/NW24CapacitySmoke.kt",
]
PASS = "PASS: NW24 24 production Kotlin library capacity assertions"

def main() -> int:
    compiler, java = shutil.which("kotlinc"), shutil.which("java")
    if not compiler or not java:
        print("UNAVAILABLE: JVM/Kotlin compiler missing; no PASS claimed")
        return 2
    if not all(source.is_file() for source in SOURCES):
        print("FAIL: Missing exact production Kotlin source or fixture")
        return 1
    try:
        with tempfile.TemporaryDirectory(prefix="nightwave-nw24-") as td:
            jar = Path(td) / "capacity.jar"
            c = subprocess.run([compiler, *map(str, SOURCES),
                                "-include-runtime", "-d", str(jar)],
                               capture_output=True, text=True, timeout=130)
            if c.returncode or not jar.is_file():
                print("FAIL: NW24 Kotlin compile\n" + c.stderr[-4000:])
                return 1
            result = subprocess.run([java, "-jar", str(jar)],
                                    capture_output=True, text=True, timeout=20)
            if result.returncode or PASS not in result.stdout:
                print("FAIL: NW24 executable tests\n" +
                      (result.stdout + result.stderr)[-4000:])
                return 1
            print(result.stdout.strip())
            return 0
    except (OSError, subprocess.TimeoutExpired) as error:
        print("FAIL: NW24 compiler failed (" + type(error).__name__ + ")")
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
