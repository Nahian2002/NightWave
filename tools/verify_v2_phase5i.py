#!/usr/bin/env python3
"""Phase 5I non-executing, source-only release gate.

This does NOT run Android Gradle, PowerShell, ADB, apksigner, or device tests.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda name: (root / name).read_text(encoding="utf-8")
gradle = read("app/build.gradle.kts")
builder = read("tools/build-windows.ps1")
signer = read("tools/verify-apk-signature.ps1")
device = read("tools/check-device-update.ps1")
preflight = read("tools/check-builder-syntax.ps1")
launcher = read("check-device-update.cmd")
build_launcher = read("build-windows.cmd")
manifest = read("app/src/main/AndroidManifest.xml")
service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
offline = read("app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt")
bridge = read("app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt")

assert 'versionCode = 34' in gradle
assert 'versionName = "2.0.0-alpha5r"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert "-VersionName '2.0.0-alpha5r' -VersionCode '34'" in builder
assert 'Verified build 34' in build_launcher
assert 'platform-tools' in builder
assert ":app:testDebugUnitTest :app:assembleDebug" in builder
assert "Select-String -LiteralPath $buildLog -Pattern '^BUILD SUCCESSFUL'" in builder
assert 'NightWave-debug-previous.apk' in builder
assert 'verify-apk-signature.ps1' in builder
assert 'apksigner.bat' in signer and 'dump badging' in signer

# Device checks are explicitly user-triggered and operate only in read-only
# ADB calls. No installation command is present in either launch path.
assert 'check-device-update.ps1' in launcher
assert 'No app installation' in device
assert 'adb.exe' in device
assert ' shell pm path $packageName' in device
assert ' pull $base[0] $temporaryApk' in device
assert 'getPackageInfo' not in device  # PackageManager source is not pulled from app data
assert 'NightWave-build-status.txt' in device
assert "$reportedVersion -ne $versionName" in device
assert "$reportedCode -ne $versionCode" in device
assert '$versionCode = 34' in device
assert 'Get-FileHash -LiteralPath $apk -Algorithm SHA256' in device
assert 'NightWave-device-update-check.txt' in device
assert 'No app installation, uninstallation or music-data change occurred.' in device
assert 'INCONCLUSIVE' in device and 'PASS - IN-PLACE UPDATE SIGNER MATCH' in device
assert 'Connect and authorize exactly one Android phone/emulator' in device
assert 'Installed NightWave version is newer. Do not downgrade or uninstall.' in device
assert "-PreviousApkPath $temporaryApk" in device
assert "Installed NightWave base.apk was not uniquely identified." in device
assert "if ($checked -like 'PASS*') { exit 0 }" in device

dangerous_actions = (
    "adb install", "adb uninstall", "pm clear", "am force-stop",
    "rm -rf", "clearApplicationUserData", "adb push",
    "shell am start", "run-as", "adb backup"
)
for script in (device, launcher):
    # Strip comment lines so safety reminders mentioning commands do not
    # count as executable actions.
    execution = "\n".join(line for line in script.splitlines()
                           if not line.lstrip().startswith("#"))
    for command in dangerous_actions:
        assert command.lower() not in execution.lower(), f"Forbidden device action: {command}"

assert "'check-device-update.ps1'" in preflight
assert 'ParseFile(' in preflight
assert 'NightWave-build-status.txt' in preflight
assert 'NightWave-debug-previous.apk' in preflight

# No changes in music state, Android launcher, or background service.
assert 'private fun resumeOfflineSession()' in service
assert 'nightwave_offline_v1' in offline
assert 'MediaController.Listener' in bridge
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1

for name, source in (
    ("builder", builder), ("APK verifier", signer),
    ("device preflight", device), ("syntax preflight", preflight)
):
    assert source.count("{") == source.count("}"), f"{name}: unbalanced source braces"

print("PASS: Phase 5I APK provenance and read-only connected-device source contracts")
print("PENDING: PowerShell execution, Android Gradle, ADB device test, runtime playback")
