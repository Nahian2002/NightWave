#!/usr/bin/env python3
"""Phase 6R1 narrow Drive audio upload source gate. Not a verified APK."""
from pathlib import Path
import xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
def src(p):return (r/p).read_text(encoding="utf-8")
b="app/src/main/java/com/nightwave/player/"
policy=src(b+"v2/locker/LockerAudioPolicy.kt")
auth=src(b+"v2/locker/NightWaveLockerAuthorization.kt")
client=src(b+"v2/locker/NightWaveMusicDriveClient.kt")
ui=src(b+"v2/locker/NightWaveLockerActivity.kt")
offline=src(b+"offline/OfflineLibraryActivity.kt")
local=src(b+"offline/OfflineAudioLibrary.kt")
backupAuth=src(b+"v2/cloud/NightWaveCloudAuthorization.kt")
backupClient=src(b+"v2/cloud/NightWaveDriveClient.kt")
tests=src("app/src/test/java/com/nightwave/player/v2/locker/LockerAudioPolicyTest.kt")
manifest=src("app/src/main/AndroidManifest.xml")
gradle=src("app/build.gradle.kts")
ci=src(".github/workflows/android.yml")
assert 'SCOPE = "https://www.googleapis.com/auth/drive.file"' in auth
assert 'SCOPE = "https://www.googleapis.com/auth/drive.appdata"' in backupAuth
assert '"nightwave_music_locker_v1"' in auth
assert 'setRequestedScopes(listOf(Scope(SCOPE)))' in auth
assert 'check(!result.hasResolution() && SCOPE in result.grantedScopes)' in auth
assert 'fun disconnect(): Boolean' in auth
assert 'fun validateLocal(song: OfflineSong, file: File)' in policy
assert 'file.isFile && file.canRead() && file.length() == song.bytes' in policy
assert 'MAX_FILE_BYTES = 250L * 1024L * 1024L' in policy
assert 'CHUNK_BYTES = 1024 * 1024' in policy
assert 'MessageDigest.getInstance("SHA-256")' in policy
assert 'MessageDigest.getInstance("MD5")' in policy
assert 'privateId = Regex' in policy
assert 'suffixToMime[extension] == song.mimeType' in policy
assert 'fun list(): List<LockerRemoteTrack>' in client
assert 'folder(createIfMissing = false)?.let { listInFolder(it) }.orEmpty()' in client
assert 'folder(createIfMissing = true)' in client
assert '!Thread.currentThread().isInterrupted' in client
assert 'fun upload(song: OfflineSong, file: File, progress: (Int) -> Unit)' in client
assert 'LockerAudioPolicy.validateLocal(song, file)' in client
assert 'val (sha256, md5) = LockerAudioPolicy.hashes(file)' in client
assert 'LockerAudioPolicy.FOLDER_NAME' in client
assert 'application/vnd.google-apps.folder' in client
assert 'repeat(2)' in client
assert 'addQueryParameter("pageSize", "100")' in client
assert 'nightwave_sha256' in client and 'nightwave_local_id' in client
assert 'addQueryParameter("uploadType", "resumable")' in client
assert '"X-Upload-Content-Type"' in client
assert '"X-Upload-Content-Length"' in client
assert 'header("Content-Range", "bytes $offset-$end/' in client
assert 'if (response.code == 308)' in client
assert 'accepted > offset && accepted <= end + 1' in client
assert 'session.isHttps' in client
assert 'session.host == "www.googleapis.com"' in client
assert 'row.md5.equals(md5, true)' in client
assert 'row.sha256 == sha256' in client
assert 'result.optJSONArray("parents")?.optString(0) == parent' in client
assert 'LockerUploadOutcome(existing, created = false)' in client
assert 'return LockerUploadOutcome(row, created = true)' in client
assert not any(x in client for x in ('delete(', 'PATCH', 'auth/drive"'))
assert '.addQueryParameter("spaces", "appDataFolder")' not in client
assert 'JSONArray().put("appDataFolder")' not in client
assert 'Identity.getAuthorizationClient(this)' in ui
assert 'ActivityResultContracts.StartIntentSenderForResult()' in ui
assert 'NightWaveLockerAuthorization.request(' in ui
assert 'state.connect(name)' in ui
assert 'fun begin(action: String)' in ui
assert 'client.upload(song, file)' in ui
assert 'client.list()' in ui
assert 'selected = runCatching { offline.find(id) }.getOrNull()' in ui
assert 'offline.fileFor(song)' in ui
assert 'Cloud-to-phone downloads are not yet enabled.' in ui
assert 'putExtra(NightWaveLockerActivity.EXTRA_OFFLINE_ID, song.id)' in offline
assert 'Intent(this@OfflineLibraryActivity, NightWaveLockerActivity::class.java)' in offline
assert 'ACTION_PLAY_OFFLINE' in offline
assert 'fun import(uri: Uri): OfflineSong' in local
assert 'appDataFolder' in backupClient
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert '<activity android:name=".v2.locker.NightWaveLockerActivity" android:exported="false" />' in manifest
assert manifest.count('android.intent.category.LAUNCHER') == 1
assert 'android:allowBackup="false"' in manifest
assert 'tools/verify_v2_phase6r1.py' in ci
assert 'feature/nightwave-v2-phase6q3-optional-settings-lyrics-backup' in ci
assert tests.count('@Test') >= 17
ET.parse(r/"app/src/main/AndroidManifest.xml")
for name,text in [("client",client),("policy",policy),("auth",auth),
                  ("ui",ui),("offline",offline),("tests",tests)]:
    assert text.count("{")==text.count("}"),name
    imports=[line for line in text.splitlines() if line.startswith("import ")]
    assert len(imports)==len(set(imports)),name
print("PASS: R1 explicitly authorized personal Drive audio upload and safe source boundaries")
print("PENDING: Kotlin/JUnit/Google OAuth/live upload/Android device and signed build")
