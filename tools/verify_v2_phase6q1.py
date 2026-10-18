#!/usr/bin/env python3
"""6Q1 source contracts for smart verified Drive backups; NOT cloud end-to-end tests."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
def src(p):return (r/p).read_text(encoding="utf-8")
base="app/src/main/java/com/nightwave/player/"
matcher=src(base+"v2/cloud/CloudSnapshotPolicy.kt")
client=src(base+"v2/cloud/NightWaveDriveClient.kt")
codec=src(base+"v2/cloud/CloudBackupCodec.kt")
worker=src(base+"v2/cloud/NightWaveCloudWorker.kt")
state=src(base+"v2/cloud/CloudBackupState.kt")
auth=src(base+"v2/cloud/NightWaveCloudAuthorization.kt")
activity=src(base+"v2/ui/NightWaveStudioActivity.kt")
test=src("app/src/test/java/com/nightwave/player/v2/cloud/CloudSnapshotPolicyTest.kt")
ci=src(".github/workflows/android.yml")
gradle=src("app/build.gradle.kts")
manifest=src("app/src/main/AndroidManifest.xml")

assert "object CloudSnapshotPolicy" in matcher
assert "CloudBackupCodec.decode(expected)" in matcher
assert "wanted == CloudBackupCodec.decode(remote)" in matcher
assert "catch (_: Exception)" in matcher
assert "data class DriveBackupOutcome(val snapshot: DriveSnapshot, val created: Boolean)" in client
assert "fun backUpIfChangedVerified(bytes: ByteArray): DriveBackupOutcome" in client
assert "CloudBackupCodec.decode(bytes)" in client
assert "listSnapshots().firstOrNull()" in client
assert "runCatching { download(latest.id) }.getOrNull()" in client
assert "CloudSnapshotPolicy.sameVerifiedContent(bytes, remote)" in client
assert "return DriveBackupOutcome(latest, created = false)" in client
assert "return DriveBackupOutcome(uploadVerified(bytes), created = true)" in client
assert "val remote = download(id)" in client
assert "remote.contentEquals(bytes)" in client
assert "CloudBackupCodec.decode(remote)" in client
assert 'addQueryParameter("spaces", "appDataFolder")' in client
assert 'JSONArray().put("appDataFolder")' in client
assert '"Bearer $accessToken"' in client
assert "https://www.googleapis.com/auth/drive.appdata" in auth
assert "data class Backup(val created: Boolean)" in activity
assert "data class Snapshots(val items: List<DriveSnapshot>)" in activity
assert "drive.backUpIfChangedVerified(bytes)" in activity
assert "latestSettings.includeHistory == settings.includeHistory" in activity
assert "showCloudSnapshotPicker(outcome.items, token," in activity
assert "cloudStore.success(expectedAccount, System.currentTimeMillis())" in activity
assert 'Cloud backup already current; nothing uploaded.' in activity
assert 'Cloud backup uploaded and verified.' in activity
assert "NightWaveCloudSchedule.update(this@NightWaveStudioActivity)" in activity
assert "NightWaveDriveClient(token).backUpIfChangedVerified(payload)" in worker
assert "beforeUpload.includeHistory != settings.includeHistory" in worker
assert "beforeUpload.wifiOnly != settings.wifiOnly" in worker
assert 'store.status("Already backed up; library unchanged")' in worker
assert "lastSuccessMs <= 0L" in worker
assert "NetworkType.UNMETERED" in worker
assert ".putBoolean(\"enabled\", false)" in state
assert '"nightwave_cloud_backup_v1"' in state
assert "MAX_BYTES = 3_000_000" in codec
assert "digest(payload)" in codec
assert 'android:allowBackup="false"' in manifest
assert "tools/verify_v2_phase6q1.py" in ci
assert "feature/nightwave-v2-phase6p-integration-build-gate" in ci
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert test.count("@Test") >= 14
for name,text in (("matcher",matcher),("client",client),("worker",worker),("activity",activity),("tests",test)):
    assert text.count("{")==text.count("}"),name
    imports=[ln for ln in text.splitlines() if ln.startswith("import ")]
    assert len(imports)==len(set(imports)),name
for forbidden in ('getSharedPreferences(', 'DELETE', 'PATCH', 'refresh_token', 'client_secret'):
    assert forbidden not in client,forbidden
for forbidden in ('HttpURLConnection', 'SharedPreferences', 'delete(', 'fileFor(', 'getFilesDir('):
    assert forbidden not in matcher,forbidden
print("PASS: 6Q1 no-deletion Google Drive dedupe with verified remote-content comparison")
print("PENDING: full Gradle/JVM execution, Google consent and Drive E2E/device verification")
