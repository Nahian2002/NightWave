#!/usr/bin/env python3
"""NightWave NW-01 source contract; not a Kotlin compiler or device test."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path: str) -> str:
    return (root / path).read_text(encoding="utf-8")

bridge = read("app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt")
service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
ui = read("app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt")
receipt = read("app/src/main/java/com/nightwave/player/playback/ShuffleModeReceiptStore.kt")
policy = read("app/src/main/java/com/nightwave/player/playback/ShuffleModePolicy.kt")
tests = read("app/src/test/java/com/nightwave/player/playback/ShuffleModePolicyTest.kt")
workflow = read(".github/workflows/android.yml")

assert "private var pendingPublicShuffle" in bridge
assert "private fun visiblePublicShuffle(" in bridge
assert "ShuffleModeReceiptStore(context).matches(" in bridge
assert "state = state.copy(shuffle = enabled)" in bridge
assert "putExtra(PlaybackService.EXTRA_SHUFFLE_REQUEST_ID, requestId)" in bridge
assert "else visiblePublicShuffle(current.shuffle)" in bridge
assert "else visiblePublicShuffle(snapshot.shuffle)" in bridge
assert "EXTRA_SHUFFLE_REQUEST_ID" in service
assert "ShuffleModePolicy.resolve(" in service
assert "ShuffleModeReceiptStore(this).acknowledge(" in service
assert "getBooleanExtra(EXTRA_SHUFFLE_ENABLED" in service
assert "fun acknowledge(requestId: String, enabled: Boolean)" in receipt
assert "fun matches(requestId: String, enabled: Boolean)" in receipt
assert "fun resolve(current: Boolean, explicit: Boolean, enabled: Boolean)" in policy
assert tests.count("@Test") >= 4
assert "rapidOnOffOnRequestsAlwaysEndEnabled" in tests

assert 'Icons.Outlined.Cloud' in ui
assert 'StudioProfileGroup("Google Drive · optional")' in ui
assert "Google Drive sign-in could not start" in ui
assert "authorization.addOnSuccessListener" in ui
assert "Google Drive authorization failed (status $statusCode)" in ui
assert "Google did not return a verified Drive account" in ui
assert "if (!candidate.hasControl())" in service
assert "Waiting for an active audio session" in service
assert "Audibility depends on audio route" in service

assert "fix/nightwave-v2-nw01-shuffle-eq-google-recovery" in workflow
assert "python3 tools/verify_nw01_recovery.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

for name, src in (("bridge", bridge), ("service", service), ("ui", ui)):
    imports = [line for line in src.splitlines() if line.startswith("import ")]
    assert len(set(imports)) == len(imports), name + ": duplicate imports"

print("PASS: NW01 static shuffle/Google/equalizer control contracts")
print("UNVERIFIED: Android Gradle compile, Google authorization, audible equalizer and phone QA")
