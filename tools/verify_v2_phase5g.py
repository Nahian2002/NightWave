#!/usr/bin/env python3
"""NightWave 5G source gate. It does NOT compile Android or execute PowerShell."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
code = root / "app/src/main/java/com/nightwave/player"
read = lambda file: file.read_text(encoding="utf-8")
gradle = read(root / "app/build.gradle.kts")
build = read(root / "tools/build-windows.ps1")
launcher = read(root / "build-windows.cmd")
bridge = read(code / "v2/playback/StudioPlaybackBridge.kt")
studio = read(code / "v2/ui/NightWaveStudioActivity.kt")
player = read(code / "v2/ui/StudioPlayerScreens.kt")
startup = read(code / "NightWaveApp.kt")
recorder = read(code / "diagnostics/NightWaveCrashRecorder.kt")
service = read(code / "playback/PlaybackService.kt")
offline = read(code / "offline/OfflineAudioLibrary.kt")
manifest = read(root / "app/src/main/AndroidManifest.xml")

assert 'versionCode = 32' in gradle
assert 'versionName = "2.0.0-alpha5p"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'compileSdk = 35' in gradle and 'targetSdk = 35' in gradle
assert 'Gradle' in build and 'gradle-8.11.1' in build
assert 'android-commandline-tools' in build
assert ':app:testDebugUnitTest :app:assembleDebug' in build

# Distinguish success from stale old builds, including Java/SDK preflight errors.
assert '$statusFile' in build and '$previousApk' in build
assert 'NightWave-debug-previous.apk' in build
assert "'STARTED - not a verified build." in build
assert 'if (-not (Test-Path -LiteralPath $errorReport))' in build
assert 'Copy-Item -LiteralPath $output -Destination $previousApk -Force' in build
assert 'Remove-Item -LiteralPath $output -Force' in build
assert "verify-apk-signature.ps1" in build
assert "-VersionName '2.0.0-alpha5p' -VersionCode '32'" in build
assert 'aapt.exe' in read(root / 'tools/verify-apk-signature.ps1')
assert 'apksigner.bat' in read(root / 'tools/verify-apk-signature.ps1')
assert '$LASTEXITCODE -ne 0' in build
assert 'SHA256:' in build
assert 'BUILD FAILED' in launcher and 'BUILD SUCCESS' in launcher

# App launch must not terminate for optional network extractor or shadow DB.
assert 'try {' in startup and 'NewPipe.init(ExtractorDownloader())' in startup
assert 'catch (error: Exception)' in startup
assert 'RoomShadowMigrator(this, database)' in studio
assert 'verifiedReader = VerifiedRoomReader(this, database)' in studio
assert 'profileShadow = ProfileDataStoreMirror(this)' in studio
assert 'catch (error: Exception)' in studio
assert 'Storage preview unavailable; NightWave is using your original library.' in studio

# Media3 controller failure and disconnect must leave reconnect possible.
assert 'private val controllerListener = object : MediaController.Listener' in bridge
assert 'override fun onDisconnected(disconnected: MediaController)' in bridge
assert 'if (released || controller !== disconnected) return' in bridge
assert 'future = null' in bridge
assert 'controller = null' in bridge
assert '.setListener(controllerListener)' in bridge
assert 'if (controller == null && future == null) connect()' in bridge
assert 'fun connect()' in bridge and 'if (released || future != null || controller != null) return' in bridge
assert 'runCatching { MediaController.releaseFuture(pending) }' in bridge
assert 'override fun retry() {' in bridge
assert 'if (!state.connected) connect()' in bridge
assert 'if (!playerState.connected) "Reconnect" else "Retry"' in studio
assert 'if (state.connected) "Retry playback" else "Reconnect player"' in player

# Privacy and backward compatibility.
assert 'NightWaveCrashRecorder.install(this)' in startup
assert '.versionName' in recorder
assert 'getSharedPreferences("nightwave_offline_v1"' in offline
assert 'private fun resumeOfflineSession()' in service
creation = service.split('override fun onCreate()', 1)[1].split('override fun onStartCommand', 1)[0]
assert 'resolveAndPlay(' not in creation and 'playOfflineId(' not in creation
assert 'deleteRecursively' not in bridge + studio + build + service
assert 'clearApplicationUserData' not in bridge + studio + build + service
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1
assert 'android:allowBackup="false"' in manifest

for name, source in (
    ('Media3 bridge', bridge), ('premium screen', studio), ('full player', player),
    ('app initialization', startup), ('crash recorder', recorder)
):
    assert source.count('{') == source.count('}'), name + ': braces'
    imports = [line for line in source.splitlines() if line.startswith('import ')]
    assert len(imports) == len(set(imports)), name + ': duplicate imports'

print('PASS: Phase 5G source-level app startup, Media3 reconnect and build failure guards')
print('PENDING: Windows PowerShell execution, Android Gradle + JUnit, Samsung playback/logcat')
