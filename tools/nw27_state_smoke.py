#!/usr/bin/env python3
"""Recompile *actual* CloudBackupState with a test-only Android API stub."""
from __future__ import annotations

from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
FILES = (
    "app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupConsentPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupState.kt",
    "tools/kotlin/NW27StateAndroidStub.kt",
    "tools/kotlin/NW27StateRaceSmoke.kt",
)
SUCCESS = "PASS: NW27 exact-source CloudBackupState reconnect/privacy race smoke"


def main() -> int:
    kotlin = shutil.which("kotlinc")
    java = shutil.which("java")
    if kotlin is None or java is None:
        print("UNAVAILABLE: Kotlin compiler or Java missing; no PASS claimed")
        return 2
    sources = [ROOT / path for path in FILES]
    if not all(path.is_file() for path in sources):
        print("FAIL: incomplete NW27 state-race smoke sources")
        return 1
    try:
        with tempfile.TemporaryDirectory(prefix="nw27-state-") as temp:
            jar = Path(temp) / "state.jar"
            compiled = subprocess.run(
                [kotlin, *map(str, sources), "-include-runtime", "-d", str(jar)],
                capture_output=True, text=True, timeout=150,
            )
            if compiled.returncode != 0 or not jar.is_file():
                print("FAIL: exact-source CloudBackupState did not compile")
                print(compiled.stderr[-4000:])
                return 1
            executed = subprocess.run(
                [java, "-jar", str(jar)],
                capture_output=True, text=True, timeout=30,
            )
            if executed.returncode != 0 or SUCCESS not in executed.stdout:
                print("FAIL: CloudBackupState race smoke")
                print((executed.stdout + executed.stderr)[-4000:])
                return 1
            print(executed.stdout.strip())
            return 0
    except (OSError, subprocess.TimeoutExpired) as error:
        print("FAIL: state smoke " + type(error).__name__)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
