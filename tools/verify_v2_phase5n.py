#!/usr/bin/env python3
"""NightWave Phase 5N compact premium profile source checks.

Does not execute Compose, Android Gradle or a phone. Requires on-device
accessibility/scaling tests before shipping.
"""
from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
read = lambda p: (root / p).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
profile = read(base + "v2/ui/NightWaveStudioActivity.kt")
gradle = read("app/build.gradle.kts")
manifest = read("app/src/main/AndroidManifest.xml")
media = read(base + "playback/PlaybackService.kt")
library = read(base + "offline/OfflineAudioLibrary.kt")
builder = read("tools/build-windows.ps1")
device = read("tools/check-device-update.ps1")

assert 'versionCode = 31' in gradle
assert 'versionName = "2.0.0-alpha5o"' in gradle
assert "-VersionName '2.0.0-alpha5o' -VersionCode '31'" in builder
assert "$versionName = '2.0.0-alpha5o'" in device
assert '$versionCode = 31' in device

# Visual structure: compact rather than 50dp-per-setting buttons and huge
# unstructured sections. Advanced tools must be collapsed at first opening.
assert 'private fun StudioProfileGroup(' in profile
assert 'private fun StudioProfileRow(' in profile
assert 'var editing by rememberSaveable { mutableStateOf(false) }' in profile
assert 'var showAppearance by rememberSaveable { mutableStateOf(false) }' in profile
assert 'var showBackup by rememberSaveable { mutableStateOf(false) }' in profile
assert 'var showAdvanced by rememberSaveable { mutableStateOf(false) }' in profile
assert 'PaddingValues(horizontal = 16.dp, vertical = 12.dp)' in profile
assert 'Arrangement.spacedBy(10.dp)' in profile
for title in ('Your space', 'Appearance & playback', 'Library & backup',
              'App health', 'Interface'):
    assert title in profile, title
assert 'StudioProfileGroup("Appearance & playback")' in profile
assert 'StudioProfileGroup("Library & backup")' in profile

# Profile editing is explicit, has nonblank bounds, retains local avatar/theme.
assert 'Save profile' in profile
assert 'name.trim().isNotEmpty()' in profile
assert 'save(profile.copy(displayName = updated))' in profile
assert 'ProfileSettings.allowedAvatars.sorted()' in profile
assert 'save(profile.copy(avatarStyle = style))' in profile
assert 'NightWaveTheme.entries.chunked(2)' in profile
assert 'save(profile.copy(theme = option))' in profile
assert 'save(profile.copy(motion =' in profile

# All legacy library backup, restore, undo, Room preview and fallback actions
# remain connected, but optional controls are collapsed by default.
assert 'onExport: () -> Unit, onImport: () -> Unit' in profile
assert 'onRecoverLastChange: () -> Unit' in profile
assert 'StudioProfileRow("Export music library"' in profile
assert 'StudioProfileRow("Restore from backup"' in profile
assert 'StudioProfileRow("Undo most recent library edit"' in profile
assert 'onCheckStorage' in profile
assert 'TRY VERIFIED ROOM READS' in profile
assert 'RETURN TO ORIGINAL LIBRARY' in profile
assert 'if (showAdvanced) {' in profile
assert 'if (showBackup) {' in profile
assert 'Room and DataStore are optional read-only mirrors' in profile
assert 'StudioProfileRow("Crash diagnostics"' in profile
assert 'StudioProfileRow("Open classic interface"' in profile

# Music identity, working premium launcher, offline audio and media service
# remain untouched in this phase.
assert 'nightwave_offline_v1' in library
assert 'private fun resumeOfflineSession()' in media
assert 'NightWaveStudioActivity::class.java' in media
assert manifest.count('android.intent.category.LAUNCHER') == 1
assert 'android:allowBackup="false"' in manifest
ET.parse(root / "app/src/main/AndroidManifest.xml")

imports = [l for l in profile.splitlines() if l.startswith('import ')]
assert len(imports) == len(set(imports))
assert profile.count('{') == profile.count('}')
print('PASS: Phase 5N compact You sections, existing profile/library actions and release wiring')
print('PENDING: Android Compose compilation, visual/font scaling and phone QA')
