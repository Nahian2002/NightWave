#!/usr/bin/env python3
"""Compile actual NW23 Kotlin queue identity code with production audio policies."""
from __future__ import annotations
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = (
    "app/src/main/java/com/nightwave/player/offline/AudioFrameSignature.kt",
    "app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadPolicy.kt",
    "app/src/main/java/com/nightwave/player/offline/FreeAudioCatalogPolicy.kt",
    "app/src/main/java/com/nightwave/player/offline/FreeAudioIntegrity.kt",
    "app/src/main/java/com/nightwave/player/offline/CatalogWorkIdentity.kt",
    "tools/kotlin/NW23QueueSmoke.kt",
)
SUCCESS = "PASS: NW23 12 production Kotlin queue identity assertions"

def main() -> int:
    kotlinc = shutil.which("kotlinc")
    java = shutil.which("java")
    if not (kotlinc and java):
        print("UNAVAILABLE: a standalone Kotlin compiler and Java are required")
        return 2
    sources = [ROOT / path for path in SOURCES]
    if not all(p.is_file() for p in sources):
        print("FAIL: required NW23 production source file missing from complete ZIP")
        return 1
    try:
        with tempfile.TemporaryDirectory(prefix="nw23-source-compile-") as temp:
            jar = Path(temp) / "nw23.jar"
            compiled = subprocess.run([kotlinc, *map(str, sources),
                                       "-include-runtime", "-d", str(jar)],
                                      capture_output=True, text=True, timeout=150)
            if compiled.returncode or not jar.is_file():
                print("FAIL: NW23 production Kotlin compiler")
                print(compiled.stderr[-4000:])
                return 1
            tested = subprocess.run([java, "-jar", str(jar)],
                                    capture_output=True, text=True, timeout=25)
            if tested.returncode or SUCCESS not in tested.stdout:
                print("FAIL: NW23 production Kotlin assertions")
                print((tested.stdout + tested.stderr)[-4000:])
                return 1
            print(tested.stdout.strip())
            print("LIMIT: WorkManager runtime and Android APK not compiled")
            return 0
    except (subprocess.TimeoutExpired, OSError) as error:
        print("FAIL: compiler execution: " + type(error).__name__)
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
