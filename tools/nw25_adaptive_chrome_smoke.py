#!/usr/bin/env python3
"""Compile actual NightWave UI sizing policies outside Android as pure Kotlin."""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = (
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveChromePolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveAdaptiveChromePolicy.kt",
    "tools/kotlin/NW25AdaptiveChromeSmoke.kt",
)
PASS = "PASS: NW25 28 exact-production Kotlin adaptive chrome assertions"

def main() -> int:
    kotlin = shutil.which("kotlinc")
    java = shutil.which("java")
    if not kotlin or not java:
        print("UNAVAILABLE: kotlinc/Java missing; no tests reported passed")
        return 2
    files = [ROOT / path for path in SOURCES]
    if not all(f.is_file() for f in files):
        print("FAIL: exact NW25 production sources missing")
        return 1
    try:
        with tempfile.TemporaryDirectory(prefix="nw25-sizes-") as dirname:
            jar = Path(dirname) / "chrome.jar"
            c = subprocess.run([kotlin, *map(str, files), "-include-runtime", "-d", str(jar)],
                               capture_output=True, text=True, timeout=150)
            if c.returncode or not jar.is_file():
                print("FAIL: NW25 production Kotlin compile\n" + c.stderr[-4000:])
                return 1
            out = subprocess.run([java, "-jar", str(jar)], capture_output=True,
                                 text=True, timeout=30)
            if out.returncode or PASS not in out.stdout:
                print("FAIL: NW25 runtime checks\n" +
                      (out.stdout + out.stderr)[-4000:])
                return 1
            print(out.stdout.strip())
            return 0
    except (OSError, subprocess.TimeoutExpired) as error:
        print("FAIL: NW25 Kotlin execution " + type(error).__name__)
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
