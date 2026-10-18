#!/usr/bin/env python3
"""6Q2 backup-history + transaction-locked recovery source gate; not Android QA."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def read(path):return (root/path).read_text(encoding="utf-8")
prefix="app/src/main/java/com/nightwave/player/"
drive=read(prefix+"v2/cloud/NightWaveDriveClient.kt")
recovery=read(prefix+"v2/cloud/CloudRecoveryPolicy.kt")
manager=read(prefix+"v2/data/LibraryArchiveManager.kt")
activity=read(prefix+"v2/ui/NightWaveStudioActivity.kt")
codec=read(prefix+"v2/cloud/CloudBackupCodec.kt")
auth=read(prefix+"v2/cloud/NightWaveCloudAuthorization.kt")
tests=read("app/src/test/java/com/nightwave/player/v2/cloud/CloudRecoveryPolicyTest.kt")
older=read("tools/verify_v2_phase5u.py")
workflow=read(".github/workflows/android.yml")
gradle=read("app/build.gradle.kts")
manifest=read("app/src/main/AndroidManifest.xml")

assert "data class DriveSnapshot(" in drive
assert "val sizeBytes: Long? = null" in drive
assert "fun listSnapshots(): List<DriveSnapshot>" in drive
assert "repeat(3)" in drive
assert 'addQueryParameter("spaces", "appDataFolder")' in drive
assert 'addQueryParameter("fields", "nextPageToken,files(id,name,size)")' in drive
assert 'addQueryParameter("pageSize", "100")' in drive
assert 'builder.addQueryParameter("pageToken", it)' in drive
assert 'seenIds.add(id)' in drive
assert 'seenTokens.add(upcoming)' in drive
assert 'expectedName.matches(name)' in drive
assert 'it in 1..CloudBackupCodec.MAX_BYTES.toLong()' in drive
assert "output.sortedByDescending { it.name }" in drive
assert "fun download(id: String): ByteArray" in drive
assert "CloudBackupCodec.decode(remote)" in drive
assert "remote.contentEquals(bytes)" in drive
assert not any(x in drive for x in ("delete(", "PATCH", "getSharedPreferences("))
assert "object CloudRecoveryPolicy" in recovery
assert "fun inspect(localBytes: ByteArray, remote: CloudBackupPreview)" in recovery
assert "LibraryBackupCodec.decode(localBytes)" in recovery
assert "LibraryBackupCodec.decode(LibraryBackupCodec.encode(remote.archive))" in recovery
assert "val remoteLikes = cloud.favorites.map { it.url }.toHashSet()" in recovery
assert "localPlaylistsNotInCloud = lostLists" in recovery
assert "localPlaylistsWithChangedContent = changedLists" in recovery
assert "replacement.second != songs" in recovery
assert "profileWouldChange = local.displayName != cloud.displayName" in recovery
assert "CloudBackupCodec.validatedHistory(remote.history)" in recovery
assert "expectedLocalBackup = localBytes.copyOf()" in recovery
assert "fun restoreCloudIfUnchanged(" in manager
assert "CloudRestoreApplyResult = LegacyLibraryTransaction.exclusive" in manager
assert "current.contentEquals(expectedLocalBackup)" in manager
assert "CloudRestoreApplyResult.STALE_PREVIEW" in manager
assert "restoreWithConfirmation(archive, restoredHistory)" in manager
assert "fun restoreWithConfirmation(" in manager
assert "restoreOldLibrary(backup)" in manager
assert "enum class CloudRestoreApplyResult { APPLIED, STALE_PREVIEW, FAILED }" in manager
assert "val options = snapshots.take(50)" in activity
assert 'Choose a cloud backup (' in activity
assert "snapshot.sizeBytes?.let" in activity
assert "CloudRecoveryPolicy.inspect(localBytes, remote)" in activity
assert "Triple(remote, plan, comparison)" in activity
assert "showCloudRestorePreview(remote, comparison, expectedAccount)" in activity
assert "localLikedNotInCloud" in activity
assert "localPlaylistsNotInCloud" in activity
assert "localPlaylistsWithChangedContent" in activity
assert "profileWouldChange" in activity
assert "Offline audio and imported LRC lyrics are not included" in activity
assert "restoreCloudAfterConfirmation(preview, report, false, account)" in activity
assert "restoreCloudAfterConfirmation(preview, report, true, account)" in activity
assert "archiveManager.restoreCloudIfUnchanged(" in activity
assert "withContext(Dispatchers.IO)" in activity
assert "CloudRestoreApplyResult.STALE_PREVIEW" in activity
assert "CloudRestoreApplyResult.APPLIED" in activity
assert 'requestCloudAction("backup")' in activity
assert 'requestCloudAction("merge")' in activity
assert "showCloudMergePreview(plan, expectedAccount)" in activity
assert "restoreCloudAfterConfirmation(preview, report, false, account)" in older
assert "restoreCloudAfterConfirmation(preview, report, true, account)" in older
assert "MAX_BYTES = 3_000_000" in codec
assert "https://www.googleapis.com/auth/drive.appdata" in auth
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert 'android:allowBackup="false"' in manifest
assert tests.count("@Test") >= 16
assert "tools/verify_v2_phase6q2.py" in workflow
assert "feature/nightwave-v2-phase6q1-smart-drive-snapshots" in workflow
for name,s in [("drive",drive),("recovery",recovery),("manager",manager),
               ("activity",activity),("tests",tests)]:
    assert s.count("{")==s.count("}"),name
    imports=[line for line in s.splitlines() if line.startswith("import ")]
    assert len(imports)==len(set(imports)),name
print("PASS: 6Q2 bounded cloud history, preview-only comparison and lock-fenced recovery")
print("PENDING: Android build, JVM execution and live Google Drive restore QA")
