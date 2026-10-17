#!/usr/bin/env python3
"""NightWave Phase 5K source-only crash triage and controller guard gate.

This verifies wiring only. It does NOT build an APK, run Android tests,
read device crash history or prove the app is crash-free.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda x: (root / x).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
gradle = read("app/build.gradle.kts")
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
premium = read(base + "v2/ui/NightWaveStudioActivity.kt")
classic = read(base + "ui/MainActivity.kt")
recorder = read(base + "diagnostics/NightWaveCrashRecorder.kt")
history = read(base + "diagnostics/NightWaveExitHistory.kt")
instrumented = read("app/src/androidTest/java/com/nightwave/player/diagnostics/NightWaveExitHistoryInstrumentedTest.kt")
service = read(base + "playback/PlaybackService.kt")
offline = read(base + "offline/OfflineAudioLibrary.kt")
builder = read("tools/build-windows.ps1")
device = read("tools/check-device-update.ps1")
manifest = read("app/src/main/AndroidManifest.xml")

assert 'versionCode = 34' in gradle
assert 'versionName = "2.0.0-alpha5r"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert "-VersionName '2.0.0-alpha5r' -VersionCode '34'" in builder
assert "$versionName = '2.0.0-alpha5r'" in device
assert "$versionCode = 34" in device
# Critically, the checker must compare the build-status *regex* against the
# current version, not merely bind the correct $versionName variable.
assert "$reportedVersion -ne $versionName" in device
assert "$reportedCode -ne $versionCode" in device
assert "Build status has no readable NightWave version code." in device

# Premium and classic surfaces both expose the opt-in private summary.
assert 'private fun showCrashSummaryDialog()' in premium
assert 'NightWaveCrashRecorder.report(this).take(5000)' in premium
assert '.setMessage(contextText + "\\n\\n" + report)' in premium
assert 'NightWaveExitHistory.report(this)' in premium
assert 'NightWaveExitHistory.report(this)' in classic
assert 'onShareDiagnostics = { showCrashSummaryDialog() }' in premium
assert 'onShareDiagnostics = onShareDiagnostics' in premium
assert 'OutlinedButton(onClick = onShareDiagnostics' in premium
assert 'View / share crash summary' in premium
assert 'setPositiveButton("Share summary")' in premium
assert 'setNegativeButton("Close", null)' in premium
assert 'Intent.createChooser' in premium
assert 'Intent.ACTION_SEND' in premium
assert 'Intent.EXTRA_TEXT, report' in premium

# Android 11+ exit reasons, no message/traces/UIDs or new permissions.
assert 'Build.VERSION.SDK_INT < Build.VERSION_CODES.R' in history
assert 'getHistoricalProcessExitReasons(' in history
assert 'context.packageName, 0, MAX_EXITS' in history
assert 'MAX_EXITS = 5' in history
for reason in ('REASON_CRASH', 'REASON_CRASH_NATIVE', 'REASON_ANR',
               'REASON_LOW_MEMORY', 'REASON_INITIALIZATION_FAILURE'):
    assert reason in history
for danger in ('getTraceInputStream(', 'getDescription(', 'getProcessName(',
               'getRealUid(', 'getPackageUid(', 'getDefiningUid('):
    # Comments may mention dangerous calls; executable lines must not use them.
    source = "\n".join(line for line in history.splitlines()
                       if not line.lstrip().startswith('*') and
                       not line.lstrip().startswith('//'))
    assert danger not in source, danger
assert instrumented.count('@Test') == 2

# Previously the UI called MediaController.play()/pause()/seekTo()
# without catching a synchronous disconnect.
assert 'private fun handleControlFailure(operation: String, error: RuntimeException)' in bridge
assert 'handleControlFailure("play/pause", error)' in bridge
assert 'handleControlFailure("seek", error)' in bridge
assert 'catch (error: RuntimeException)' in bridge
assert 'MediaController.releaseFuture(pending)' in bridge
assert 'error = "Playback connection was interrupted. Tap Reconnect."' in bridge
assert 'override fun retry() {' in bridge

# Existing service and user collections were not replaced or cleared.
assert 'private fun resumeOfflineSession()' in service
assert 'nightwave_offline_v1' in offline
assert 'nightwave_crash_diagnostics_v1' in recorder
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1

for name, code in (('controller', bridge), ('premium', premium),
                   ('classic', classic), ('exit history', history)):
    assert code.count('{') == code.count('}'), name + " braces"
    imports = [l for l in code.splitlines() if l.startswith('import ')]
    assert len(set(imports)) == len(imports), name + " duplicate imports"

print("PASS: Phase 5K controller-race recovery and opt-in Android crash/exit triage source wiring")
print("PENDING: Gradle/Android SDK build, instrumentation, phone crash reproduction")
