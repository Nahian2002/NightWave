#!/usr/bin/env python3
"""Read-only NW22 source integration gate. Never claims Android build PASS."""
from __future__ import annotations
import argparse
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

def checks(files: dict[str, str]) -> list[str]:
    findings = []
    def need(condition: bool, reason: str) -> None:
        if not condition:
            findings.append(reason)

    verifier = files["verifier"]
    windows = files["windows"]
    workflow = files["workflow"]
    runner = files["runner"]
    smoke = files["smoke"]
    gradle = files["gradle"]
    need(verifier.count("NightWave APK verification PASSED") == 1,
         "Exactly one completed APK verification success path required")
    need(verifier.count("function Read-Signer") == 1,
         "Exactly one signature-reader function required")
    need(verifier.count("} catch {") == 1,
         "Exactly one terminal signature-verifier catch required")
    need("^[0-9A-Fa-f]{64}$" in verifier,
         "Trusted signer must be validated as exactly 64 hex digits")
    need("if ($newSigner -ne $trusted.ToUpperInvariant())" in verifier,
         "Trusted signer mismatch must block APK publication")
    need("if ($previousSigner -ne $newSigner)" in verifier,
         "Previous APK signer continuity must remain enforced")
    need("-TrustedSignerSHA256 $env:NIGHTWAVE_TRUSTED_SIGNER_SHA256" in windows,
         "Windows builder must pass optional trusted certificate fingerprint")
    need("app\\build\\outputs\\apk\\debug\\app-debug.apk" in windows,
         "Only freshly built APK may be promoted")
    need("Source files changed during Gradle build" in windows,
         "Post-build source mutation gate must remain")
    need(":app:compileDebugAndroidTestKotlin" in windows,
         "Instrumented Android test sources must compile")
    need("fix/nightwave-v2-nw22-build-stability" in workflow,
         "NW22 must be included in CI branch filter")
    need("python3 tools/nw22_audio_core_smoke.py" in workflow,
         "Production Kotlin smoke must run in CI when compiler present")
    need("python3 tools/verify_nw22_build_readiness.py" in workflow,
         "Self-integrity must run in CI")
    need(len(re.findall(r"NW22AudioCoreSmoke", smoke)) >= 1,
         "NW22 behavior fixture is missing")
    need('"-include-runtime"' in runner and "SUCCESS not in executed.stdout" in runner,
         "Compiler runner must fail on missing success evidence")
    need('applicationId = "com.nightwave.player"' in gradle,
         "Android package ID changed unexpectedly")
    version = re.search(r'versionCode\s*=\s*(\d+)', gradle)
    need(version is not None and int(version.group(1)) >= 35,
         "Android app version code is missing or unexpectedly regressed")
    return findings

def self_test() -> None:
    fixture = {
        "verifier": ("function Read-Signer {}\nNightWave APK verification PASSED\n"
                     "} catch {\n^[0-9A-Fa-f]{64}$\n"
                     "if ($newSigner -ne $trusted.ToUpperInvariant())\n"
                     "if ($previousSigner -ne $newSigner)"),
        "windows": ("-TrustedSignerSHA256 $env:NIGHTWAVE_TRUSTED_SIGNER_SHA256\n"
                    "app\\build\\outputs\\apk\\debug\\app-debug.apk\n"
                    "Source files changed during Gradle build\n"
                    ":app:compileDebugAndroidTestKotlin"),
        "workflow": ("fix/nightwave-v2-nw22-build-stability\n"
                     "python3 tools/nw22_audio_core_smoke.py\n"
                     "python3 tools/verify_nw22_build_readiness.py"),
        "runner": '"-include-runtime"\nSUCCESS not in executed.stdout',
        "smoke": "NW22AudioCoreSmoke",
        "gradle": 'applicationId = "com.nightwave.player"\nversionCode = 35',
    }
    assert not checks(fixture), checks(fixture)
    fixture["verifier"] = fixture["verifier"].replace("^[0-9A-Fa-f]{64}$", "")
    assert "Trusted signer" in " ".join(checks(fixture))
    fixture["verifier"] += "\nNightWave APK verification PASSED"
    assert any("Exactly one" in item for item in checks(fixture))
    print("PASS: NW22 read-only build-gate positive/negative self-tests")

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        self_test()
        return 0
    sources = {
        "verifier": "tools/verify-apk-signature.ps1",
        "windows": "tools/build-windows.ps1",
        "workflow": ".github/workflows/android.yml",
        "runner": "tools/nw22_audio_core_smoke.py",
        "smoke": "tools/kotlin/NW22AudioCoreSmoke.kt",
        "gradle": "app/build.gradle.kts",
    }
    try:
        data = {key: (ROOT / file).read_text(encoding="utf-8") for key, file in sources.items()}
    except (OSError, UnicodeError) as e:
        print("FAIL: NW22 complete ZIP/source is missing (" + type(e).__name__ + ")")
        return 1
    issues = checks(data)
    for issue in issues:
        print("FAIL: " + issue)
    if issues:
        return 1
    print("PASS: NW22 source, signer safety and compiler gate wiring")
    print("UNVERIFIED: Android Gradle build, user-device signer, Samsung playback")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
