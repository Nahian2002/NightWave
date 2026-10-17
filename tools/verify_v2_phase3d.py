#!/usr/bin/env python3
"""NightWave Phase 3D source gate; NOT equivalent to compiling or running Android tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
code = root / "app/src/main/java/com/nightwave/player"
read = lambda path: path.read_text(encoding="utf-8")
music = read(code / "library/MusicLibrary.kt")
planner = read(code / "library/LibraryMutationPlan.kt")
writer = read(code / "library/LegacyLibraryTransaction.kt")
policy = read(code / "library/LibraryStoragePolicy.kt")
archive = read(code / "v2/data/LibraryArchiveManager.kt")
ui = read(code / "v2/ui/NightWaveStudioActivity.kt")
service = read(code / "playback/PlaybackService.kt")
gradle = read(root / "app/build.gradle.kts")
unit = read(root / "app/src/test/java/com/nightwave/player/library/LibraryMutationPlanTest.kt")
device_a = read(root / "app/src/androidTest/java/com/nightwave/player/library/SingleWriterBasicInstrumentedTest.kt")
device_b = read(root / "app/src/androidTest/java/com/nightwave/player/library/SingleWriterConcurrencyInstrumentedTest.kt")

assert any(f'versionName = "{v}"' in gradle for v in ("2.0.0-alpha3d", "2.0.0-alpha4", "2.0.0-alpha4-hotfix1", "2.0.0-alpha4-hotfix2", "2.0.0-alpha5a", "2.0.0-alpha5b", "2.0.0-alpha5c", "2.0.0-alpha5d", "2.0.0-alpha5e", "2.0.0-alpha5f", "2.0.0-alpha5g"))
assert any(f"versionCode = {v}" in gradle for v in (13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23))
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'ROOM_PRIMARY_WRITES_ENABLED = false' in policy
assert 'LEGACY_SINGLE_WRITER_ENABLED = true' in policy
assert 'check(!LibraryStoragePolicy.ROOM_PRIMARY_WRITES_ENABLED)' in writer
assert 'fun <T> exclusive(' in writer
assert 'prefs.edit().putString(' not in music
assert '.apply()' not in music
assert 'LegacyLibraryTransaction.mutate(app' in music
assert 'LegacyLibraryTransaction.exclusive' in archive
assert 'fun restoreWithConfirmation' in archive
assert 'LegacyLibrarySnapshotParser.parse(raw)' not in writer or 'readCollections(raw)' in writer
assert 'readCollections(raw)' in writer
assert 'LAST_AFTER = "last_after_fingerprint"' in writer
assert 'fingerprint(afterValues)' in writer
assert 'fingerprint(current)' in writer
assert 'if (!saved)' in writer
assert 'if (!editor.commit())' in writer
assert 'if (key !in newValues) editor.remove(key)' in writer
assert 'LegacyLibrarySnapshotParser.parse(previous)' in writer
assert 'UNDO MOST RECENT LIBRARY EDIT' in ui
assert 'confirmLastWriteRecovery()' in ui
assert 'recoverLastWriteIfUnchanged(this)' in ui
assert 'MediaSessionService' in service and 'Player.STATE_ENDED' in service
assert 'RoomShadowMigrator' in ui # Room still only mirror
assert unit.count('@Test') >= 10
assert device_a.count('@Test') >= 5
assert device_b.count('@Test') >= 2

for label, body in (('library', music), ('writer', writer), ('planner', planner),
                    ('archive', archive), ('ui', ui)):
    assert body.count('{') == body.count('}'), f'{label} brace counts differ'
    imports = [x for x in body.splitlines() if x.startswith('import ')]
    assert len(imports) == len(set(imports)), f'{label} duplicate imports'

root_xml = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
an = '{http://schemas.android.com/apk/res/android}name'
assert '.ui.MainActivity' in [x.attrib.get(an) for x in root_xml.iter('activity')]
assert '.v2.ui.NightWaveStudioActivity' in [x.attrib.get(an) for x in root_xml.iter('activity')]

print("PASS: single-writer source wiring, atomic commits, two-stage recovery guard,")
print("      explicit Room-write disable, backup import coordination, 17 test cases")
print("NOT VERIFIED: Android compilation, JVM test execution or instrumentation")
