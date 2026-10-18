#!/usr/bin/env python3
"""6R2 cloud-to-offline import preservation; source checks are NOT Android tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
client = read(base+"v2/locker/NightWaveMusicDriveClient.kt")
policy = read(base+"v2/locker/LockerAudioPolicy.kt")
ui = read(base+"v2/locker/NightWaveLockerActivity.kt")
offline = read(base+"offline/OfflineAudioLibrary.kt")
lyrics = read(base+"v2/lyrics/PrivateLyricsStore.kt")
lockerAuth = read(base+"v2/locker/NightWaveLockerAuthorization.kt")
backupAuth = read(base+"v2/cloud/NightWaveCloudAuthorization.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/locker/LockerDownloadPolicyTest.kt")
old = read("tools/verify_v2_phase6r1.py")
workflow = read(".github/workflows/android.yml")
manifest = read("app/src/main/AndroidManifest.xml")
gradle = read("app/build.gradle.kts")

assert "sourceLocalId: String = \"\"" in policy
assert "fun validLocalId(value: String)" in policy
assert "fun chooseRestoredId(remote: LockerRemoteTrack, occupied: (String) -> Boolean)" in policy
assert "!occupied(preferred)" in policy
assert "UUID.randomUUID().toString()" in policy
assert "fun requireDownloadable(remote: LockerRemoteTrack)" in policy
assert "remote.sha256.isNotBlank()" in policy
assert 'remote.md5.matches(Regex("[a-fA-F0-9]{32}"))' in policy
assert "remote.sourceLocalId.isBlank() || validLocalId(remote.sourceLocalId)" in policy
assert 'suffixToMime[ext] == mime' in policy
assert "MAX_FILE_BYTES = 250L * 1024L * 1024L" in policy
assert "check(!Thread.currentThread().isInterrupted)" in policy

assert "fun downloadToStaging(" in client
assert "LockerAudioPolicy.requireDownloadable(selected)" in client
assert "folder(createIfMissing = false)" in client
assert "listInFolder(parent).firstOrNull { it.id == selected.id }" in client
assert "require(latest == selected)" in client
assert "capabilities(canDownload)" in client
assert 'optBoolean("canDownload") == true' in client
assert 'metadata.optJSONArray("parents")' in client
assert "current == selected" in client
assert "props?.optString(\"nightwave_local_id\").orEmpty()" in client
assert "properties?.optString(\"nightwave_local_id\").orEmpty()" in client
assert 'addQueryParameter("alt", "media")' in client
assert "require(response.code == 200)" in client
assert "announced == -1L || announced == selected.bytes" in client
assert "received <= selected.bytes" in client
assert "received == selected.bytes" in client
assert 'FileOutputStream(partial).use { output ->' in client
assert "output.fd.sync()" in client
assert "LockerAudioPolicy.hashes(partial)" in client
assert "sha256 == selected.sha256" in client
assert "md5.equals(selected.md5, ignoreCase = true)" in client
assert "partial.createNewFile()" in client
assert 'UUID.randomUUID().toString() + ".pending"' in client
assert "partial.delete()" in client
assert "Thread.currentThread().isInterrupted" in client
assert "progress(100)" in client
assert "fun upload(song: OfflineSong, file: File, progress: (Int) -> Unit)" in client
assert '"https://www.googleapis.com/auth/drive"' not in client
assert '.addQueryParameter("spaces", "appDataFolder")' not in client
assert '"PATCH"' not in client
assert '.method("DELETE"' not in client

assert "fun importVerifiedCloud(" in offline
assert 'File(context.applicationContext.filesDir, "locker_staging").canonicalFile' in offline
assert "file.parentFile == stagedRoot" in offline
assert "file.length() == remote.bytes" in offline
assert "LockerAudioPolicy.hashes(file)" in offline
assert "sha == remote.sha256 && md5.equals(remote.md5, true)" in offline
assert "if (song.bytes != remote.bytes || song.mimeType != remote.mimeType) continue" in offline
assert "if (LockerAudioPolicy.hashes(current).first == sha)" in offline
assert "CloudOfflineImportResult(song, created = false" in offline
assert "previous.size < MAX_SONGS" in offline
assert "previous.sumOf { it.bytes } + remote.bytes <= MAX_LIBRARY_BYTES" in offline
assert "LockerAudioPolicy.chooseRestoredId(remote)" in offline
assert "previous.any { song -> song.id == id } || File(root, id).exists()" in offline
assert "require(!finalFile.exists())" in offline
assert "Files.move(file.toPath(), finalFile.toPath())" in offline
assert "prefs.edit().putString(\"items\", encode(previous + saved)).commit()" in offline
assert "finalFile.delete()" in offline
assert "originalIdPreserved = saved.id == remote.sourceLocalId" in offline
assert "fun import(uri: Uri): OfflineSong" in offline
assert "fun delete(song: OfflineSong)" in offline

assert 'setTitle("Download to NightWave?")' in ui
assert 'setPositiveButton("Download to phone")' in ui
assert 'begin("download", song)' in ui
assert "LockerAudioPolicy.requireDownloadable(song)" in ui
assert "client.downloadWithRecovery(" in ui
assert 'File(filesDir, "locker_staging")' in ui
assert "offline.importVerifiedCloud(staging, track)" in ui
assert "checkpoints.discard()" in ui
assert "is CloudOfflineImportResult ->" in ui
assert "Original song ID retained for LRC recovery" in ui
assert "No local audio selected for upload." in ui
assert "client.upload(song, file)" in ui
assert "client.inventory()" in ui
assert "is LockerCloudInventory ->" in ui
assert "Cloud-to-phone downloads are not yet enabled." not in ui
assert "fun restoreCloudMissing(entries: List<CloudLyricEntry>)" in lyrics
assert '!existing.containsKey("lrc:" + it.localSongId)' in lyrics
assert 'SCOPE = "https://www.googleapis.com/auth/drive.file"' in lockerAuth
assert 'SCOPE = "https://www.googleapis.com/auth/drive.appdata"' in backupAuth

assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert '<activity android:name=".v2.locker.NightWaveLockerActivity" android:exported="false" />' in manifest
assert 'android:allowBackup="false"' in manifest
ET.parse(root/"app/src/main/AndroidManifest.xml")

assert 'begin(action: String, download: LockerRemoteTrack? = null)' in old
assert 'tools/verify_v2_phase6r2.py' in workflow
assert 'feature/nightwave-v2-phase6r1-drive-audio-upload' in workflow
assert ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' in workflow
assert tests.count("@Test") >= 17
for name,s in [("client",client),("policy",policy),("ui",ui),
               ("offline",offline),("tests",tests)]:
    assert s.count("{") == s.count("}"), name
    imports = [line for line in s.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name
print("PASS: R2 bounded verified Drive download, collision-safe offline import and optional lyric identity")
print("UNVERIFIED: Gradle/JVM execution, Drive OAuth and live transfer, local Android install and signer")
