#!/usr/bin/env python3
"""Compile exact production NW27 Kotlin policies; no Android or account access."""
from __future__ import annotations
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
FILES = (
    "app/src/main/java/com/nightwave/player/diagnostics/NightWaveCrashPrivacyPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/cloud/DriveResponseReadPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupConsentPolicy.kt",
    "tools/kotlin/NW27HardeningSmoke.kt",
)
SUCCESS = "PASS: NW27 33 exact-source Kotlin safety assertions"

def main() -> int:
    kotlin = shutil.which("kotlinc")
    java = shutil.which("java")
    if kotlin is None or java is None:
        print("UNAVAILABLE: Kotlin compiler or Java missing; no test PASS claimed")
        return 2
    files = [ROOT / path for path in FILES]
    if not all(file.is_file() for file in files):
        print("FAIL: NW27 full source and test fixture are not available")
        return 1
    try:
        with tempfile.TemporaryDirectory(prefix="nw27-policy-") as folder:
            jar = Path(folder) / "nw27.jar"
            compile_result = subprocess.run(
                [kotlin, *map(str, files), "-include-runtime", "-d", str(jar)],
                capture_output=True, text=True, timeout=150)
            if compile_result.returncode != 0 or not jar.is_file():
                print("FAIL: production Kotlin policy compilation")
                print(compile_result.stderr[-4000:])
                return 1
            runtime = subprocess.run([java, "-jar", str(jar)],
                                     capture_output=True, text=True, timeout=30)
            if runtime.returncode != 0 or SUCCESS not in runtime.stdout:
                print("FAIL: NW27 policy runtime verification")
                print((runtime.stdout + runtime.stderr)[-4000:])
                return 1
            print(runtime.stdout.strip())
            return 0
    except (OSError, subprocess.TimeoutExpired) as exc:
        print("FAIL: NW27 Kotlin runner " + type(exc).__name__)
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
