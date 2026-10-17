#!/usr/bin/env python3
"""Alpha4 Hotfix2 structural QA. Run Gradle and Android phone tests separately."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
code = root / "app/src/main/java/com/nightwave/player"
read = lambda p: p.read_text(encoding="utf-8")
gradle = read(root / "app/build.gradle.kts")
layout = read(root / "app/src/main/res/layout/activity_main.xml")
theme = read(code / "v2/ui/NightWaveTheme.kt")
classic = read(code / "ui/MainActivity.kt")
compose = read(code / "v2/ui/NightWaveStudioActivity.kt")
components = read(code / "v2/ui/StudioComponents.kt")
offline = read(code / "offline/OfflineAudioLibrary.kt")
offline_activity = read(code / "offline/OfflineLibraryActivity.kt")
service = read(code / "playback/PlaybackService.kt")

assert 'versionCode = 20' in gradle
assert 'versionName = "2.0.0-alpha5d"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'Color(raw.toInt())' in theme
assert 'Color(raw.toULong())' not in theme
assert '"Night"' in components and '"Wave"' in components
assert 'android:text="NightWave"' in layout
assert 'NIGHT MODE' not in layout
assert 'android:id="@+id/brandText"' in layout
assert 'android:id="@+id/miniPlayerRestore"' in layout
assert 'android:id="@+id/collapsePlayerButton"' in layout
assert 'height="56dp"' in layout or 'android:layout_height="56dp"' in layout
for field in ("brandText", "miniPlayerRestore", "collapsePlayerButton"):
    assert f"binding.{field}" in classic
assert "hideCompactPlayer()" in classic
assert "restoreCompactPlayer()" in classic
assert 'SpannableString("NightWave")' in classic
assert "OfflineLibraryActivity::class.java" in classic
assert "OfflineLibraryActivity::class.java" in compose
assert "onOfflineLibrary" in compose and "onOffline = onOfflineLibrary" in compose
assert "NightWaveStudioActivity::class.java" in classic
assert "MAX_SONG_BYTES" in offline and "MAX_LIBRARY_BYTES" in offline
assert "FileOutputStream(pending)" in offline and "renameTo(finalFile)" in offline
assert "prefs.edit().putString" in offline
assert "MAX_SONGS" in offline
assert 'openInputStream(uri)' in offline
assert "ActivityResultContracts.OpenMultipleDocuments()" in offline_activity
assert "ACTION_PLAY_OFFLINE" in offline_activity and "ACTION_PLAY_OFFLINE" in service
assert "DefaultDataSource.Factory(this, networkSource)" in service
assert "OfflineAudioLibrary(this)" in service
assert "MediaSessionService" in service
assert "if (offlinePlaying) return" in service
assert "MediaMetadata.Builder()" in service
assert "YouTubeGateway" not in offline

manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
a = "{http://schemas.android.com/apk/res/android}name"
activities = {item.attrib.get(a): item for item in manifest.iter("activity")}
assert ".ui.MainActivity" in activities
assert ".v2.ui.NightWaveStudioActivity" in activities
assert ".offline.OfflineLibraryActivity" in activities
assert activities[".offline.OfflineLibraryActivity"].attrib.get(
    "{http://schemas.android.com/apk/res/android}exported") == "false"
assert sum(
    1 for action in manifest.iter("action") if action.attrib.get(a) == "android.intent.action.MAIN"
) == 1
ET.parse(root / "app/src/main/res/layout/activity_main.xml")
ET.parse(root / "app/src/main/res/drawable/bg_nav_active.xml")

tests = read(root / "app/src/androidTest/java/com/nightwave/player/offline/OfflineAudioLibraryInstrumentedTest.kt")
assert tests.count("@Test") >= 3

for label, source in (("classic", classic), ("compose", compose),
                      ("offline data", offline), ("offline UI", offline_activity),
                      ("service", service), ("theme", theme)):
    assert source.count("{") == source.count("}"), f"{label}: unmatched braces"
    imports = [x for x in source.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), f"{label}: duplicate imports"

print("PASS: NightWave branding, responsive classic chrome, offline audio storage,")
print("      Media3 local playback, preview color correction, and new Android tests")
print("PENDING: Gradle compilation, instrumentation on disposable profile, phone QA")
