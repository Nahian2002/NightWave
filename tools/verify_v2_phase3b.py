#!/usr/bin/env python3
"""Phase 3B source gate; actual Room compilation requires Android/KSP Gradle."""
from pathlib import Path
import xml.etree.ElementTree as ET

base = Path(__file__).resolve().parents[1]
java = base / "app/src/main/java/com/nightwave/player"
test = base / "app/src/test/java/com/nightwave/player/v2/data/room"
android_test = base / "app/src/androidTest/java/com/nightwave/player/v2/data"
read = lambda p: p.read_text(encoding="utf-8")
root_gradle = read(base / "build.gradle.kts")
app_gradle = read(base / "app/build.gradle.kts")
db = read(java / "v2/data/room/NightWaveDatabase.kt")
parser = read(java / "v2/data/room/LegacyLibrarySnapshot.kt")
migrator = read(java / "v2/data/room/RoomShadowMigrator.kt")
store = read(java / "v2/data/ProfileDataStoreMirror.kt")
ui = read(java / "v2/ui/NightWaveStudioActivity.kt")
legacy = read(java / "library/MusicLibrary.kt")
service = read(java / "playback/PlaybackService.kt")

assert 'id("com.google.devtools.ksp") version "2.0.21-1.0.28"' in root_gradle
assert 'id("com.google.devtools.ksp")' in app_gradle
assert 'room-runtime:2.7.2' in app_gradle and 'room-compiler:2.7.2' in app_gradle
assert 'datastore-preferences:1.1.7' in app_gradle
assert 'room.schemaLocation' in app_gradle
assert any(f'versionName = "{v}"' in app_gradle for v in ("2.0.0-alpha3b", "2.0.0-alpha3c", "2.0.0-alpha3d", "2.0.0-alpha4", "2.0.0-alpha4-hotfix1", "2.0.0-alpha4-hotfix2", "2.0.0-alpha5a", "2.0.0-alpha5b", "2.0.0-alpha5c", "2.0.0-alpha5d", "2.0.0-alpha5e", "2.0.0-alpha5f", "2.0.0-alpha5g"))
assert 'applicationId = "com.nightwave.player"' in app_gradle
assert 'fallbackToDestructiveMigration' not in db.replace(
    '// Never use fallbackToDestructiveMigration.', ''
)
for table in ("saved_track", "playlist", "playlist_entry", "favorite", "recent", "migration_checkpoint"):
    assert f'tableName = "{table}"' in db
assert "version = 1" in db and "exportSchema = true" in db
assert "withTransaction" in migrator
assert "dao.saveCheckpoint(" in migrator and "dao.clearTracks()" in migrator
assert "LegacyLibrarySnapshotParser.parse" in migrator
assert "SharedPreferencesMigration" not in store
assert "nightwave_v2_guest_datastore" in store
assert "nightwave_v2_guest_profile_1" in read(java / "v2/data/GuestProfileStore.kt")
assert "RoomShadowMigrator(this, NightWaveDatabase.get(this))" in ui
assert "ProfileDataStoreMirror(this)" in ui
assert "Original library remains in use." in ui
assert "if (::roomShadow.isInitialized)" in ui
assert "fun toggleFavorite" in legacy
assert "MediaSessionService" in service and "Player.STATE_ENDED" in service
assert "onSaveProfile" in ui and "refreshShadows()" in ui
assert (test / "LegacyLibrarySnapshotParserTest.kt").exists()
assert read(test / "LegacyLibrarySnapshotParserTest.kt").count("@Test") >= 8
room_test = read(android_test / "room/RoomShadowMigratorInstrumentedTest.kt")
profile_test = read(android_test / "ProfileDataStoreMirrorInstrumentedTest.kt")
assert room_test.count("@Test") >= 5
assert profile_test.count("@Test") >= 1
assert "sqliteAbortRollsBackRowsAndCheckpoint" in room_test

root = ET.parse(base / "app/src/main/AndroidManifest.xml").getroot()
android_name = "{http://schemas.android.com/apk/res/android}name"
activities = {x.attrib.get(android_name) for x in root.iter("activity")}
assert ".ui.MainActivity" in activities
assert ".v2.ui.NightWaveStudioActivity" in activities
assert "ACTION_PLAY_QUEUE" in service

for name, source in (("db", db), ("parser", parser), ("migrator", migrator),
                     ("datastore", store), ("activity", ui)):
    assert source.count("{") == source.count("}"), f"{name}: brace count differs"
    imports = [x for x in source.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), f"{name}: duplicate imports"

print("PASS: Room/KSP/DataStore wiring, legacy preservation, atomic checkpoints,")
print("      schema versioning, Compose migration UI, and 14 supplied test cases")
print("NOT VERIFIED: Android Gradle compilation and execution of JVM/device tests")
