#!/usr/bin/env python3
"""Compile original production Kotlin policy files and execute NW26 assertions.

Standalone runner never accesses real Google accounts, network or Android data.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
FILES = (
    "app/src/main/java/com/nightwave/player/v2/cloud/DriveHttpFailurePolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupConsentPolicy.kt",
    "app/src/main/java/com/nightwave/player/offline/OfflineSearchRanking.kt",
    "tools/kotlin/NW26ReliabilitySmoke.kt",
)
PASS = "PASS: NW26 32 exact-production Kotlin Drive/search assertions"

def main() -> int:
    compiler, java = shutil.which("kotlinc"), shutil.which("java")
    if not compiler or not java:
        print("UNAVAILABLE: kotlinc or Java missing (no PASS claimed)")
        return 2
    sources = [ROOT / p for p in FILES]
    if not all(p.is_file() for p in sources):
        print("FAIL: NW26 exact-source files are missing")
        return 1
    try:
        with tempfile.TemporaryDirectory(prefix="nw26-code-audit-") as dirname:
            jar = Path(dirname) / "smoke.jar"
            built = subprocess.run(
                [compiler, *map(str, sources), "-include-runtime", "-d", str(jar)],
                capture_output=True, text=True, timeout=150)
            if built.returncode or not jar.is_file():
                print("FAIL: Kotlin production compiler\n" + built.stderr[-4000:])
                return 1
            ran = subprocess.run([java, "-jar", str(jar)], capture_output=True,
                                 text=True, timeout=30)
            if ran.returncode or PASS not in ran.stdout:
                print("FAIL: production policy runtime\n" +
                      (ran.stdout + ran.stderr)[-4000:])
                return 1
            print(ran.stdout.strip())
            return 0
    except (subprocess.TimeoutExpired, OSError) as exc:
        print("FAIL: compiler invocation " + type(exc).__name__)
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
