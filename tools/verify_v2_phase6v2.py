#!/usr/bin/env python3
"""6V2: protect offline music index across Activity/library objects.

Static contract only. Real Android/JVM tests require successful Gradle execution.
"""
from pathlib import Path
import ast
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
def source(path): return (root/path).read_text(encoding="utf-8")
base="app/src/main/java/com/nightwave/player/"
library=source(base+"offline/OfflineAudioLibrary.kt")
lock=source(base+"offline/OfflineLibraryIndexLock.kt")
tests=source("app/src/test/java/com/nightwave/player/offline/OfflineLibraryIndexLockTest.kt")
client=source(base+"v2/locker/NightWaveMusicDriveClient.kt")
locker=source(base+"v2/locker/NightWaveLockerActivity.kt")
manifest=source("app/src/main/AndroidManifest.xml")
gradle=source("app/build.gradle.kts")
workflow=source(".github/workflows/android.yml")
old=source("tools/verify_v2_phase6v1.py")
assert 'internal object OfflineLibraryIndexLock' in lock
assert 'val monitor = Any()' in lock
assert 'package com.nightwave.player.offline' in lock
assert 'private val indexLock = Any()' not in library
assert '@Synchronized' not in library
assert 'fun import(uri: Uri): OfflineSong = synchronized(OfflineLibraryIndexLock.monitor)' in library
assert '''fun importVerifiedCloud(
        staged: File, remote: LockerRemoteTrack
    ): CloudOfflineImportResult = synchronized(OfflineLibraryIndexLock.monitor)''' in library
assert 'fun delete(song: OfflineSong): Boolean = synchronized(OfflineLibraryIndexLock.monitor)' in library
assert library.count('synchronized(OfflineLibraryIndexLock.monitor)') == 3
assert 'val previous = songs()' in library
assert 'prefs.edit().putString("items", encode(previous + song)).commit()' in library
assert 'prefs.edit().putString("items", encode(previous + saved)).commit()' in library
assert 'prefs.edit().putString("items", encode(updated)).commit()' in library
assert 'LockerAudioPolicy.requireDownloadable(remote)' in library
assert 'sha == remote.sha256 && md5.equals(remote.md5, true)' in library
assert 'Files.move(file.toPath(), finalFile.toPath())' in library
assert 'finalFile.delete()' in library
assert 'originalIdPreserved = saved.id == remote.sourceLocalId' in library
assert 'CloudOfflineImportResult(song, created = false' in library
assert 'OfflineAudioLibrary(this)' in locker
assert 'offline.importVerifiedCloud(staging, track)' in locker
assert 'fun upload(song: OfflineSong, file: File, progress: (Int) -> Unit)' in client
assert 'fun downloadWithRecovery(' in client
assert 'LockerDriveReceiptPolicy.requireCompletedUpload(' in client
assert tests.count('@Test') >= 3
assert 'CountDownLatch' in tests
assert 'TimeUnit' in tests
assert 'synchronized(OfflineLibraryIndexLock.monitor)' in tests
assert 'highest.get()' in tests
assert 'AtomicInteger' in tests
assert 'assertFalse("Index lock allowed overlapping mutations"' in tests
assert 'assertEquals(1, highest.get())' in tests
assert 'android:process=' not in manifest
assert 'android:allowBackup="false"' in manifest
ET.parse(root/"app/src/main/AndroidManifest.xml")
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'testImplementation("junit:junit:4.13.2")' in gradle
assert 'tools/verify_v2_phase6v2.py' in workflow
assert 'feature/nightwave-v2-phase6v1-receipt-contract-build-hardening' in workflow
assert 'python3 tools/verify_v2_phase6v1.py' in workflow
assert 'python3 tools/verify_v2_phase6v2.py' in workflow
assert ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' in workflow
assert 'assert tests.count("@Test") >= 18' in old
for label,data in [('library',library),('lock',lock),('tests',tests)]:
    assert data.count('{') == data.count('}'),label
    imports=[line for line in data.splitlines() if line.startswith('import ')]
    assert len(imports) == len(set(imports)),label
ast.parse(source("tools/verify_v2_phase6v2.py"))
print("PASS: 6V2 all three offline index writers share one process-wide JVM monitor")
print("PENDING: Android SharedPreferences race/Gradle/JUnit test execution")
