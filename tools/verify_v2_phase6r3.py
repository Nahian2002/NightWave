#!/usr/bin/env python3
"""6R3 bounded, explicit restart-safe cloud download recovery source gate."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def read(path): return (root/path).read_text(encoding="utf-8")
b="app/src/main/java/com/nightwave/player/"
client=read(b+"v2/locker/NightWaveMusicDriveClient.kt")
store=read(b+"v2/locker/LockerDownloadCheckpointStore.kt")
policy=read(b+"v2/locker/LockerResumePolicy.kt")
ui=read(b+"v2/locker/NightWaveLockerActivity.kt")
offline=read(b+"offline/OfflineAudioLibrary.kt")
auth=read(b+"v2/locker/NightWaveLockerAuthorization.kt")
old_auth=read(b+"v2/cloud/NightWaveCloudAuthorization.kt")
tests=read("app/src/test/java/com/nightwave/player/v2/locker/LockerResumePolicyTest.kt")
old=read("tools/verify_v2_phase6r2.py")
workflow=read(".github/workflows/android.yml")
gradle=read("app/build.gradle.kts")
manifest=read("app/src/main/AndroidManifest.xml")

assert 'object LockerResumePolicy' in policy
assert 'MAX_ATTEMPTS = 3' in policy
assert 'CHECKPOINT_STEP = 1024L * 1024L' in policy
assert 'fun verifiedSpan(' in policy
assert 'require(code == 206)' in policy
assert 'start == offset' in policy
assert 'end >= start && end < total' in policy
assert 'length.toLong() == total' in policy
assert 'declaredBytes == -1L || declaredBytes == count' in policy
assert 'declaredBytes == -1L || declaredBytes == total' in policy
assert 'offset == 0L && code == 200' in policy
assert 'coerceAtMost(99)' in policy
assert 'exception is java.io.IOException' in policy

assert 'class LockerDownloadCheckpointStore(context: Context)' in store
assert 'AtomicFile(File(root, "nightwave_locker_download_v1.json"))' in store
assert 'LockerPendingDownload(' in store
assert 'val committedBytes: Long' in store
assert 'fun read(): LockerPendingDownload?' in store
assert 'fun begin(account: String, track: LockerRemoteTrack)' in store
assert 'fun commit(previous: LockerPendingDownload, committed: Long)' in store
assert 'fun stagedFile(item: LockerPendingDownload)' in store
assert 'fun needsRepair(): Boolean' in store
assert 'fun discard()' in store
assert '"file", "committed"' in store
assert '"id", "name", "mime", "bytes", "md5", "sha256", "source"' in store
assert 'LockerAudioPolicy.requireDownloadable' in store
assert 'old.account.equals(account, true) && old.remote == track' in store
assert 'old.committedBytes <= committed && committed <= old.remote.bytes' in store
assert 'pendingPattern.matches(name)' in store
assert 'file.parentFile == dir' in store
assert 'atomic.startWrite()' in store
assert 'atomic.finishWrite(stream)' in store
assert 'atomic.failWrite(stream)' in store
assert 'atomic.delete()' in store
assert '"https://www.googleapis.com/auth/drive.file"' not in store
assert not any(s in store for s in ('access_token', 'refresh_token', 'Bearer ', 'upload_id=', 'sessionUrl', 'client_secret'))

assert 'fun downloadWithRecovery(' in client
assert 'LockerAudioPolicy.requireDownloadable(selected)' in client
assert 'listInFolder(parent).firstOrNull { it.id == selected.id }' in client
assert 'require(latest == selected)' in client
assert 'capabilities(canDownload)' in client
assert 'metadata.optJSONArray("parents")' in client
assert 'require(fresh == selected' in client
assert 'checkpoints.begin(account, selected)' in client
assert 'checkpoints.stagedFile(checkpoint)' in client
assert 'checkpoint.committedBytes == 0L && partial.createNewFile()' in client
assert 'partial.length() >= checkpoint.committedBytes' in client
assert 'file.setLength(durable.committedBytes)' in client
assert 'val offset = durable.committedBytes' in client
assert 'builder.header("Range", "bytes=$offset-")' in client
assert 'LockerResumePolicy.verifiedSpan(' in client
assert 'if (response.code in listOf(408, 429, 500, 502, 503, 504))' in client
assert 'throw java.io.IOException("Drive stalled during resume")' in client
assert 'output.fd.sync()' in client
assert 'checkpoints.commit(durable, offset + received)' in client
assert 'LockerResumePolicy.percent(offset + received, selected.bytes)' in client
assert 'attempts < LockerResumePolicy.MAX_ATTEMPTS' in client
assert 'LockerResumePolicy.shouldRetry(attempts, ex)' in client
assert 'Thread.sleep(250L * attempts)' in client
assert 'check(!Thread.currentThread().isInterrupted)' in client
assert 'completed.committedBytes == selected.bytes' in client
assert 'partial.length() == selected.bytes' in client
assert 'LockerAudioPolicy.hashes(partial)' in client
assert 'sha256 != selected.sha256' in client
assert 'md5.equals(selected.md5, true)' in client
assert 'checkpoints.discard()' in client
assert 'fun upload(song: OfflineSong, file: File, progress: (Int) -> Unit)' in client
assert '.addQueryParameter("spaces", "appDataFolder")' not in client
assert '.method("DELETE"' not in client
assert '"PATCH"' not in client

assert 'checkpoints = LockerDownloadCheckpointStore(this)' in ui
assert '"Resume interrupted download"' in ui
assert '"Discard incomplete download"' in ui
assert 'setTitle("Resume verified download?")' in ui
assert 'saved.account.equals(state.account(), true)' in ui
assert 'setPositiveButton("Resume")' in ui
assert 'setPositiveButton("Discard")' in ui
assert 'begin("download", saved.remote)' in ui
assert 'client.downloadWithRecovery(' in ui
assert 'offline.importVerifiedCloud(staging, track)' in ui
assert 'checkpoints.discard()' in ui
assert 'updatePending()' in ui
assert 'checkpoints.needsRepair()' in ui
assert '"Download interrupted. Resume after checking your connection, "' in ui
assert '"Cloud audio and saved songs are untouched."' not in ui or 'Your Drive upload' in ui
assert 'worker.shutdownNow()' in ui
assert 'CloudOfflineImportResult' in ui
assert 'fun importVerifiedCloud(' in offline
assert 'sha == remote.sha256 && md5.equals(remote.md5, true)' in offline
assert 'Files.move(file.toPath(), finalFile.toPath())' in offline
assert 'SCOPE = "https://www.googleapis.com/auth/drive.file"' in auth
assert 'SCOPE = "https://www.googleapis.com/auth/drive.appdata"' in old_auth

assert 'assert "client.downloadWithRecovery(" in ui' in old
assert 'assert "checkpoints.discard()" in ui' in old
assert 'tools/verify_v2_phase6r3.py' in workflow
assert 'feature/nightwave-v2-phase6r2-verified-cloud-downloads' in workflow
assert ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' in workflow
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'android:allowBackup="false"' in manifest
assert tests.count('@Test') >= 20
for label,source in [("policy",policy),("store",store),("client",client),("ui",ui),("tests",tests)]:
    assert source.count("{")==source.count("}"),label
    imports=[line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports)==len(set(imports)),label
print("PASS: 6R3 restart-safe checkpoint, exact Drive byte ranges and bounded retries")
print("NOT EXECUTED: Kotlin/JVM tests, Android/Google Drive E2E, signer continuity")
