#!/usr/bin/env python3
"""Phase 6Q3 opt-in Drive v3 extension preservation. Source-only, not an APK build."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def src(p):return (root/p).read_text(encoding="utf-8")
p="app/src/main/java/com/nightwave/player/"
codec=src(p+"v2/cloud/CloudBackupCodec.kt")
options=src(p+"v2/cloud/CloudOptionalData.kt")
lyrics=src(p+"v2/lyrics/PrivateLyricsStore.kt")
state=src(p+"v2/cloud/CloudBackupState.kt")
worker=src(p+"v2/cloud/NightWaveCloudWorker.kt")
consent_policy=src(p+"v2/cloud/CloudBackupConsentPolicy.kt")
activity=src(p+"v2/ui/NightWaveStudioActivity.kt")
auth=src(p+"v2/cloud/NightWaveCloudAuthorization.kt")
drive=src(p+"v2/cloud/NightWaveDriveClient.kt")
tests=src("app/src/test/java/com/nightwave/player/v2/cloud/CloudOptionalDataTest.kt")
workflow=src(".github/workflows/android.yml")
gradle=src("app/build.gradle.kts")
assert "val preferences: CloudPlaybackPreferences? = null" in codec
assert "val lyrics: List<CloudLyricEntry> = emptyList()" in codec
assert "val includesLyrics: Boolean = false" in codec
assert 'val optional = includeLyrics || preferences != null' in codec
assert '.put("format", if (optional) 3 else 2)' in codec
assert 'version == 2 && keys == oldKeys' in codec
assert 'version == 3 && keys == oldKeys +' in codec
assert 'CloudPlaybackPreferences.parse' in codec
assert 'CloudLyricsPolicy.validated(entries)' in codec
assert 'require(includeLyrics || lyrics.isEmpty())' in codec
assert 'digest(payloadBytes)' in codec
assert 'digest(payload) == wrapper.getString("sha256")' in codec
assert 'MAX_BYTES = 3_000_000' in codec
assert "object CloudLyricsPolicy" in options
assert "MAX_ENTRIES = 20" in options
assert "MAX_TOTAL_BYTES = 512_000" in options
assert "TimedLyricsCodec.parse(bytes)" in options
assert "Duplicated lyrics attachment" in options
assert "fun applyTo(local: ProfileSettings)" in options
assert "showArtworkOnLockscreen = lockscreenArtwork" in options
assert "keepScreenAwakeWhileBrowsing = keepScreenAwake" in options
assert "require(crossfadeSeconds in 0..12)" in options
for forbidden in ("accessToken", "localId =", "historyPrivacy =", "password", "client_secret"):
    assert forbidden not in options,forbidden
assert "fun exportCloud(): List<CloudLyricEntry>" in lyrics
assert "CloudLyricsPolicy.validated(records)" in lyrics
assert "fun restoreCloudMissing(entries: List<CloudLyricEntry>)" in lyrics
assert "it.localSongId in validIds" in lyrics
assert '!existing.containsKey("lrc:" + it.localSongId)' in lyrics
assert "fun save(id: String, bytes: ByteArray)" in lyrics
assert 'nightwave_private_lrc_v1' in lyrics
assert 'includeLyrics: Boolean = false' in state
assert 'includePreferences: Boolean = false' in state
assert 'fun setLyrics(value: Boolean)' in state
assert 'fun setPreferences(value: Boolean)' in state
assert 'putBoolean("lyrics", value)' in state
assert 'putBoolean("preferences", value)' in state
assert "PrivateLyricsStore(applicationContext).exportCloud()" in worker
assert "CloudPlaybackPreferences.from(GuestProfileStore(applicationContext).load())" in worker
assert "CloudBackupConsentPolicy.same(consent," in worker
assert "beforeUpload.consentSnapshot()" in worker
assert "store.successIfCurrent(consent," in worker
assert "expected.lyrics == current.lyrics" in consent_policy
assert "expected.preferences == current.preferences" in consent_policy
assert "expected.revision == current.revision" in consent_policy
assert 'CloudBackupCodec.encode(backup, history, settings.includeHistory,' in worker
assert "val extraLyrics = if (settings.includeLyrics)" in activity
assert "val extraPreferences = if (settings.includePreferences)" in activity
assert "latestSettings.includeLyrics == settings.includeLyrics" in activity
assert "latestSettings.includePreferences == settings.includePreferences" in activity
assert "showCloudOptionalRestorePrompt(preview, account)" in activity
assert 'setTitle("Restore optional backup data?")' in activity
assert 'setNegativeButton("Keep local options", null)' in activity
assert 'setPositiveButton("Apply optional data")' in activity
assert "lyricsStore.restoreCloudMissing(backup.lyrics)" in activity
assert "profiles.save(restored.applyTo(current))" in activity
assert "cloudStore.setLyrics(enabled)" in activity
assert "cloudStore.setPreferences(enabled)" in activity
assert "onCloudLyrics = onCloudLyrics" in activity
assert "onCloudPreferences = onCloudPreferences" in activity
assert 'StudioProfileRow("Back up imported LRC lyrics"' in activity
assert 'StudioProfileRow("Back up playback preferences"' in activity
assert 'showCloudRestorePreview(remote, comparison, expectedAccount)' in activity
assert "archiveManager.restoreCloudIfUnchanged(" in activity
assert 'https://www.googleapis.com/auth/drive.appdata' in auth
assert 'addQueryParameter("spaces", "appDataFolder")' in drive
assert "delete(" not in drive and "PATCH" not in drive
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert "tools/verify_v2_phase6q3.py" in workflow
assert "feature/nightwave-v2-phase6q2-recovery-history" in workflow
assert tests.count("@Test") >= 16
for name,s in [("codec",codec),("options",options),("lyrics",lyrics),
               ("state",state),("worker",worker),("activity",activity),("tests",tests)]:
    assert s.count("{")==s.count("}"),name
    imports=[line for line in s.splitlines() if line.startswith("import ")]
    assert len(imports)==len(set(imports)),name
print("PASS: 6Q3 optional v3 Drive lyrics/preferences backup, v2 compatibility, confirmed restore contracts")
print("NOT VERIFIED: Kotlin/JUnit, Google Drive E2E, Android device and existing installed-app signing")
