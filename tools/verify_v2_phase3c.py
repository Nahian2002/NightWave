#!/usr/bin/env python3
"""Phase 3C static source checks. NOT a substitute for Gradle or on-device tests."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/com/nightwave/player"
test = root / "app/src/test/java/com/nightwave/player/v2/data/room"
instrumented = root / "app/src/androidTest/java/com/nightwave/player/v2/data/room"
read = lambda path: path.read_text(encoding="utf-8")

gradle = read(root / "app/build.gradle.kts")
dao = read(src / "v2/data/room/NightWaveDatabase.kt")
parity = read(src / "v2/data/room/LibraryReadParity.kt")
reader = read(src / "v2/data/room/VerifiedRoomReader.kt")
migrator = read(src / "v2/data/room/RoomShadowMigrator.kt")
ui = read(src / "v2/ui/NightWaveStudioActivity.kt")
old = read(src / "library/MusicLibrary.kt")
service = read(src / "playback/PlaybackService.kt")

assert any(f'versionName = "{v}"' in gradle for v in ("2.0.0-alpha3c", "2.0.0-alpha3d", "2.0.0-alpha4", "2.0.0-alpha4-hotfix1", "2.0.0-alpha4-hotfix2", "2.0.0-alpha5a", "2.0.0-alpha5b", "2.0.0-alpha5c", "2.0.0-alpha5d", "2.0.0-alpha5e", "2.0.0-alpha5f", "2.0.0-alpha5g", "2.0.0-alpha5h", "2.0.0-alpha5i", "2.0.0-alpha5j", "2.0.0-alpha5k", "2.0.0-alpha5l", "2.0.0-alpha5m", "2.0.0-alpha5n", "2.0.0-alpha5o", "2.0.0-alpha5p"))
assert any(f"versionCode = {code}" in gradle for code in (12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32))
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'fallbackToDestructiveMigration' not in dao.replace(
    "// Never use fallbackToDestructiveMigration.", ""
)
for name in ("readPlaylists", "readFavorites", "readRecent", "readPlaylistSongs"):
    assert f"suspend fun {name}(" in dao
assert "database.withTransaction" in reader and "database.withTransaction" in migrator
assert "Existing legacy library keys vanished" in migrator
assert "LegacyLibrarySnapshotParser.parse" in reader
assert "before.digest != after.digest" in reader
assert "checkpointDigest != expected.digest" in parity
for reason in ("SOURCE_CHANGED", "FAVORITES_DIFFER", "HISTORY_DIFFER",
               "PLAYLIST_NAMES_DIFFER", "PLAYLIST_TRACKS_DIFFER"):
    assert reason in parity
assert "val canUseRoom:" in reader
assert "if (parity.eligible) actual else null" in reader
assert "roomPreviewRequested = false" in ui
assert "roomPreviewActive = false" in ui
assert "Legacy fallback" in ui
assert "roomPreviewPlaylists" in ui
assert "TRY VERIFIED ROOM READS" in ui
assert "RETURN TO ORIGINAL LIBRARY" in ui
assert "LibraryReadParity" in parity
assert "MusicLibrary(this)" in ui
assert "MediaSessionService" in service and "Player.STATE_ENDED" in service
assert "prefs.edit()" not in reader
assert "libraryPrefs.edit()" not in reader

jvm = read(test / "LibraryReadParityTest.kt")
android = read(instrumented / "VerifiedRoomReaderInstrumentedTest.kt")
assert jvm.count("@Test") >= 10
assert android.count("@Test") >= 6
old_room = read(instrumented / "RoomShadowMigratorInstrumentedTest.kt")
assert "vanishedLegacyKeysCannotErasePreviouslyPopulatedRoomSnapshot" in old_room
assert "sameCountsButCorruptedRoomMetadataForcesFallback" in android
assert "newLegacyEditMakesRoomSnapshotIneligibleUntilReconcile" in android

for name, content in (("dao", dao), ("parity", parity),
                      ("reader", reader), ("activity", ui)):
    assert content.count("{") == content.count("}"), name
    imports = [line for line in content.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name

print("PASS: verified Room read routing, strict metadata/order parity, checkpoint")
print("      validation, legacy-only writes, automatic fallback and 17 added tests")
print("PENDING: Android/Kotlin/KSP build, JVM test run and device instrumentation")
