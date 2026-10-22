#!/usr/bin/env python3
"""Compile selected unmodified production Kotlin policy sources, execute assertions.

This is deliberately narrower than :app:assembleDebug and cannot certify an APK.
It never accesses Android devices, accounts, song libraries, tokens, or network.
"""
from __future__ import annotations
import argparse
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
    "tools/kotlin/NW22AudioCoreSmoke.kt",
)
SUCCESS = "PASS: NW22 24 production Kotlin audio policy assertions"

def compile_and_test(kotlin: str | None, java: str | None, paths: list[Path]) -> tuple[int, str]:
    if not kotlin or not java:
        return 2, "UNAVAILABLE: JDK/kotlinc missing; cannot report any compiler PASS"
    missing = [str(path.relative_to(ROOT)) for path in paths if not path.is_file()]
    if missing:
        return 1, "FAIL: Missing exact project source: " + ", ".join(missing)
    try:
        with tempfile.TemporaryDirectory(prefix="nightwave-nw22-audio-") as td:
            jar = Path(td) / "audio-policy.jar"
            done = subprocess.run(
                [kotlin, *map(str, paths), "-include-runtime", "-d", str(jar)],
                capture_output=True, text=True, timeout=150
            )
            if done.returncode or not jar.is_file():
                return 1, "FAIL: Production Kotlin compile\n" + done.stderr[-4500:]
            executed = subprocess.run(
                [java, "-jar", str(jar)],
                capture_output=True, text=True, timeout=25
            )
            if executed.returncode or SUCCESS not in executed.stdout:
                return 1, ("FAIL: Production Kotlin behavior\n" +
                           (executed.stdout + executed.stderr)[-4500:])
            return 0, executed.stdout.strip()
    except (subprocess.TimeoutExpired, OSError) as error:
        return 1, "FAIL: audio compile/test process " + type(error).__name__

def self_test() -> None:
    assert len(SOURCES) == len(set(SOURCES)) == 5
    assert all(path.endswith(".kt") for path in SOURCES)
    code, result = compile_and_test(None, None, [])
    assert code == 2 and "cannot report" in result
    code, result = compile_and_test("kotlinc", "java", [ROOT / "missing" / "never.kt"])
    assert code == 1 and "Missing exact" in result
    assert "24 production Kotlin" in SUCCESS
    print("PASS: NW22 compiler runner fail-closed self-test")
    print("LIMIT: self-test does not compile production code")

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.list:
        print("\n".join(SOURCES))
        return 0
    if args.self_test:
        self_test()
        return 0
    code, msg = compile_and_test(
        shutil.which("kotlinc"), shutil.which("java"), [ROOT / p for p in SOURCES]
    )
    print(msg)
    return code

if __name__ == "__main__":
    raise SystemExit(main())
