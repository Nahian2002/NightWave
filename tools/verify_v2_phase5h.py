#!/usr/bin/env python3
"""NightWave Phase 5H source-only APK publishing gate.

Checks repository consistency; Android Gradle, PowerShell and signer
verification must still run on a Windows machine with Android Build Tools.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
base = root / "app/src/main/java/com/nightwave/player"
read = lambda f: f.read_text(encoding="utf-8")
gradle = read(root / "app/build.gradle.kts")
builder = read(root / "tools/build-windows.ps1")
signer = read(root / "tools/verify-apk-signature.ps1")
syntax = read(root / "tools/check-builder-syntax.ps1")
cmd = read(root / "build-windows.cmd")
manifest = read(root / "app/src/main/AndroidManifest.xml")
service = read(base / "playback/PlaybackService.kt")
offline = read(base / "offline/OfflineAudioLibrary.kt")
bridge = read(base / "v2/playback/StudioPlaybackBridge.kt")
smoke = read(root / "app/src/androidTest/java/com/nightwave/player/diagnostics/NightWaveStartupSmokeTest.kt")

# App identity must match the builder and signer gate.
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 32' in gradle
assert 'versionName = "2.0.0-alpha5p"' in gradle
assert 'Version: 2.0.0-alpha5p' in builder
assert 'Version code: 32' in builder
assert "-VersionName '2.0.0-alpha5p' -VersionCode '32'" in builder
assert "'com.nightwave.player'" in signer

# Refuse unsigned, wrong-package, wrong-version, or mismatched-signer APKs.
assert 'aapt.exe' in signer and 'dump badging' in signer
assert 'apksigner.bat' in signer
assert 'verify --min-sdk-version 26 --verbose --print-certs' in signer
assert 'Signer #1 certificate SHA-256 digest:' in signer
assert 'Unexpected multiple signers' in signer
assert 'previousSigner -ne $newSigner' in signer
assert 'APK signing certificate changed.' in signer
assert 'installed-device signer' in signer.lower()
assert 'Get-FileHash -LiteralPath $output -Algorithm SHA256' in builder

# Windows preflight handles both scripts and never displays a stale published APK.
assert "'build-windows.ps1', 'verify-apk-signature.ps1'" in syntax
assert 'ParseFile(' in syntax
assert 'NightWave-build-status.txt' in syntax
assert 'NightWave-debug-previous.apk' in syntax
assert 'Remove-Item -LiteralPath $apk -Force' in syntax
assert 'Copy-Item -LiteralPath $apk -Destination $backup -Force' in syntax
assert 'NightWave-build-errors.txt' in syntax
assert 'NightWave-build-status.txt' in builder
assert 'Copy-Item -LiteralPath $output -Destination $previousApk -Force' in builder
assert 'Remove-Item -LiteralPath $output -Force' in builder
assert "Select-String -LiteralPath $buildLog -Pattern '^BUILD SUCCESSFUL'" in builder
assert 'if ($LASTEXITCODE -ne 0)' in builder
assert "if (-not $signerLine -or -not $continuityLine)" in builder
assert ':app:testDebugUnitTest :app:assembleDebug' in builder
assert 'No new APK was verified.' in cmd
assert 'Verified build 32' in cmd

# Emulator-only launch smoke checks must not mutate library, install or clear data.
assert 'ActivityScenario.launch(MainActivity::class.java)' in smoke
assert 'ActivityScenario.launch(NightWaveStudioActivity::class.java)' in smoke
assert smoke.count('@Test') == 2
assert 'clearApplicationUserData' not in smoke
assert 'removeFromPlaylist' not in smoke
assert 'forceStop' not in smoke

# Existing app namespaces remain unchanged.
assert 'private fun resumeOfflineSession()' in service
assert 'nightwave_offline_v1' in offline
assert 'MediaController.Listener' in bridge
assert 'deleteRecursively' not in signer + builder + syntax
assert 'adb uninstall' not in signer + builder + syntax
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1

for name, script in (
    ('Windows builder', builder), ('APK verifier', signer),
    ('PowerShell syntax preflight', syntax)
):
    assert script.count('{') == script.count('}'), name + ' braces'

print('PASS: Phase 5H package/signer/previous-APK continuity and Windows preflight source gates')
print('PENDING: Windows PowerShell run, Android Gradle/SDK signing, emulator smoke tests, physical phone QA')
