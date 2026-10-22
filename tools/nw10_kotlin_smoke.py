#!/usr/bin/env python3
"""Compile real, Android-independent NightWave source and exercise its behavior.

This is not an APK build or Android UI test. Kotlin compiler and Java must exist.
Exit nonzero on missing tools, compile errors, runtime assertion failures, or timeouts.
"""
from pathlib import Path
import argparse
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
KOTLIN_SOURCE_FILES = (
    "app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt",
    "app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt",
    "app/src/main/java/com/nightwave/player/playback/PublicQueueSelectionPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/ShuffleModePolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/RepeatModePolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/SoundStudioPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/playback/PlaybackSeekIdentityPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekGesturePolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveArtworkPolicy.kt",
    "app/src/main/java/com/nightwave/player/v2/cloud/CloudAuthorizationRecoveryPolicy.kt",
    "tools/kotlin/NW10PureSmoke.kt",
)

def run_smoke() -> None:
    kotlinc = shutil.which("kotlinc")
    java = shutil.which("java")
    if not kotlinc or not java:
        raise RuntimeError("Kotlin compiler and Java are required (no silent SKIP).")
    paths = [ROOT / p for p in KOTLIN_SOURCE_FILES]
    for file in paths:
        if not file.is_file():
            raise RuntimeError(f"Missing Kotlin source: {file.relative_to(ROOT)}")
    with tempfile.TemporaryDirectory(prefix="nightwave-nw10-kotlin-") as temp:
        jar = Path(temp) / "nw10-smoke.jar"
        result = subprocess.run(
            [kotlinc, *map(str, paths), "-include-runtime", "-d", str(jar)],
            capture_output=True, text=True, timeout=150,
        )
        if result.returncode:
            raise RuntimeError("Kotlin compilation failed:\n" + result.stderr[-6000:])
        result = subprocess.run([java, "-jar", str(jar)],
                                capture_output=True, text=True, timeout=25)
        if result.returncode or "PASS: NW-10 compiled Kotlin smoke" not in result.stdout:
            raise RuntimeError("Kotlin behavior smoke failed:\n" +
                               result.stdout[-1000:] + result.stderr[-3500:])
        print(result.stdout.strip())
        print("LIMIT: This checks pure Kotlin policies, not the Android module or APK.")

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--list", action="store_true",
                        help="Show exact production sources included in the smoke")
    args = parser.parse_args()
    if args.list:
        for path in KOTLIN_SOURCE_FILES:
            print(path)
        return 0
    try:
        run_smoke()
        return 0
    except (RuntimeError, subprocess.TimeoutExpired) as exc:
        print(f"FAIL: NW-10 Kotlin smoke: {exc}")
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
