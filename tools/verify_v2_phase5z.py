#!/usr/bin/env python3
"""5Z source contract for later CI execution. NOT live sync or Android verification."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
planner = read(base + "v2/cloud/CloudLibraryMerger.kt")
manager = read(base + "v2/data/LibraryArchiveManager.kt")
activity = read(base + "v2/ui/NightWaveStudioActivity.kt")
codec = read(base + "v2/data/LibraryBackupCodec.kt")
drive = read(base + "v2/cloud/NightWaveDriveClient.kt")
worker = read(base + "v2/cloud/NightWaveCloudWorker.kt")
gradle = read("app/build.gradle.kts")
manifest = read("app/src/main/AndroidManifest.xml")
workflow = read(".github/workflows/android.yml")
tests = read("app/src/test/java/com/nightwave/player/v2/cloud/CloudLibraryMergerTest.kt")

# Local-first, additive-only plan: never copy over a device's profile or history.
assert "object CloudLibraryMerger" in planner
assert "LibraryBackupCodec.decode(localBackup)" in planner
assert "LibraryBackupCodec.decode(LibraryBackupCodec.encode(remote))" in planner
assert "val mergedFavorites = local.favorites + additions" in planner
assert "val mergedPlaylists = local.playlists.toMutableList()" in planner
assert "merged = local.copy(" in planner
assert "favorites = mergedFavorites" in planner
assert "playlists = mergedPlaylists" in planner
assert 'availableImportedName(remoteName, names)' in planner
assert 'val alreadyImported = mergedPlaylists.any' in planner
assert "if (alreadyImported) continue" in planner
assert "require(mergedFavorites.size <= 500)" in planner
assert "require(mergedPlaylists.size <= 50)" in planner
assert "LibraryBackupCodec.decode(LibraryBackupCodec.encode(merged))" in planner
assert "LibraryBackupCodec.encode" in manager

# Atomic local before/after compare under the EXISTING transaction owner.
assert "fun mergeIfUnchanged(" in manager
assert "LegacyLibraryTransaction.exclusive {" in manager
assert "current.contentEquals(plan.expectedLocalBackup)" in manager
assert "CloudMergeApplyResult.STALE_PREVIEW" in manager
assert "restoreWithConfirmation(validated)" in manager
assert "validated.displayName != currentProfile.displayName" in manager
assert "validated.theme != currentProfile.theme" in manager
assert "validated.avatar != currentProfile.avatar" in manager
assert "restoreOldLibrary(backup)" in manager
assert "validatedHistory ?: library.recent()" in manager

# User picks an authenticated immutable snapshot, reviews, then confirms a merge.
assert 'requestCloudAction("merge")' in activity
assert '"preview", "merge" -> {' in activity
assert 'forMerge = action == "merge"' in activity
assert "CloudLibraryMerger.preview(" in activity
assert 'setTitle(if (plan.hasChanges) "Review library merge" else "Already up to date")' in activity
assert 'setPositiveButton("Merge into this device")' in activity
assert "archiveManager.mergeIfUnchanged(plan)" in activity
assert 'CloudMergeApplyResult.STALE_PREVIEW ->' in activity
assert 'StudioProfileRow("Compare & merge devices"' in activity
assert "cloudStore.read().account.equals(account, ignoreCase = true)" in activity

# Not a bidirectional live sync, not a Drive mutation, not an offline audio sync.
assert 'addQueryParameter("spaces", "appDataFolder")' in drive
assert 'uploadVerified' in drive
assert 'NightWaveCloudWorker' in worker
assert 'android:allowBackup="false"' in manifest
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert tests.count("@Test") >= 10
for name, source in (("planner", planner), ("manager", manager), ("activity", activity)):
    assert source.count("{") == source.count("}"), name
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
assert 'tools/verify_v2_phase5z.py' in workflow
assert ':app:compileDebugAndroidTestKotlin' in workflow
print("PASS: phase 5Z manual cross-device merge, non-destructive conflicts and stale-preview protection")
print("PENDING: Kotlin/JVM/Android build, Google OAuth and on-device QA")
