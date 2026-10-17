#!/usr/bin/env python3
"""6H read-only release-safety and source-contract gate. NOT Android compilation."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(rel: str) -> str:
    return (root / rel).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
policy = read(base + "v2/playback/PlaybackCommandPolicy.kt")
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
service = read(base + "playback/PlaybackService.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/playback/PlaybackCommandPolicyTest.kt")
workflow = read(".github/workflows/android.yml")
compile_summary = read("tools/ci_compile_summary.py")
gradle = read("app/build.gradle.kts")
windows = read("tools/build-windows.ps1")

# 6G allowed the controller's loaded MediaItem only for the selected ID,
# but an unconditional active.isPlaying shortcut still kept a *wrong*
# loaded song playing when Home Resume was requested after a queue change.
assert "fun resumeSatisfied(" in policy
assert "isPlaying && canResumeLoaded(selectedId, loadedId, playerHasError)" in policy
assert "PlaybackCommandPolicy.resumeSatisfied(" in bridge
assert "selectedMediaId(), active.currentMediaItem?.mediaId" in bridge
assert "active.playerError != null, active.isPlaying" in bridge
assert "active.isPlaying -> Unit" not in bridge
assert "action(PlaybackService.ACTION_RESUME_SAVED)" in bridge
assert "action(PlaybackService.ACTION_RESUME_OFFLINE)" in bridge
assert "PlaybackResolutionPolicy.reusesLoadedTrack(" in service
assert "private fun resumeSavedQueue()" in service
assert "private fun resumeOfflineSession()" in service
assert tests.count("@Test") >= 10
assert "homeResumeIsOnlyIdempotentForTheExactPlayingSelection" in tests

# Fail before SDK provisioning when repository assertions are stale.
assert "- feature/nightwave-v2-phase6g-playback-continuity-diagnostics" in workflow
assert workflow.index("Source integrity, build recovery") < workflow.index("setup-java@v4")
assert workflow.index("python3 tools/verify_v2_phase6h.py") < workflow.index("Install Android SDK 35")
assert "python3 tools/ci_compile_summary.py --self-test" in workflow

# Compiler failure output must be diagnostic-only, bounded, filtered.
assert "MAX_BYTES = 4 * 1024 * 1024" in compile_summary
assert "MAX_FINDINGS = 65" in compile_summary
assert "def self_test() -> None:" in compile_summary
assert "redacted" in compile_summary
assert "access_token" in compile_summary
assert "set -o pipefail" in workflow
assert "tee build/nightwave-gradle.log" in workflow
assert "if: failure()" in workflow
assert "nightwave-ci-build-summary.txt" in workflow
assert "NightWave-6H-build-diagnostic-NO-APK" in workflow
assert "if: success()" in workflow
assert "app/build/outputs/apk/debug/app-debug.apk" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

# App identity, local signed builds and existing data are unchanged.
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert "verify-apk-signature.ps1" in windows
assert "check-device-update.cmd" in read("README.md")
for name, src in (("policy", policy), ("bridge", bridge),
                  ("service", service), ("tests", tests)):
    assert src.count("{") == src.count("}"), name + " braces"
    imports = [line for line in src.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: 6H guarded playing-resume, fail-fast CI source gate and sanitized compiler diagnostics")
print("PENDING: Full Android/Compose/JUnit compilation, signer continuity and device verification")
