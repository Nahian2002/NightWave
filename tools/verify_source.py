#!/usr/bin/env python3
"""Offline source sanity checks. NOT a substitute for Android compilation/tests."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
main = root / "app/src/main"
xmls = sorted(main.rglob("*.xml"))
for path in xmls:
    ET.parse(path)

layout = (main / "res/layout/activity_main.xml").read_text(encoding="utf-8")
activity = (main / "java/com/nightwave/player/ui/MainActivity.kt").read_text(encoding="utf-8")
ids = set(re.findall(r'android:id="@\+id/(\w+)"', layout))
# Generated ActivityMainBinding uses the camel-cased XML IDs directly.
refs = set(re.findall(r'\bbinding\.(\w+)', activity)) - {"root", "ActivityMainBinding"}
missing = refs - {x[0].lower() + x[1:] for x in ids}
if missing:
    sys.exit(f"FAIL missing view binding IDs: {sorted(missing)}")

manifest = (main / "AndroidManifest.xml").read_text(encoding="utf-8")
for perm in ("INTERNET", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_MEDIA_PLAYBACK", "WAKE_LOCK"):
    assert 'android.permission.' + perm in manifest, perm
assert 'android:foregroundServiceType="mediaPlayback"' in manifest
assert 'androidx.media3.session.MediaLibraryService' in manifest
assert 'android.media.browse.MediaBrowserService' in manifest
assert 'android:usesCleartextTraffic="false"' in manifest

gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
assert 'com.github.TeamNewPipe:NewPipeExtractor:v0.26.5' in gradle
assert 'coreLibraryDesugaring(' in gradle
assert 'minSdk = 26' in gradle
workflow = (root / ".github/workflows/current-android.yml").read_text(encoding="utf-8")
assert 'gradle-version: ' in workflow and '8.11.1' in workflow
for task in (':app:testDebugUnitTest', ':app:assembleDebug',
             ':app:compileDebugAndroidTestKotlin'):
    assert task in workflow, 'Missing Android CI task: ' + task
assert 'pull_request:' in workflow
assert 'python tools/verify_current.py' in workflow
assert 'app/build/outputs/apk/debug/app-debug.apk' in workflow
print(f"PASS: {len(xmls)} XML files, {len(ids)} view IDs / {len(refs)} Kotlin view references")
print("PASS: manifest, secure transport flag, dependency/version and CI artifact wiring")
