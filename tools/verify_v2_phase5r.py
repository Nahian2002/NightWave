#!/usr/bin/env python3
"""NightWave 5R source gate: gesture stability, CI compilation and release identity.

A passing source gate is NOT proof of Android compilation or phone stability.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda p: (root / p).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
manifest = read("app/src/main/AndroidManifest.xml")
gradle = read("app/build.gradle.kts")
player = read(base + "v2/ui/StudioPlayerScreens.kt")
gesture = read(base + "v2/ui/PlayerGestureRules.kt")
studio = read(base + "v2/ui/NightWaveStudioActivity.kt")
service = read(base + "playback/PlaybackService.kt")
offline = read(base + "offline/OfflineAudioLibrary.kt")
builder = read("tools/build-windows.ps1")
device = read("tools/check-device-update.ps1")
workflow = read(".github/workflows/android.yml")
old_gate = read("tools/verify_v2_phase5o.py")
unit_tests = read("app/src/test/java/com/nightwave/player/v2/ui/PlayerGestureRulesTest.kt")

assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 34' in gradle
assert 'versionName = "2.0.0-alpha5r"' in gradle
assert "-VersionName '2.0.0-alpha5r' -VersionCode '34'" in builder
assert "$versionName = '2.0.0-alpha5r'" in device
assert "$versionCode = 34" in device
assert '$reportedVersion -ne $versionName' in device

# Consistent physical travel distances across low/high DPI displays.
assert 'MIN_VERTICAL_SWIPE_DP = 72f' in gesture
assert 'fun decideVerticalSwipe(distancePx: Float, thresholdPx: Float)' in gesture
assert 'distancePx.isFinite()' in gesture
assert 'thresholdPx.isFinite()' in gesture
assert 'PlayerVerticalSwipe.EXPAND' in gesture
assert 'PlayerVerticalSwipe.MINIMIZE' in gesture
assert 'PlayerGestureRules.MIN_VERTICAL_SWIPE_DP.dp.toPx()' in player
assert player.count('PlayerGestureRules.MIN_VERTICAL_SWIPE_DP.dp.toPx()') >= 2
assert 'PlayerVerticalSwipe.MINIMIZE -> onHide()' in player
assert 'PlayerVerticalSwipe.EXPAND -> onExpand()' in player
assert 'PlayerVerticalSwipe.MINIMIZE) onDismiss()' in player
assert 'if (travel > 70f)' not in player
assert 'if (dragged > 65f)' not in player
assert 'IconButton(onClick = actions::next, modifier = Modifier.size(48.dp)' in player

# No collision with artwork/queue gestures and the page navigation pager.
assert 'detectHorizontalDragGestures(' in player
assert 'MIN_SWIPE_DP.dp.toPx()' in player
assert 'detectVerticalDragGestures(' in player
assert 'travel <= -72.dp.toPx()' in player
assert 'SnackbarResult.ActionPerformed' in player
assert 'userScrollEnabled = !expanded && !showQueue' in studio
assert 'profile.motion == MotionPreference.REDUCED' in studio
assert 'onOpenClassic = onOpenClassic' in studio

# JVM unit regression coverage now includes DPI scaling and bad values.
assert unit_tests.count('@Test') >= 9
assert 'verticalGesturesUseDensityScaledThresholdAndBothDirections' in unit_tests
assert 'verticalSwipesRejectInvalidNumbersAndZeroThreshold' in unit_tests
assert 'verticalSwipeIsIndependentOfHorizontalPlaybackEligibility' in unit_tests
assert 'PlayerVerticalSwipe.MINIMIZE -> onHide()' in old_gate

# CI is automatic only for the new stacked draft PR, and tests actual Kotlin
# Android/debug unit/instrumentation-source compilations before artifact upload.
assert 'pull_request:' in workflow
assert 'feature/nightwave-v2-phase5q-search-recovery' in workflow
assert 'workflow_dispatch:' in workflow
assert "gradle-version: '8.11.1'" in workflow
assert 'tools/verify_source.py' in workflow
assert 'tools/verify_v2_phase5r.py' in workflow
for task in (':app:testDebugUnitTest', ':app:assembleDebug',
             ':app:compileDebugAndroidTestKotlin'):
    assert task in workflow, task
assert "versionCode='34' versionName='2.0.0-alpha5r'" in workflow
assert 'sha256sum "$apk"' in workflow
assert 'if: success()' in workflow
assert 'retention-days: 5' in workflow
assert 'UNVERIFIED-FOR-DEVICE' in workflow
assert 'permissions:' in workflow and 'contents: read' in workflow

# Preserve the canonical premium launcher, playlists and imported files.
assert manifest.count('android.intent.category.LAUNCHER') == 1
assert 'android:allowBackup="false"' in manifest
assert 'nightwave_offline_v1' in offline
assert 'private fun resumeOfflineSession()' in service
assert 'NightWaveStudioActivity::class.java' in service
ET.parse(root / "app/src/main/AndroidManifest.xml")

for label, code in (('player', player), ('gesture', gesture),
                    ('studio', studio), ('service', service)):
    assert code.count('{') == code.count('}'), label + " braces"
    imports = [line for line in code.splitlines() if line.startswith('import ')]
    assert len(imports) == len(set(imports)), label + " duplicate imports"

print("PASS: NightWave Phase 5R density-stable swipes, safe player UX, CI build contract")
print("PENDING: GitHub Actions result, Android device gesture and in-place APK update QA")
