#!/usr/bin/env python3
"""6V3 Kotlin project linkage and build triage source contract, not Android build."""
from pathlib import Path
import ast
import xml.etree.ElementTree as ET

r=Path(__file__).resolve().parents[1]
def src(p):return (r/p).read_text(encoding="utf-8")
linker=src("tools/check-kotlin-internal-imports.py")
workflow=src(".github/workflows/android.yml")
win=src("tools/build-windows.ps1")
local=src("tools/check-kotlin-compat.ps1")
ci=src("tools/ci_compile_summary.py")
lib=src("app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt")
lock=src("app/src/main/java/com/nightwave/player/offline/OfflineLibraryIndexLock.kt")
upload=src("app/src/main/java/com/nightwave/player/v2/locker/NightWaveMusicDriveClient.kt")
receipt=src("app/src/main/java/com/nightwave/player/v2/locker/LockerDriveReceiptPolicy.kt")
gradle=src("app/build.gradle.kts")
manifest=src("app/src/main/AndroidManifest.xml")
old=src("tools/verify_v2_phase6v2.py")

assert "def audit(root: Path)" in linker
assert "def self_test()" in linker
assert "def declarations(body: str, package: str)" in linker
assert "SOURCE_DIRS = (" in linker
assert '"app/src/main/java"' in linker
assert '"app/src/test/java"' in linker
assert '"app/src/androidTest/java"' in linker
assert '"app/src/debug/java"' in linker
assert '"com.nightwave.player.R"' in linker
assert '"com.nightwave.player.BuildConfig"' in linker
assert 'name.startswith("com.nightwave.player.")' in linker
assert 'name in all_symbols' in linker
assert 'name.startswith(x + ".") for x in all_symbols' in linker
assert '"UNRESOLVED: " + line' in linker
assert "if errors and args.strict:" in linker
assert "synthetic valid Kotlin project imports accepted" in linker
assert "MissingSong" in linker
assert "not issues" in linker
assert "len(errors) == 1" in linker
assert "Kotlin compilation unverified" in linker

assert "python3 tools/check-kotlin-internal-imports.py --self-test" in workflow
assert "python3 tools/check-kotlin-internal-imports.py" in workflow
assert "python3 tools/verify_v2_phase6v3.py" in workflow
assert "feature/nightwave-v2-phase6v2-offline-index-serialization" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
assert "gradle --no-daemon --stacktrace" in workflow
assert "setup-java@v4" in workflow
assert "gradle/actions/setup-gradle@v4" in workflow
assert "android-actions/setup-android@v3" in workflow
assert "python3 tools/ci_compile_summary.py --self-test" in workflow
assert "python3 tools/ci_compile_summary.py --log" in workflow

assert "check-kotlin-compat.ps1" in win
assert "check-phase6v-integration.ps1" in win
assert "verify-apk-signature.ps1" in win
assert "BUILD SUCCESSFUL" in win
assert "'versionCode = 35'" in local
assert "def redact(line: str)" in ci
assert "MAX_FINDINGS = 65" in ci
assert "No terminal Gradle result" in ci

assert "fun import(uri: Uri): OfflineSong = synchronized(OfflineLibraryIndexLock.monitor)" in lib
assert "CloudOfflineImportResult = synchronized(OfflineLibraryIndexLock.monitor)" in lib
assert "fun delete(song: OfflineSong): Boolean = synchronized(OfflineLibraryIndexLock.monitor)" in lib
assert "val monitor = Any()" in lock
assert "LockerDriveReceiptPolicy.requireCompletedUpload(" in upload
assert "fun requireCompletedUpload(" in receipt
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert 'android:allowBackup="false"' in manifest
assert "android:process=" not in manifest
ET.parse(r/"app/src/main/AndroidManifest.xml")
assert "assert library.count" in old
ast.parse(linker)
ast.parse(ci)
ast.parse(src("tools/verify_v2_phase6v3.py"))
print("PASS: 6V3 Kotlin internal linkage scanner registered alongside actual Gradle build")
print("NOT VERIFIED: full Kotlin/Android compile, JUnit, Google Drive, signer/device tests")
