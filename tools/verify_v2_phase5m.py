#!/usr/bin/env python3
"""NightWave 5M: source-only premium default entry and fallback acceptance.

Checks source and manifest wiring, NOT Android build/device behavior.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda file: (root / file).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
gradle = read("app/build.gradle.kts")
manifest = read("app/src/main/AndroidManifest.xml")
studio = read(base + "v2/ui/NightWaveStudioActivity.kt")
classic = read(base + "ui/MainActivity.kt")
service = read(base + "playback/PlaybackService.kt")
offline = read(base + "offline/OfflineAudioLibrary.kt")
builder = read("tools/build-windows.ps1")
device = read("tools/check-device-update.ps1")
instrumentation = read("app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveDefaultEntryInstrumentedTest.kt")

assert 'versionCode = 30' in gradle
assert 'versionName = "2.0.0-alpha5n"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert "-VersionName '2.0.0-alpha5n' -VersionCode '30'" in builder
assert "$versionName = '2.0.0-alpha5n'" in device and '$versionCode = 30' in device

# The merged manifest must retain exactly one public launcher.
tree = ET.fromstring(manifest)
ns = '{http://schemas.android.com/apk/res/android}'
activities = tree.findall('./application/activity')
def activity(name):
    return next(a for a in activities if a.attrib.get(ns+'name') == name)
premium = activity('.v2.ui.NightWaveStudioActivity')
legacy = activity('.ui.MainActivity')
def has_intent(a, action):
    return any(i.attrib.get(ns+'name') == action for i in a.findall('.//action'))
assert premium.attrib[ns+'exported'] == 'true'
assert premium.attrib[ns+'launchMode'] == 'singleTop'
assert legacy.attrib[ns+'exported'] == 'false'
assert has_intent(premium, 'android.intent.action.MAIN')
assert has_intent(premium, 'android.intent.action.SEND')
assert not has_intent(legacy, 'android.intent.action.MAIN')
assert not has_intent(legacy, 'android.intent.action.SEND')
assert manifest.count('android.intent.category.LAUNCHER') == 1

# Music notification must route to redesigned app, never classic Home.
assert 'Intent(this, NightWaveStudioActivity::class.java)' in service
assert 'import com.nightwave.player.v2.ui.NightWaveStudioActivity' in service
assert 'Intent(this, MainActivity::class.java)' not in service

# Premium Settings leads to You, not immediately to the classic screen.
assert 'IconButton(onClick = { tab = StudioTab.PROFILE })' in studio
assert 'contentDescription = "Open NightWave settings"' in studio
assert 'contentDescription = "Open classic player"' not in studio
assert 'onOpenClassic = {' in studio
assert 'startActivity(Intent(this, MainActivity::class.java))' in studio
assert 'Text("Open classic interface")' in studio
assert 'onOpenClassic = onOpenClassic' in studio
assert 'onClose: () -> Unit' not in studio

# Native Back state continues to close sheets/player/playlist first.
assert 'showQueue -> showQueue = false' in studio
assert 'expanded -> expanded = false' in studio
assert 'selectedPlaylist = null' in studio
assert 'tab != StudioTab.HOME -> tab = StudioTab.HOME' in studio
assert 'BackHandler(enabled = showQueue || expanded ||' in studio

# Shared links open in new UI with bounded text and no rotate replay.
assert 'handleIncomingShare(intent)' in studio
assert 'if (savedInstanceState == null) handleIncomingShare(intent)' in studio
assert 'override fun onNewIntent(intent: Intent)' in studio
assert 'Intent.ACTION_SEND' in studio and 'intent.type != "text/plain"' in studio
assert 'intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim().take(160)' in studio
assert 'VideoLink.normalize(shared)' in studio
assert 'playback.play(VideoPreview("Shared track"' in studio
assert 'nativeSearch(shared, SearchMode.SONGS)' in studio
assert 'LaunchedEffect(searchNavigationToken)' in studio

# No "preview" or "early access" UI entry remains in classic Home.
assert 'sectionTitle("Early access")' not in classic
assert 'Preview redesigned NightWave' not in classic
assert 'Return to NightWave' in classic
assert 'Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP' in classic

# Android on-device instrumentation enforces actual merged manifest routing.
assert instrumentation.count('@Test') == 2
assert 'launcherResolvesOnlyToPremiumNightWave' in instrumentation
assert 'sharedTextOpensPremiumAndClassicIsNotExternallyExposed' in instrumentation

# Storage, app namespace, and media session must remain undisturbed.
assert 'nightwave_offline_v1' in offline
assert 'private fun resumeOfflineSession()' in service
assert 'android:allowBackup="false"' in manifest
for label, source in (('studio', studio), ('classic', classic), ('service', service)):
    assert source.count('{') == source.count('}'), label + ' brace balance'
    imports = [ln for ln in source.splitlines() if ln.startswith('import ')]
    assert len(imports) == len(set(imports)), label + ' duplicate imports'

print('PASS: Phase 5M premium launch/share/settings, classic fallback, safe Back and media notification source wiring')
print('PENDING: Android Gradle compilation, instrumentation on emulator, in-place phone update and playback QA')
