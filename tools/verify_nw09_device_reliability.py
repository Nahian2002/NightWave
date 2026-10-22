#!/usr/bin/env python3
"""NW-09 Android reliability source contracts.

Static preflight only: does not prove Google OAuth or audible Android effects.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

activity = read("app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt")
auth_policy = read("app/src/main/java/com/nightwave/player/v2/cloud/CloudAuthorizationRecoveryPolicy.kt")
auth_tests = read("app/src/test/java/com/nightwave/player/v2/cloud/CloudAuthorizationRecoveryPolicyTest.kt")
service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
eq = read("app/src/main/java/com/nightwave/player/v2/ui/NightWaveSoundStudio.kt")
workflow = read(".github/workflows/android.yml")
builder = read("tools/build-windows.ps1")

# The Google PendingIntent can outlive an Android Activity recreation.
assert "override fun onSaveInstanceState(outState: Bundle)" in activity
assert "outState.putString(cloudActionSavedKey, cloudPendingAction)" in activity
assert "outState.putBoolean(cloudResolutionSavedKey, true)" in activity
assert "restoredCloudAction = savedInstanceState?.getString(cloudActionSavedKey)" in activity
assert "cloudPendingAction = restoredCloudAction.orEmpty()" in activity
assert "cloudAwaitingResolution = true" in activity
assert "cloudBusy = true" in activity
assert "CloudAuthorizationRecoveryPolicy.shouldRestore(" in activity
assert "if (!cloudAwaitingResolution ||" in activity

# Unresponsive / obsolete Play Services tasks may not mutate guest libraries.
assert "CloudAuthorizationRecoveryPolicy.requestIsCurrent(" in activity
assert "delay(CloudAuthorizationRecoveryPolicy.timeoutMs())" in activity
assert "cloudRequestGeneration++" in activity
assert "Google Drive authorization timed out before opening" in activity
assert "GoogleApiAvailability.getInstance()" in activity
assert "ConnectionResult.SUCCESS" in activity
assert "Google Play services unavailable (status $playServicesCode)" in activity
assert "Google permission expired. Please start the action again." in activity
assert "fun safeAction(action: String?): String?" in auth_policy
assert "fun requestIsCurrent(" in auth_policy
assert auth_tests.count("@Test") >= 3
assert "onlyKnownPermissionActionsCanSurviveRecreation" in auth_tests
assert "staleOrFinishedRequestsDoNotMutateCurrentUi" in auth_tests
assert "accessToken" not in auth_policy and "accessToken" not in auth_tests
assert "idToken" not in auth_policy and "idToken" not in auth_tests

# An old status from SharedPreferences is not proof of a current audio effect.
assert 'ACTION_EQ_DISCOVER -> {' in service
assert "val activeOnSession = activeEqualizer === probe &&" in service
assert "eqStatus.write(requestedEqPreset, activeOnSession," in service
assert "No active audio session. Play a song to enable the equalizer." in service
assert "No supported equalizer available on this audio session." in service
assert 'if (!capabilities.sessionReady) "WAITING FOR AUDIO"' in eq
assert "for (attempt in 0 until 10)" in eq
assert "if (capabilities.sessionReady) break" in eq
assert "repeat(25)" in eq
assert "delay(160L)" in eq

# Preserve other integrated phases and mandatory compilation/test gates.
for phase in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub",
              "nw04_premium_player", "nw05_integrated_stability",
              "nw06_discovery_search", "nw07_release_gate",
              "nw08_attestation", "nw09_device_reliability"):
    assert f"python3 tools/verify_{phase}.py" in workflow
assert "fix/nightwave-v2-nw09-device-reliability-diagnostics" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
assert "check-nw07-source-gate.ps1" in builder
assert "get-nw08-source-inputs.ps1" in builder

for name, body in (("Activity", activity), ("Sound Studio", eq), ("Service", service)):
    imports = [line for line in body.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + ": duplicate Kotlin imports"
    assert body.count("{") == body.count("}"), name + ": mismatched Kotlin braces"

print("PASS: NW09 source contracts for OAuth recreation/timeout and EQ session diagnostics")
print("UNVERIFIED: Kotlin compilation, JVM test execution, Google consent and Android audio playback")
