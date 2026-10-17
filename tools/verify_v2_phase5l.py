#!/usr/bin/env python3
"""Phase 5L: source-only non-destructive crash-capture/build wiring contract.

Does not execute PowerShell, Gradle, Android SDK tools or an actual device.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda path: (root / path).read_text(encoding="utf-8")
gradle = read("app/build.gradle.kts")
builder = read("tools/build-windows.ps1")
syntax = read("tools/check-builder-syntax.ps1")
capture = read("tools/capture-nightwave-crash.ps1")
cmd = read("capture-nightwave-crash.cmd")
device = read("tools/check-device-update.ps1")
signer = read("tools/verify-apk-signature.ps1")
bridge = read("app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt")
offline = read("app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt")
service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
manifest = read("app/src/main/AndroidManifest.xml")

# Always keep the build, provenance check and installed-device check aligned.
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 30' in gradle
assert 'versionName = "2.0.0-alpha5n"' in gradle
assert "-VersionName '2.0.0-alpha5n' -VersionCode '30'" in builder
assert "'Version: 2.0.0-alpha5n'" in builder
assert "'Version code: 30'" in builder
assert "$versionName = '2.0.0-alpha5n'" in device
assert '$versionCode = 30' in device
assert '$reportedVersion -ne $versionName' in device
assert '$reportedCode -ne $versionCode' in device
assert device.count("if ($checked -like 'PASS*') { exit 0 }") == 1
assert device.rstrip().endswith('exit 1')
assert device.count("The build status version does not match the current NightWave source.") == 1
assert device.count("$signerVerifier = Join-Path $PSScriptRoot") == 1
assert 'Build status version does not match the current NightWave source.' in device

# New capture must read only bounded logcat history and never save raw logs.
assert "NightWave-crash-summary.txt" in capture
assert "NightWaveBuild\\android-sdk\\platform-tools\\adb.exe" in capture
assert 'logcat -b crash -d -t 4000 -v threadtime' in capture
assert "No raw Android logcat was saved." in capture
assert 'Process:' in capture and 'com\\.nightwave\\.player' in capture
assert "exceptionClasses" in capture and "NightWave frames:" in capture
assert "Select-Object -Last $limit" in capture and "MAX" not in capture
assert '$raw = $null' in capture
assert 'param([switch]$SelfTest)' in capture
assert 'OTHER_APP_SECRET|USER_PRIVATE_SECRET|com.example.other|PID:' in capture
assert "PASS: NightWave-only crash filtering" in capture
assert 'capture-nightwave-crash.ps1' in cmd
assert 'NightWave-crash-summary.txt' in cmd
assert 'NightWave-crash.log' not in cmd
assert 'NightWave-system-log.log' not in cmd
assert 'logcat -b main -b system' not in cmd
assert 'capture-nightwave-crash.ps1' in syntax
assert 'ParseFile(' in syntax
assert "Crash redaction self-test" in builder
assert "-File $crashScript -SelfTest" in builder
assert "crashTest -notmatch 'PASS: NightWave-only crash filtering'" in builder

# Read-only capture cannot perform destructive Android commands.
for source in (capture, cmd):
    executable = "\n".join(
        line for line in source.splitlines()
        if not line.lstrip().startswith('#') and not line.lstrip().lower().startswith('rem ')
    ).lower()
    for prohibited in ('adb uninstall', 'adb install', 'pm clear',
                       'logcat -c', 'am force-stop', 'shell rm ', 'adb push'):
        assert prohibited not in executable, prohibited

# Disconnect has to release its future without launching an audio session.
on_disconnect = bridge.split('override fun onDisconnected(disconnected: MediaController)', 1)[1].split(
    'private val listener', 1
)[0]
assert 'controller = null' in on_disconnect
assert 'future = null' in on_disconnect
assert 'MediaController.releaseFuture(pending)' in on_disconnect
assert 'if (released || controller !== disconnected) return' in on_disconnect
assert 'override fun retry() {' in bridge

# Original imported-media index, safe explicit resume and one launcher retained.
assert 'nightwave_offline_v1' in offline
assert 'private fun resumeOfflineSession()' in service
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1
assert 'android:allowBackup="false"' in manifest
assert "apksigner.bat" in signer

for name, contents in (
    ('builder', builder), ('syntax preflight', syntax),
    ('crash capture', capture), ('device check', device), ('bridge', bridge)
):
    assert contents.count('{') == contents.count('}'), name + ' brace count'
print('PASS: Phase 5L safe redacted NightWave logcat capture + dynamic APK version/device contract')
print('PENDING: real Windows PowerShell, Android Gradle, APK, and phone crash runtime tests')
