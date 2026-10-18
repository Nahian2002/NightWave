#!/usr/bin/env python3
"""6R3B upload session status and retry source checks; not live Drive tests."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
def src(p): return (r/p).read_text(encoding="utf-8")
b="app/src/main/java/com/nightwave/player/"
c=src(b+"v2/locker/NightWaveMusicDriveClient.kt")
p=src(b+"v2/locker/LockerUploadResumePolicy.kt")
ui=src(b+"v2/locker/NightWaveLockerActivity.kt")
old=src("tools/verify_v2_phase6r1.py")
dl=src(b+"v2/locker/LockerDownloadCheckpointStore.kt")
t=src("app/src/test/java/com/nightwave/player/v2/locker/LockerUploadResumePolicyTest.kt")
ci=src(".github/workflows/android.yml")
gradle=src("app/build.gradle.kts")
manifest=src("app/src/main/AndroidManifest.xml")
assert "object LockerUploadResumePolicy" in p
assert "MAX_CONSECUTIVE_FAILURES = 3" in p
assert "CHUNK_ALIGNMENT = 256 * 1024" in p
assert "fun acknowledged(range: String?, total: Long, previouslyConfirmed: Long)" in p
assert "require(previouslyConfirmed in 0..total)" in p
assert "previouslyConfirmed == 0L" in p
assert "last >= 0 && last < total" in p
assert "received >= previouslyConfirmed" in p
assert "offset % CHUNK_ALIGNMENT == 0L" in p
assert "total - offset <= LockerAudioPolicy.CHUNK_BYTES.toLong()" in p
assert "code == 408 || code == 429 || code in 500..599" in p
assert "failures in 1 until MAX_CONSECUTIVE_FAILURES" in p
assert "private fun uploadCompletion(" in c
assert "response.code == 200 || response.code == 201" in c
assert "LockerAudioPolicy.requireDownloadable(LockerRemoteTrack(" in c
assert "track.name == LockerAudioPolicy.fileName(song)" in c
assert "track.sourceLocalId == song.id" in c
assert "track.md5.equals(md5, ignoreCase = true)" in c
assert "track.sha256 == sha256" in c
assert "return LockerUploadOutcome(track, created = true)" in c
assert "private sealed class UploadProbe" in c
assert "data class Resume(val offset: Long)" in c
assert "data class Complete(val outcome: LockerUploadOutcome)" in c
assert "private fun queryUploadStatus(" in c
assert 'header("Content-Range", "bytes */' in c
assert "ByteArray(0).toRequestBody()" in c
assert "200, 201 -> UploadProbe.Complete(" in c
assert "308 -> UploadProbe.Resume(" in c
assert "LockerUploadResumePolicy.acknowledged(" in c
assert "LockerUploadResumePolicy.transientStatus(response.code)" in c
assert "Drive upload session expired or refused status check" in c
assert "fun upload(song: OfflineSong, file: File, progress: (Int) -> Unit)" in c
assert "LockerAudioPolicy.validateLocal(song, file)" in c
assert "LockerAudioPolicy.hashes(file)" in c
assert "LockerUploadOutcome(existing, created = false)" in c
assert 'addQueryParameter("uploadType", "resumable")' in c
assert '"X-Upload-Content-Type"' in c
assert '"X-Upload-Content-Length"' in c
assert "uri.isHttps" in c
assert 'uri.host == "www.googleapis.com"' in c
assert "val session = uri.toString()" in c
assert "LockerUploadResumePolicy.canSendNext(offset, song.bytes)" in c
assert 'header("Content-Range", "bytes $offset-$end/' in c
assert "reader.seek(offset)" in c
assert "reader.readFully(bytes)" in c
assert "require(next <= end + 1)" in c
assert "if (next == offset)" in c
assert "No accepted upload progress" in c
assert "LockerUploadResumePolicy.retryAllowed(failures)" in c
assert "Thread.sleep(300L * failures)" in c
assert "queryUploadStatus(session, song, parent, sha256, md5, offset)" in c
assert "is UploadProbe.Complete ->" in c
assert "is UploadProbe.Resume ->" in c
assert "if (status.offset > offset)" in c
assert 'check(!Thread.currentThread().isInterrupted)' in c
assert "coerceAtMost(99)" in c
assert "progress(100)" in c
assert "Drive has not finalized the upload" in c
assert '.addQueryParameter("spaces", "appDataFolder")' not in c
assert '.method("DELETE"' not in c
assert '.method("PATCH"' not in c
assert "client.upload(song, file)" in ui
assert 'begin("upload")' in ui
assert "Upload could not be verified." in ui
assert "Retry explicitly:" in ui
assert "client.downloadWithRecovery(" in ui
assert "nightwave_locker_download_v1.json" in dl
assert "sessionUrl" not in dl
assert "assert '308 -> {' in client" in old
assert "assert 'track.md5.equals(md5, ignoreCase = true)' in client" in old
assert "tools/verify_v2_phase6r3b.py" in ci
assert "feature/nightwave-v2-phase6r3-download-checkpoints" in ci
assert ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' in ci
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert 'android:allowBackup="false"' in manifest
assert t.count("@Test") >= 20
for name,data in [("client",c),("policy",p),("ui",ui),("tests",t)]:
    assert data.count("{")==data.count("}"),name
    imports=[s for s in data.splitlines() if s.startswith("import ")]
    assert len(imports)==len(set(imports)),name
print("PASS: R3B Drive upload status probe/reconciliation and checksum completion safety")
print("PENDING: real Gradle/JUnit, OAuth, Drive E2E and signed Android device test")
