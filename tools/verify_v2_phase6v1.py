#!/usr/bin/env python3
"""NightWave 6V1: strict, data-testable Drive upload receipt integration preflight.

Offline source verification, NOT a substitute for Gradle or Google OAuth E2E.
"""
from pathlib import Path
import ast
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
def src(path):
    return (root / path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
receipt = src(base+"v2/locker/LockerDriveReceiptPolicy.kt")
client = src(base+"v2/locker/NightWaveMusicDriveClient.kt")
policy = src(base+"v2/locker/LockerAudioPolicy.kt")
offline = src(base+"offline/OfflineAudioLibrary.kt")
tests = src("app/src/test/java/com/nightwave/player/v2/locker/LockerDriveReceiptPolicyTest.kt")
auth = src(base+"v2/locker/NightWaveLockerAuthorization.kt")
cloud = src(base+"v2/cloud/NightWaveCloudAuthorization.kt")
old = src("tools/verify_v2_phase6r1.py")
old_r3b = src("tools/verify_v2_phase6r3b.py")
phase6v = src("tools/verify_v2_phase6v.py")
local = src("tools/check-phase6v-integration.ps1")
ci = src(".github/workflows/android.yml")
gradle = src("app/build.gradle.kts")
manifest = src("app/src/main/AndroidManifest.xml")

assert receipt.startswith("package com.nightwave.player.v2.locker")
assert "object LockerDriveReceiptPolicy" in receipt
assert "fun requireCompletedUpload(" in receipt
assert "response: JSONObject" in receipt
assert "source: OfflineSong" in receipt
assert "expectedFolder: String" in receipt
assert "expectedSha256: String" in receipt
assert "expectedMd5: String" in receipt
assert "LockerAudioPolicy.validDriveId(expectedFolder)" in receipt
assert "LockerAudioPolicy.validSha256(expectedSha256)" in receipt
assert 'Regex("[a-fA-F0-9]{32}")' in receipt
assert 'response.optJSONObject("appProperties")' in receipt
assert 'response.optString("size").toLongOrNull()' in receipt
assert "LockerAudioPolicy.requireDownloadable(LockerRemoteTrack(" in receipt
assert 'fields.optString("nightwave_sha256")' in receipt
assert 'fields.optString("nightwave_local_id")' in receipt
assert 'response.optJSONArray("parents")' in receipt
assert 'parents.optString(it) == expectedFolder' in receipt
assert "track.bytes == source.bytes" in receipt
assert "track.mimeType == source.mimeType" in receipt
assert "track.name == LockerAudioPolicy.fileName(source)" in receipt
assert "track.sourceLocalId == source.id" in receipt
assert "track.md5.equals(expectedMd5, ignoreCase = true)" in receipt
assert "track.sha256 == expectedSha256" in receipt
assert "return LockerUploadOutcome(track, created = true)" in receipt
assert not any(s in receipt for s in (
    "access_token", "refresh_token", "Bearer ", "Request.Builder",
    "HttpURLConnection", "SharedPreferences", ".delete(",
))

assert "private fun uploadCompletion(" in client
assert "response.code == 200 || response.code == 201" in client
assert "LockerDriveReceiptPolicy.requireCompletedUpload(" in client
assert "data, song, expectedParent, sha256, md5" in client
assert "private fun queryUploadStatus(" in client
assert "200, 201 -> UploadProbe.Complete(" in client
assert "308 -> UploadProbe.Resume(" in client
assert "LockerUploadResumePolicy.acknowledged(" in client
assert "queryUploadStatus(session, song, parent, sha256, md5, offset)" in client
assert "LockerUploadOutcome(existing, created = false)" in client
assert "progress(100)" in client
assert "coerceAtMost(99)" in client
assert "fun downloadWithRecovery(" in client
assert "LockerAudioPolicy.hashes(partial)" in client
assert "fun importVerifiedCloud(" in offline
assert "LockerAudioPolicy.requireDownloadable(remote)" in offline
assert "fun requireDownloadable(remote: LockerRemoteTrack)" in policy
assert ".method(\"DELETE\"" not in client
assert ".method(\"PATCH\"" not in client
assert '.addQueryParameter("spaces", "appDataFolder")' not in client

assert "SCOPE = \"https://www.googleapis.com/auth/drive.file\"" in auth
assert "SCOPE = \"https://www.googleapis.com/auth/drive.appdata\"" in cloud
assert "assert 'LockerDriveReceiptPolicy.requireCompletedUpload(' in client" in old
assert 'assert "LockerDriveReceiptPolicy.requireCompletedUpload(" in c' in old_r3b
assert 'has(music, "LockerDriveReceiptPolicy.requireCompletedUpload("' in phase6v
assert "Require $lockerClient 'LockerDriveReceiptPolicy.requireCompletedUpload('" in local
assert "tools/verify_v2_phase6v1.py" in ci
assert "feature/nightwave-v2-phase6v-integrated-build-audit" in ci
assert ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' in ci
assert 'testImplementation("org.json:json:20240303")' in gradle
assert 'testImplementation("junit:junit:4.13.2")' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'android:allowBackup="false"' in manifest
ET.parse(root/"app/src/main/AndroidManifest.xml")
assert tests.count("@Test") >= 18
for name, content in [("receipt",receipt),("client",client),("tests",tests)]:
    assert content.count("{") == content.count("}"), name
    imports=[line for line in content.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name
ast.parse(src("tools/verify_v2_phase6v1.py"))
print("PASS: 6V1 actual upload completion delegates to strict receipt policy, 18 authored fixtures")
print("NOT VERIFIED: Android Gradle/CI execution, Google OAuth, live upload or device signing")
