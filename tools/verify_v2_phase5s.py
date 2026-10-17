#!/usr/bin/env python3
"""NightWave Phase 5S source-only build-recovery safety checks.

This is deliberately NOT an Android APK compile or Windows PowerShell run.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda path: (root / path).read_text(encoding="utf-8")
gradle = read("app/build.gradle.kts")
build = read("tools/build-windows.ps1")
doctor = read("tools/diagnose-build.ps1")
doctor_cmd = read("diagnose-nightwave-build.cmd")
syntax = read("tools/check-builder-syntax.ps1")
device = read("tools/check-device-update.ps1")
workflow = read(".github/workflows/android.yml")
nightwave = read("app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt")
sheet = read("app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt")
queue = read("app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt")
service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
offline = read("app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt")
backup = read("app/src/main/java/com/nightwave/player/v2/data/LibraryBackupCodec.kt")
snapshot = read("app/src/main/java/com/nightwave/player/v2/data/room/LegacyLibrarySnapshot.kt")
manifest = read("app/src/main/AndroidManifest.xml")

assert 'versionName = "2.0.0-alpha5s"' in gradle
assert 'versionCode = 35' in gradle
assert "-VersionName '2.0.0-alpha5s' -VersionCode '35'" in build
assert "'Version: 2.0.0-alpha5s'" in build
assert "'Version code: 35'" in build
assert "$versionName = '2.0.0-alpha5s'" in device
assert "$versionCode = 35" in device
assert '$reportedVersion -ne $versionName' in device

# A new run cannot claim the last run's log or APK as its own.
assert "$previousLog = Join-Path $outDir 'NightWave-build-full-previous.log'" in build
assert "Copy-Item -LiteralPath $buildLog -Destination $previousLog -Force" in build
assert "Gradle not yet started for this NightWave build attempt." in build
assert "Set-Content -LiteralPath $buildLog -Encoding UTF8" in build
assert "Remove-Item -LiteralPath $output -Force" in build
assert "No Android APK was verified or published by this build." in build

# Unlike older builds, also compile every instrumentation Kotlin source
# even before an emulator/physical device is available.
for task in (":app:testDebugUnitTest", ":app:assembleDebug",
             ":app:compileDebugAndroidTestKotlin"):
    assert task in build, task
    assert task in workflow, task
assert "Select-String -LiteralPath $buildLog -Pattern '^BUILD SUCCESSFUL'" in build
assert "Gradle returned an Android compilation/test failure." in build

# The diagnostics tool runs locally, never uses ADB/network and excludes raw
# Android music data. It must not turn build failure into success.
assert "diagnose-build.ps1" in build
assert "-File $diagnosticScript -SelfTest" in build
assert "PASS: NightWave build diagnostic self-test" in build
assert "diagnose-build.ps1" in syntax
assert "tools\\diagnose-build.ps1" in doctor_cmd
assert "NightWave-build-diagnostic.txt" in doctor
assert "NightWave-build-full.log" in doctor
assert "NightWave-build-full-previous.log" not in doctor
assert "Redact-Line" in doctor
assert "Users/<user>" in doctor
assert "imaginary-test-secret" in doctor
assert "No compiler/test failure lines were available." in doctor
assert "A missing log does not mean compilation succeeded." in doctor
for forbidden in ("adb.exe", "adb install", "pm clear", "logcat -c",
                  "Invoke-WebRequest", "Start-Process"):
    assert forbidden not in doctor, forbidden

# Real historical Windows failures must not regress.
assert "import androidx.compose.material3.Surface" in nightwave
assert "keySet()" not in backup
assert "keySet()" not in snapshot

# Repeated queue song URLs no longer crash LazyColumn with duplicate keys.
assert "itemsIndexed(state.queue, key = { index, _ -> index })" in sheet
assert "itemsIndexed(state.queue, key = { _, video -> video.url })" not in sheet
queue_tests = read("app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveQueueSwipeInstrumentedTest.kt")
assert 'duplicatedQueueUrlsUseUniqueComposeRowKeys' in queue_tests
assert queue_tests.count('@Test') == 3
assert "fun removeUpcoming(at: Int, expectedUrl: String)" in queue
assert "ACTION_REMOVE_UPCOMING" in service

# CI is scoped to this stacked PR, and its artifact is NOT a safe installed
# APK until an on-device signing check confirms compatibility.
assert "pull_request:" in workflow
assert "feature/nightwave-v2-phase5r-accessibility-build-gates" in workflow
assert "tools/verify_v2_phase5s.py" in workflow
assert "UNVERIFIED-FOR-DEVICE" in workflow
assert "versionCode='35' versionName='2.0.0-alpha5s'" in workflow
assert "sha256sum" in workflow

assert "nightwave_offline_v1" in offline
assert "private fun resumeOfflineSession()" in service
assert manifest.count("android.intent.category.LAUNCHER") == 1
assert 'android:allowBackup="false"' in manifest
ET.parse(root / "app/src/main/AndroidManifest.xml")

for label, code in (("build", build), ("diagnostic", doctor), ("queue UI", sheet)):
    assert code.count("{") == code.count("}"), label + " brace balance"
for label, code in (("queue UI", sheet), ("premium UI", nightwave)):
    imports = [line for line in code.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), label + " duplicate imports"

# Compose SemanticsNodeInteraction.assertExists/assertDoesNotExist are instance
# methods in the pinned test dependency, not top-level functions to import.
# Windows Gradle first exposed these compile failures in Phase 5S.
for test_file in (
    "app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveProfileCompactInstrumentedTest.kt",
    "app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveQueueSwipeInstrumentedTest.kt",
):
    test_source = read(test_file)
    for invalid_import in (
        "import androidx.compose.ui.test.assertExists",
        "import androidx.compose.ui.test.assertDoesNotExist",
    ):
        assert invalid_import not in test_source, (test_file, invalid_import)
    assert ".assertExists()" in test_source, test_file
assert ".assertDoesNotExist()" in queue_tests

print("PASS: NightWave 5S build diagnostics, Android test compilation and repeated-queue crash source gates")
print("PENDING: actual Windows PowerShell self-test, Android APK compile and signing/device checks")
