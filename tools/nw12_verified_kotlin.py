#!/usr/bin/env python3
"""Compile and execute six original NightWave JVM-only policies, fail closed.

Uses the source in this extracted repository, not an imitation of an Android
device or an emulator. Does not inspect tokens, install or sign APKs.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PATHS = (
    "app/src/main/java/com/nightwave/player/playback/ShuffleModePolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/playback/PlaybackSeekIdentityPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/cloud/CloudAuthorizationRecoveryPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekGesturePolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveArtworkPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/SoundStudioPolicy.kt",
    "tools/kotlin/NW12VerifiedSmoke.kt",
)

def main() -> int:
    kotlinc, java = shutil.which("kotlinc"), shutil.which("java")
    if not kotlinc or not java:
        print("UNAVAILABLE: JDK and standalone Kotlin compiler required; no pass claimed")
        return 2
    source_files = [ROOT / path for path in PATHS]
    for file in source_files:
        if not file.is_file():
            print("FAIL: source ZIP missing " + file.relative_to(ROOT).as_posix())
            return 1
    try:
        with tempfile.TemporaryDirectory(prefix="nw12-verified-policies-") as temp:
            jar = Path(temp) / "nw12-policies.jar"
            compiled = subprocess.run(
                [kotlinc, *map(str, source_files), "-include-runtime", "-d", str(jar)],
                capture_output=True, text=True, timeout=150,
            )
            if compiled.returncode:
                print("FAIL: Kotlin compiler returned nonzero\n" + compiled.stderr[-5000:])
                return 1
            tested = subprocess.run(
                [java, "-jar", str(jar)], capture_output=True, text=True, timeout=30
            )
            if tested.returncode or "28 Kotlin behavior assertions" not in tested.stdout:
                print("FAIL: Kotlin behavior assertions\n" +
                      (tested.stdout + tested.stderr)[-5000:])
                return 1
            print(tested.stdout.strip())
            print("LIMIT: no Compose, Media3, Android SDK, Gradle or APK tested")
            return 0
    except subprocess.TimeoutExpired as exc:
        print(f"FAIL: compiler/runtime timeout at {exc.cmd}")
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
