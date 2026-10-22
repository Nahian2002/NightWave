#!/usr/bin/env python3
"""NW16 source contracts for explicit permitted HTTPS audio download.

Preflight only, NOT actual Android compile, network simulation or device QA.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

policy = text("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadPolicy.kt")
engine = text("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloader.kt")
screen = text("app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadActivity.kt")
library = text("app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt")
offline_ui = text("app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt")
manifest = text("app/src/main/AndroidManifest.xml")
tests = text("app/src/test/java/com/nightwave/player/offline/PermittedAudioDownloadPolicyTest.kt")
workflow = text(".github/workflows/android.yml")

# Direct user-approved audio sources only. No stream scraping or format lies.
assert "uri.scheme.equals(\"https\", true)" in policy
for denied in ("youtube.com", "youtu.be", "googlevideo.com", "spotify.com"):
    assert denied in policy
assert "uri.userInfo == null" in policy
assert "uri.rawFragment == null" in policy
assert "supportedContentType" in policy
assert "audioSignatureMatches" in policy
assert "MAX_AUDIO_BYTES = 250L * 1024L * 1024L" in policy
assert tests.count("@Test") >= 12

# Download is bounded and cancellable with no silent redirects to internal hosts.
assert ".followRedirects(false)" in engine
assert ".followSslRedirects(false)" in engine
assert "resolved.all(::isPublicAddress)" in engine
assert "callTimeout(8, TimeUnit.MINUTES)" in engine
assert "AtomicBoolean(false)" in engine
assert "currentCall?.cancel()" in engine
assert "received <= PermittedAudioDownloadPolicy.MAX_BYTES" in engine
assert "audioSignatureMatches(" in engine
assert "return OfflineAudioLibrary(context).importPermittedStagedAudio(" in engine
assert "pending.delete()" in engine
assert "response.code == 200" in engine
assert "Content-Type" in engine

# Existing indexed songs cannot be deleted or overwritten during import.
assert "synchronized(OfflineLibraryIndexLock.monitor)" in library
assert "fun importPermittedStagedAudio(staged: File, filename: String)" in library
assert "Files.isSymbolicLink(staged.toPath())" in library
assert "staged.canonicalFile.parentFile == base" in library
assert "pending.renameTo(finalFile)" in library
assert 'putString("items", encode(current + song)).commit()' in library
assert "finalFile.delete()" in library

# Ensure a discoverable UI and no public exported downloader activity.
assert "PermittedAudioDownloadActivity::class.java" in offline_ui
assert 'android:name=".offline.PermittedAudioDownloadActivity" android:exported="false"' in manifest
assert "Download to Offline Library" in screen
assert "Confirm that you have permission" in screen
assert "onDestroy()" in screen and "transfer?.cancel()" in screen
assert "YouTube watch links" in screen

assert "feature/nightwave-v2-nw16-authorized-offline-downloads" in workflow
assert "python3 tools/verify_nw16_permitted_downloads.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
for n in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub",
          "nw04_premium_player", "nw05_integrated_stability",
          "nw06_discovery_search", "nw07_release_gate", "nw08_attestation",
          "nw09_device_reliability", "nw11_junit_source", "nw12_integration",
          "nw13_queue_navigation", "nw14_queue_window", "nw15_queue_intent",
          "nw15b_windows_encoding", "nw15c_kotlin_imports", "nw15d_nw07_imports"):
    assert f"python3 tools/verify_{n}.py" in workflow

for name, value in (("policy", policy), ("engine", engine),
                    ("screen", screen), ("library", library)):
    imports = [line for line in value.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), f"Duplicate import in {name}"

print("PASS: NW16 permissioned download, atomic library import and regression wiring")
print("UNVERIFIED: Android build, real network transfer and device playback")
