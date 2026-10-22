#!/usr/bin/env python3
"""NW07 cross-phase release evidence checks. Does not compile or execute Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]

def source(path: str) -> str:
    return (root / path).read_text(encoding="utf-8")

gate = source("tools/check-nw07-source-gate.ps1")
builder = source("tools/build-windows.ps1")
launcher = source("build-windows.cmd")
workflow = source(".github/workflows/android.yml")
status_check = source("tools/check-device-update.ps1")
signer = source("tools/verify-apk-signature.ps1")

# The local builder must invoke a fail-closed integrated gate, not merely
# advertise release readiness in the README.
assert "check-nw07-source-gate.ps1" in builder
assert "-SelfTest" in builder
assert "-ProjectRoot $project" in builder
assert "NW07 integrated source gate failed; no Gradle build attempted." in builder
assert "NW07 complete source tree" in gate
assert "NW01" in gate and "NW06" in gate
assert "Duplicate import self-test unexpectedly passed." in gate

# Fingerprinting must be tied to one attempt, not a stale APK or source ZIP.
assert "NightWave-source-fingerprint.txt" in builder
assert "Remove-Item -LiteralPath $sourceManifest -Force" in builder
assert "Source fingerprint SHA256: $sourceManifestHash" in builder
assert "Source files changed during Gradle build." in builder
assert "Source fingerprint manifest changed during build." in builder
assert "Get-FileHash -LiteralPath $full -Algorithm SHA256" in builder

# Mandatory build result and safety checks.
for task in (":app:testDebugUnitTest", ":app:assembleDebug",
             ":app:compileDebugAndroidTestKotlin"):
    assert task in builder and task in workflow
assert "Select-String -LiteralPath $buildLog -Pattern '^BUILD SUCCESSFUL'" in builder
assert "verify-apk-signature.ps1" in builder
assert "Get-FileHash -LiteralPath $output -Algorithm SHA256" in builder
assert "dist\\NightWave-debug-previous.apk" in launcher
assert "apksigner verify" in signer
assert "Signer #1 certificate SHA-256 digest" in signer
assert "APK signing certificate changed" in signer
assert "No freshly verified NightWave-debug.apk exists" in status_check
assert "APK SHA256 differs from the last verified local build" in status_check

# GitHub workflow must preserve all prior functional gates; no fake PASS on
# private-runner startup failures.
for phase in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub",
              "nw04_premium_player", "nw05_integrated_stability",
              "nw06_discovery_search", "nw07_release_gate"):
    assert f"python3 tools/verify_{phase}.py" in workflow
assert "fix/nightwave-v2-nw07-build-release-gate" in workflow
assert "pwsh -NoProfile -File tools/check-nw07-source-gate.ps1 -SelfTest" in workflow
assert "pwsh -NoProfile -File tools/check-nw07-source-gate.ps1" in workflow
assert "actions/checkout@v4" in workflow
assert "gradle --no-daemon" in workflow

print("PASS: NW07 local build safety, source attestation and CI integration contracts")
print("UNVERIFIED: Android compilation, JVM results, APK, signers and Samsung device QA")
