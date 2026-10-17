#!/usr/bin/env python3
"""Phase 5U static privacy and safety gate (not Android compilation)."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
read = lambda name: (root / name).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
activity = read(base + "v2/ui/NightWaveStudioActivity.kt")
state = read(base + "v2/cloud/CloudBackupState.kt")
codec = read(base + "v2/cloud/CloudBackupCodec.kt")
drive = read(base + "v2/cloud/NightWaveDriveClient.kt")
auth = read(base + "v2/cloud/NightWaveCloudAuthorization.kt")
worker = read(base + "v2/cloud/NightWaveCloudWorker.kt")
archive = read(base + "v2/data/LibraryArchiveManager.kt")
existing = read(base + "v2/data/LibraryBackupCodec.kt")
gradle = read("app/build.gradle.kts")
manifest = read("app/src/main/AndroidManifest.xml")
workflow = read(".github/workflows/android.yml")
tests = read("app/src/test/java/com/nightwave/player/v2/cloud/CloudBackupCodecTest.kt")

assert 'implementation("com.google.android.gms:play-services-auth:21.4.0")' in gradle
assert 'implementation("androidx.work:work-runtime-ktx:2.10.4")' in gradle
assert 'android.permission.ACCESS_NETWORK_STATE' in manifest
assert manifest.count("android.intent.category.LAUNCHER") == 1
assert 'android:allowBackup="false"' in manifest
ET.parse(root / "app/src/main/AndroidManifest.xml")

# No broad Drive permissions or service credentials.
assert 'https://www.googleapis.com/auth/drive.appdata' in auth
for forbidden in ('https://www.googleapis.com/auth/drive"', 'drive.readonly',
                  'requestOfflineAccess(', 'client_secret', 'refresh_token',
                  'GoogleSignInOptions.DEFAULT_SIGN_IN'):
    assert forbidden not in auth + drive + worker, forbidden
assert 'setAccount(Account(accountName, "com.google"))' in auth
assert 'requireToken(result: AuthorizationResult)' in auth
assert 'authorizedAccount(result: AuthorizationResult)' in auth
assert '"Bearer $accessToken"' in drive
assert 'getSharedPreferences(' not in drive
assert '"accessToken"' not in state
assert '"refreshToken"' not in state

# Only app data, copy-on-write snapshot, remote reread and checksum.
assert 'addQueryParameter("spaces", "appDataFolder")' in drive
assert 'JSONArray().put("appDataFolder")' in drive
assert 'uploadType", "multipart"' in drive
assert 'contentEquals(bytes)' in drive
assert 'CloudBackupCodec.decode(remote)' in drive
assert 'fun download(id: String)' in drive
for dangerous in ('delete(', 'uploadType", "media"', 'PATCH', 'https://www.googleapis.com/auth/drive '):
    assert dangerous not in drive
assert 'digest(payload)' in codec
assert 'require(bytes.size in 1..MAX_BYTES)' in codec
assert 'LibraryBackupCodec.decode(manualBackup)' in codec
assert 'val recent = if (includeHistory)' in codec
assert 'validatedHistory(recent)' in codec
assert 'validate' in codec.lower()

# Never enable periodic sync until after explicit manual verified backup.
assert '.putBoolean("enabled", false)' in state
assert 'fun success(account: String, whenMs: Long)' in state
assert 'lastSuccessMs <= 0L' in worker
assert 'NetworkType.UNMETERED' in worker
assert 'PeriodicWorkRequestBuilder<NightWaveCloudWorker>(24, TimeUnit.HOURS)' in worker
assert 'authorize(NightWaveCloudAuthorization.request(settings.account))' in worker
assert 'if (authorization.hasResolution())' in worker
assert 'store.read().account != settings.account' in worker
assert 'ExistingPeriodicWorkPolicy.UPDATE' in worker

# Manual export and confirmed restore remain intact.
assert 'fun export(): ByteArray = LegacyLibraryTransaction.exclusive' in archive
assert 'fun restoreWithConfirmation(' in archive
assert 'restoredHistory: List<VideoPreview>? = null' in archive
assert 'validatedHistory ?: library.recent()' in archive
assert 'if (!change.commit()) return@exclusive false' in archive
assert 'restoreOldLibrary(backup)' in archive
assert 'const val VERSION = 1' in existing

assert 'ActivityResultContracts.StartIntentSenderForResult()' in activity
assert 'cloudPendingAction = action' in activity
assert 'requestCloudAction("connect")' in activity
assert 'requestCloudAction("backup")' in activity
assert 'requestCloudAction("preview")' in activity
assert 'drive.listSnapshots().takeIf { it.isNotEmpty() }' in activity
assert 'showCloudSnapshotPicker(preview, token)' in activity
assert 'setTitle("Choose a cloud backup")' in activity
assert 'CloudBackupCodec.decode(NightWaveDriveClient(token).download(chosen.id))' in activity
assert 'setTitle("Review cloud restore")' in activity
assert 'setNegativeButton("Cancel", null)' in activity
assert 'restoreCloudAfterConfirmation(preview, false)' in activity
assert 'restoreCloudAfterConfirmation(preview, true)' in activity
assert 'cloudStore.disconnect()' in activity
assert 'NightWaveCloudSchedule.stop(this)' in activity
assert 'cloudStore.success(expectedAccount, System.currentTimeMillis())' in activity
assert 'onCloudHistory = { enabled ->' in activity
assert 'Cloud backup uploaded and verified.' in activity
assert 'onExportGuest = { exportFile.launch(' in activity
assert 'onImportGuest = { importFile.launch(' in activity
assert 'SavedTrackSearch.match(query, savedTracks)' in activity

assert tests.count('@Test') >= 5
for name, text in [('ui', activity), ('worker', worker), ('codec', codec),
                   ('rest client', drive), ('auth', auth), ('state', state)]:
    assert text.count("{") == text.count("}"), name + ' brace balance'
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + ' duplicated imports'

assert "tools/verify_v2_phase5u.py" in workflow
assert 'feature/nightwave-v2-phase5t-reliable-discovery' in workflow
assert ':app:compileDebugAndroidTestKotlin' in workflow

print("PASS: 5U opt-in private Drive snapshots, integrity, scoped auth and explicit restore safeguards")
print("PENDING: Google OAuth project/SHA, Android Gradle, Google Drive E2E and device QA")
