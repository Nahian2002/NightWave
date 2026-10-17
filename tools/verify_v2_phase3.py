#!/usr/bin/env python3
"""NightWave v2 Phase 3 static gate. No Android build or runtime claim."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
java = root / "app/src/main/java/com/nightwave/player"
read = lambda p: (java / p).read_text(encoding="utf-8")

files = {
    "codec": read("v2/backup/BackupFormat.kt"),
    "merge": read("v2/backup/BackupMerge.kt"),
    "coordinator": read("v2/backup/GuestBackupRepository.kt"),
    "music": read("library/MusicLibrary.kt"),
    "room": read("v2/data/NightWaveDatabase.kt"),
    "mirror": read("v2/data/RoomMirrorRepository.kt"),
    "datastore": read("v2/data/GuestSettingsMirror.kt"),
    "activity": read("v2/ui/NightWaveStudioActivity.kt"),
    "profile": read("v2/model/ProfileModel.kt")
}
gradle = (root / "app/build.gradle.kts").read_text()
root_gradle = (root / "build.gradle.kts").read_text()

assert 'versionName = "2.0.0-alpha3"' in gradle
assert 'versionCode = 10' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert "2.0.21-1.0.28" in root_gradle
assert "ksp(" in gradle and "room-compiler:2.6.1" in gradle
assert "room-runtime:2.6.1" in gradle
assert "datastore-preferences:1.1.1" in gradle
assert 'room.schemaLocation' in gradle

assert all(x in files["codec"] for x in (
    "MAX_BYTES", "checksum_sha256", "MessageDigest.isEqual",
    "VideoLink.normalize(", "InvalidBackupException",
    "include", "validArtwork", "requireKeys("
))
assert "RefreshToken" not in files["codec"]
assert "audioUrl" not in files["codec"] and "access_token" not in files["codec"]
assert all(x in files["merge"] for x in (
    "KEEP_EXISTING", "REPLACE_LIBRARY", "MERGE_LIBRARY", "InvalidBackupException"
))
assert all(x in files["coordinator"] for x in (
    "nightwave_v2_pending_restore_rescue.json", "recoverPendingRestore()",
    "commitImportedLibrary", "FileOutputStream(rescue)",
    "out.fd.sync()", "GuestProfileStore", "rescue.delete()"
))
assert "prefs.edit()" in files["music"] and ".commit()" in files["music"]
assert "snapshotForBackup()" in files["music"]
assert "commitImportedLibrary(" in files["music"]
assert "@Database" in files["room"] and "version = 1" in files["room"]
assert "withTransaction" in files["mirror"] and "actual != snapshot" in files["mirror"]
assert "preferencesDataStore" in files["datastore"] and "suspend fun matches" in files["datastore"]
assert "nightwave_v2_settings_shadow" in files["datastore"]
assert "RoomMirrorRepository(applicationContext).stage" in files["activity"]
assert "GuestSettingsMirror(applicationContext).stage" in files["activity"]
assert "ActivityResultContracts.CreateDocument" in files["activity"]
assert "ActivityResultContracts.OpenDocument" in files["activity"]
assert "Dispatchers.IO" in files["activity"]
assert "profile.historyPrivacy == ListeningPrivacy.INCLUDE_IN_BACKUP" in files["activity"]
assert "ImportStrategy.REPLACE_LIBRARY" in files["activity"]
assert "ImportStrategy.MERGE_LIBRARY" in files["activity"]
assert "import androidx.compose.runtime.saveable.rememberSaveable" in files["activity"]
assert "GoogleSignIn" not in files["activity"] and "signInWithCredential" not in files["activity"]

for source_name, source in files.items():
    assert source.count("{") == source.count("}"), source_name + ": unmatched braces"
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), source_name + ": duplicate imports"

unit = root / "app/src/test/java/com/nightwave/player/v2/backup"
n = sum((unit / name).read_text().count("@Test")
        for name in ("BackupFormatTest.kt", "BackupMergeTest.kt"))
assert n >= 12, f"Expected at least 12 portable-backup tests, found {n}"
android_test = root / "app/src/androidTest/java/com/nightwave/player/v2/data/RoomMirrorMigrationTest.kt"
assert android_test.exists()
assert android_test.read_text().count("@Test") >= 2

print("PASS: Phase 3 source and privacy checks, opt-in SAF UI,")
print("      Room/DataStore staging and rollback-journal structure")
print("PENDING: Android Gradle compilation, JVM test execution,")
print("         instrumented Room tests, actual import/restore on phone")
