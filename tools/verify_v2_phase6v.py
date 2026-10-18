#!/usr/bin/env python3
"""6V cross-phase source/integration audit; never claim it compiled Android.

This is a zero-account, zero-network safety gate for the stacked 6Q-6R code.
It validates build inputs and compatibility invariants before Gradle begins.
"""
from __future__ import annotations

import ast
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = Path("app/src/main/java/com/nightwave/player")
FAILED: list[str] = []

def need(condition: bool, message: str) -> None:
    if not condition:
        FAILED.append(message)

def source(relative: str | Path) -> str:
    path = ROOT / relative
    if not path.is_file():
        need(False, "Missing build input: " + str(relative))
        return ""
    return path.read_text(encoding="utf-8")

def has(body: str, token: str, purpose: str) -> None:
    need(token in body, "Missing " + purpose)

def main() -> int:
    gate_files = sorted((ROOT / "tools").glob("verify_v2_phase*.py"))
    need(len(gate_files) >= 25, "Historical Python source gates missing")
    for file in gate_files:
        try:
            ast.parse(file.read_text(encoding="utf-8"), filename=file.name)
        except SyntaxError as exc:
            need(False, f"Invalid Python source checker {file.name}:{exc.lineno}")

    m = lambda name: source(MAIN / name)
    cloud = m("v2/cloud/CloudBackupCodec.kt")
    cloud_client = m("v2/cloud/NightWaveDriveClient.kt")
    cloud_auth = m("v2/cloud/NightWaveCloudAuthorization.kt")
    state = m("v2/cloud/CloudBackupState.kt")
    worker = m("v2/cloud/NightWaveCloudWorker.kt")
    recovery = m("v2/data/LibraryArchiveManager.kt")
    cloud_preview = m("v2/cloud/CloudRecoveryPolicy.kt")
    lyrics = m("v2/lyrics/PrivateLyricsStore.kt")
    music = m("v2/locker/NightWaveMusicDriveClient.kt")
    music_auth = m("v2/locker/NightWaveLockerAuthorization.kt")
    music_ui = m("v2/locker/NightWaveLockerActivity.kt")
    audio = m("offline/OfflineAudioLibrary.kt")
    progress = m("v2/locker/LockerDownloadCheckpointStore.kt")
    upload_policy = m("v2/locker/LockerUploadResumePolicy.kt")
    download_policy = m("v2/locker/LockerResumePolicy.kt")
    gradle = source("app/build.gradle.kts")
    root_gradle = source("build.gradle.kts")
    workflow = source(".github/workflows/android.yml")
    win = source("tools/build-windows.ps1")
    win_syntax = source("tools/check-builder-syntax.ps1")
    win_cloud = source("tools/check-phase6v-integration.ps1")

    need('SCOPE = "https://www.googleapis.com/auth/drive.appdata"' in cloud_auth,
         "Historical metadata backup must request only drive.appdata")
    need('SCOPE = "https://www.googleapis.com/auth/drive.file"' in music_auth,
         "Music locker must request separate drive.file")
    for label, content in (
        ("metadata cloud", cloud_client),
        ("personal audio", music),
    ):
        need("Bearer $accessToken" in content if label == "metadata cloud"
             else 'Bearer $token' in content,
             label + " requests must retain scoped short-lived authorization")
    has(cloud, "version == 2 && keys == oldKeys", "format-2 backup compatibility")
    has(cloud, "version == 3 && keys == oldKeys +", "format-3 optional backups")
    has(cloud, "digest(payload)", "checksummed cloud payload")
    has(state, "includeLyrics: Boolean = false", "off-by-default LRC backup")
    has(state, "includePreferences: Boolean = false", "off-by-default preferences backup")
    has(worker, "beforeUpload.includeLyrics != settings.includeLyrics", "worker opt-in recheck")
    has(worker, "beforeUpload.includePreferences != settings.includePreferences",
        "worker preferences recheck")
    has(cloud_client, "backUpIfChangedVerified(bytes: ByteArray)",
        "verified cloud snapshot dedupe")
    has(cloud_client, "appDataFolder", "metadata backup private Drive folder")
    has(recovery, "restoreCloudIfUnchanged(", "stale-preview fenced restore")
    has(recovery, "CloudRestoreApplyResult.STALE_PREVIEW", "explicit stale-restore result")
    has(cloud_preview, "localLikedNotInCloud", "local-vs-cloud loss preview")
    has(lyrics, '!existing.containsKey("lrc:" + it.localSongId)', "non-overwriting LRC recovery")
    has(music, "private fun queryUploadStatus(", "upload status probe")
    has(music, "fun downloadWithRecovery(", "resumable verified Drive downloads")
    has(music, "LockerAudioPolicy.hashes(partial)", "download SHA/MD5")
    has(music, "LockerDriveReceiptPolicy.requireCompletedUpload(", "verified Drive upload receipt")
    has(music, "sha256, md5", "upload hashes passed to receipt")
    has(music, "coerceAtMost(99)", "prevent false upload completion")
    has(music, "progress(100)", "confirmed transfer completion feedback")
    has(upload_policy, "MAX_CONSECUTIVE_FAILURES = 3", "bounded upload retries")
    has(download_policy, "MAX_ATTEMPTS = 3", "bounded download retries")
    has(progress, "AtomicFile(File(root,", "private atomic download checkpoint")
    has(audio, "fun importVerifiedCloud(", "verified offline cloud re-import")
    has(music_ui, 'setTitle("Resume verified download?")',
        "user-confirmed download recovery")
    has(music_ui, "Upload could not be verified.", "upload failure notice")

    for suspicious in (
        '.addQueryParameter("spaces", "appDataFolder")',
        '.method("DELETE"',
        '.method("PATCH"',
    ):
        need(suspicious not in music, "Personal music locker contains an unsafe Drive operation")
    for secret in ("access_token", "refresh_token", "sessionUrl", "Authorization: Bearer"):
        need(secret not in progress, "Sensitive authorization data in durable checkpoint")

    for app_marker in (
        'applicationId = "com.nightwave.player"',
        "versionCode = 35",
        'versionName = "2.0.0-alpha5s"',
        'testImplementation("junit:junit:4.13.2")',
        'testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")',
    ):
        has(gradle, app_marker, "Android identity/test requirement " + app_marker)
    for marker in (
        'id("com.android.application") version "8.9.2"',
        'id("org.jetbrains.kotlin.android") version "2.0.21"',
    ):
        has(root_gradle, marker, "pinned Android/Kotlin toolchain")
    for marker in (
        'python3 tools/verify_v2_phase6r3b.py',
        'python3 tools/verify_v2_phase6v.py',
        ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin',
        'app/build/outputs/apk/debug/app-debug.apk',
        "runs-on: ubuntu-latest",
    ):
        has(workflow, marker, "CI task/input " + marker)
    has(win, "check-phase6v-integration.ps1", "local Windows 6V gate")
    has(win, "verify-apk-signature.ps1", "existing Windows signer verification")
    has(win_syntax, "check-phase6v-integration.ps1",
        "local syntax check registration")
    has(win_cloud, "PASS: NightWave 6V cross-phase", "local verifier success marker")

    manifest_path = ROOT / "app/src/main/AndroidManifest.xml"
    try:
        root = ET.parse(manifest_path).getroot()
        a = "{http://schemas.android.com/apk/res/android}"
        app = root.find("application")
        need(app is not None, "Android application declaration missing")
        if app is not None:
            need(app.attrib.get(a + "allowBackup") == "false",
                 "Android broad backup unexpectedly enabled")
            need(app.attrib.get(a + "usesCleartextTraffic") == "false",
                 "Android cleartext unexpectedly enabled")
            activities = app.findall("activity")
            locker = [x for x in activities if x.attrib.get(a + "name") ==
                      ".v2.locker.NightWaveLockerActivity"]
            need(len(locker) == 1 and locker[0].attrib.get(a + "exported") == "false",
                 "Music locker must not be exported")
            launcher_count = sum(
                1 for activity in activities
                for flt in activity.findall("intent-filter")
                for category in flt.findall("category")
                if category.attrib.get(a + "name") == "android.intent.category.LAUNCHER"
            )
            need(launcher_count == 1, "Exactly one app launcher required")
    except (ET.ParseError, OSError) as exc:
        need(False, f"Invalid Android manifest: {exc}")

    # Enforce consistent Kotlin package paths/unique imports in core cloud files.
    for rel in (
        "v2/cloud/CloudBackupCodec.kt", "v2/cloud/NightWaveCloudWorker.kt",
        "v2/cloud/NightWaveDriveClient.kt", "v2/cloud/CloudOptionalData.kt",
        "v2/locker/NightWaveMusicDriveClient.kt",
        "v2/locker/NightWaveLockerActivity.kt",
        "v2/locker/LockerDownloadCheckpointStore.kt",
        "v2/locker/LockerResumePolicy.kt",
        "v2/locker/LockerUploadResumePolicy.kt",
        "offline/OfflineAudioLibrary.kt",
    ):
        content = m(rel)
        imports = [s for s in content.splitlines() if s.startswith("import ")]
        need(len(imports) == len(set(imports)), "Duplicate imports: " + rel)
        need(content.count("{") == content.count("}"), "Unbalanced braces: " + rel)
        expected_package = "com.nightwave.player." + ".".join(rel.split("/")[:-1])
        need(content.startswith("package " + expected_package + "\n"),
             "Misplaced Kotlin package: " + rel)

    if FAILED:
        print("FAIL: Phase 6V preflight: " + str(len(FAILED)) + " contract violations")
        for message in FAILED:
            print(" - " + message)
        print("No Gradle build or APK approval was attempted by this script.")
        return 1
    print("PASS: 6V source/cloud integration, Android identity and local build gate")
    print(f"PASS: {len(gate_files)} Python regression gates parsed, AndroidManifest parsed")
    print("UNVERIFIED: Kotlin compiler, Gradle/JVM tests, Drive OAuth/E2E and signed APK")
    return 0

if __name__ == "__main__":
    sys.exit(main())
