#!/usr/bin/env python3
"""NW27 cross-file release safety gate. SOURCE ONLY: cannot certify an APK."""
from __future__ import annotations
import argparse
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {
    "app": "app/src/main/java/com/nightwave/player/NightWaveApp.kt",
    "recorder": "app/src/main/java/com/nightwave/player/diagnostics/NightWaveCrashRecorder.kt",
    "privacy": "app/src/main/java/com/nightwave/player/diagnostics/NightWaveCrashPrivacyPolicy.kt",
    "client": "app/src/main/java/com/nightwave/player/v2/cloud/NightWaveDriveClient.kt",
    "response": "app/src/main/java/com/nightwave/player/v2/cloud/DriveResponseReadPolicy.kt",
    "worker": "app/src/main/java/com/nightwave/player/v2/cloud/NightWaveCloudWorker.kt",
    "state": "app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupState.kt",
    "auth": "app/src/main/java/com/nightwave/player/v2/cloud/NightWaveCloudAuthorization.kt",
    "offline": "app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt",
    "lyrics": "app/src/main/java/com/nightwave/player/v2/lyrics/PrivateLyricsStore.kt",
    "manifest": "app/src/main/AndroidManifest.xml",
    "gradle": "app/build.gradle.kts",
    "windows": "tools/build-windows.ps1",
    "windowsGate": "tools/check-nw27-integrity.ps1",
    "windowsSyntax": "tools/check-builder-syntax.ps1",
    "signing": "tools/verify-apk-signature.ps1",
    "runner": "tools/nw27_hardening_smoke.py",
    "smoke": "tools/kotlin/NW27HardeningSmoke.kt",
    "unitCrash": "app/src/test/java/com/nightwave/player/diagnostics/NightWaveCrashPrivacyPolicyTest.kt",
    "unitDrive": "app/src/test/java/com/nightwave/player/v2/cloud/DriveResponseReadPolicyTest.kt",
    "ci": ".github/workflows/android.yml",
    "canary": ".github/workflows/nightwave-runner-canary.yml",
}

def check(source: dict[str, str]) -> list[str]:
    failures: list[str] = []
    def s(key: str) -> str:
        return source.get(key, "")
    def need(ok: bool, issue: str) -> None:
        if not ok:
            failures.append(issue)

    need("catch (error: Exception)" in s("app") and
         "catch (error: LinkageError)" in s("app"),
         "Offline startup fallback must catch extractor linkage failure")
    need("NightWaveCrashPrivacyPolicy.threadLabel(thread.name)" in s("recorder") and
         "thread.name.take(" not in s("recorder"),
         "Crash reports must not include custom thread names")
    need("NightWaveCrashPrivacyPolicy.safeSourceSymbol(" in s("recorder") and
         "NightWaveCrashPrivacyPolicy.safeFrameLine(" in s("recorder") and
         "throwable.message" not in s("recorder"),
         "Crash summaries must redact symbol/line data and exception messages")
    need('else -> "background"' in s("privacy") and
         'character in "._$"' in s("privacy"),
         "Crash redaction categories and safe symbol alphabet missing")
    need("catch (cancel: CancellationException)" in s("worker") and
         "throw cancel" in s("worker"),
         "Cloud WorkManager cancellation must propagate, not schedule retries")
    need("DriveHttpFailurePolicy.mayRetry(code)" in s("worker") and
         "beforeUpload.consentSnapshot()" in s("worker"),
         "NW26 retry/consent checks must survive NW27")
    need("DriveResponseReadPolicy.canAppend(" in s("client") and
         "DriveResponseReadPolicy.stalled(" in s("client") and
         ".followRedirects(false)" in s("client"),
         "Drive streaming must be bounded and redirects disabled")
    need("incomingBytes <= maxBytes - currentBytes" in s("response") and
         "MAX_CONSECUTIVE_EMPTY_READS = 8" in s("response"),
         "Drive bounded-read arithmetic must be overflow safe")
    need('"nightwave_cloud_backup_v1"' in s("state") and
         "drive.appdata" in s("auth") and "drive.file" not in s("auth"),
         "Least privilege Drive scope/state changed")
    need('getSharedPreferences("nightwave_offline_v1"' in s("offline") and
         "OfflineLibraryCapacity.MAX_ITEMS" in s("offline"),
         "Legacy offline library ID/index preservation changed")
    need('"nightwave_private_lrc_v1"' in s("lyrics"),
         "Private synchronized lyrics storage changed")
    need('android:allowBackup="false"' in s("manifest") and
         'android:usesCleartextTraffic="false"' in s("manifest"),
         "Android private storage and network security flags changed")
    need('applicationId = "com.nightwave.player"' in s("gradle") and
         "minSdk = 26" in s("gradle") and
         "targetSdk = 35" in s("gradle"),
         "App package/SDK identity changed unexpectedly")
    need(":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin"
         in s("windows") and
         "Source files changed during Gradle build" in s("windows"),
         "Windows release compiler and mutation gates missing")
    need("check-nw27-integrity.ps1" in s("windows") and
         "check-nw27-integrity.ps1" in s("windowsSyntax") and
         "PASS: NW27 Windows 14-source" in s("windowsGate") and
         "param([switch]$SelfTest)" in s("windowsGate"),
         "One-click Windows builder must run/parse NW27 native source gate")
    need("if ($previousSigner -ne $newSigner)" in s("signing") and
         "if ($newSigner -ne $trusted.ToUpperInvariant())" in s("signing"),
         "Installed/upgraded APK signer continuity checks missing")
    need("PASS: NW27 28 exact-source Kotlin safety assertions" in s("runner") and
         'println("PASS: NW27 " + cases.size' in s("smoke"),
         "NW27 exact Kotlin runtime fixture or runner missing")
    need(s("unitCrash").count("@Test") >= 2 and
         s("unitDrive").count("@Test") >= 3,
         "NW27 new unit regression suites missing")
    need("python3 tools/verify_nw27_hardening.py" in s("ci") and
         "python3 tools/nw27_hardening_smoke.py" in s("ci") and
         "fix/nightwave-v2-nw27-final-hardening" in s("ci"),
         "NW27 release gate not wired to full Android CI")
    need("fix/nightwave-v2-nw27-final-hardening" in s("canary") and
         "NIGHTWAVE_RUNNER_STARTED=true" in s("canary"),
         "Independent one-step runner canary missing")
    for key in ("app", "recorder", "privacy", "client", "response",
                "worker", "offline", "lyrics"):
        imports = re.findall(r"^import\s+(\S+)", s(key), re.M)
        need(len(imports) == len(set(imports)), "Duplicate exact Kotlin imports: " + key)
    return failures

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        broken = {key: "" for key in SOURCES}
        findings = check(broken)
        assert len(findings) >= 18, findings
        assert check({}) == findings
        print("PASS: NW27 source release gate rejects incomplete code")
        print("LIMIT: no Android compilation was attempted")
        return 0
    try:
        content = {key: (ROOT / path).read_text(encoding="utf-8")
                   for key, path in SOURCES.items()}
    except (OSError, UnicodeError) as error:
        print("FAIL: missing/inaccessible complete NW27 ZIP (" +
              type(error).__name__ + ")")
        return 1
    findings = check(content)
    for failure in findings:
        print("FAIL:", failure)
    if findings:
        return 1
    print("PASS: NW27 crash, Drive, data-preservation and release source contracts")
    print("NOT VERIFIED: Android Gradle build, signing against installed app and phone QA")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
