#!/usr/bin/env python3
"""NW-03 source contract for local-first account/Google Drive backup UI.

Source checks are not Android compilation or a Google OAuth device test.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def src(path: str) -> str:
    return (root / path).read_text(encoding="utf-8")

activity = src("app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt")
hub = src("app/src/main/java/com/nightwave/player/v2/ui/NightWaveAccountHub.kt")
policy = src("app/src/main/java/com/nightwave/player/v2/cloud/CloudAccountPresentation.kt")
tests = src("app/src/test/java/com/nightwave/player/v2/cloud/CloudAccountPresentationTest.kt")
state = src("app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupState.kt")
auth = src("app/src/main/java/com/nightwave/player/v2/cloud/NightWaveCloudAuthorization.kt")
workflow = src(".github/workflows/android.yml")

assert "internal fun NightWaveAccountHubDialog(" in hub
assert "Guest profile · stored on this device" in hub
assert "Connecting Drive never signs you out or replaces your library." in hub
assert "CloudAccountPresentation.stage(cloud)" in hub
assert "CloudAccountPresentation.maskedAccount(cloud.account)" in hub
assert "onBackUp" in hub and "onPreviewRestore" in hub and "onCompare" in hub
assert "if (confirmDisconnect)" in hub and "if (confirmChangeAccount)" in hub
assert "onDismiss()" in hub and "fun openExternalFlow" in hub
assert "existing backups remain in the previous account." in hub
assert "It is not a Spotify account, Google account sign-in" in hub
assert "enabled = connected && !busy" not in hub or "AccountBackupToggle" in hub
assert "NightWaveAccountHubDialog(" in activity
assert "var showAccountHub by rememberSaveable" in activity
assert "onOpenAccount = { showAccountHub = true }" in activity
assert "onCloudConnect = { requestCloudAction(\"connect\") }" in activity
assert 'StudioProfileGroup("Google Drive · optional")' in activity
assert "CloudAccountPresentation.title(CloudAccountPresentation.stage(cloud))" in activity
assert "onOpenAccount" in activity
assert 'StudioProfileRow("Disconnect or change Drive"' in activity

assert 'enum class BackupStage { LOCAL_ONLY, CONNECTED_NOT_BACKED_UP, VERIFIED_BACKUP }' in policy
assert "enabled && lastSuccessMs > 0L" in policy
assert "fun maskedAccount(email: String)" in policy
assert "fun canBackUp(status: CloudBackupStatus, busy: Boolean)" in policy
assert tests.count("@Test") >= 5
assert "authorizingDriveDoesNotPretendABackupExists" in tests
assert "onlyVerifiedSuccessCountsAsBackup" in tests
assert "masksOnlyDisplayAndKeepsDomain" in tests
assert "existing backups" in hub

assert "no access or refresh token is ever persisted" in state
assert 'putBoolean("enabled", false)' in state
assert 'fun disconnect(): Boolean' in state
assert 'const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"' in auth
assert "feature/nightwave-v2-nw03-account-backup-hub" in workflow
assert "python3 tools/verify_nw03_account_hub.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

for name, body in (("activity", activity), ("hub", hub), ("policy", policy)):
    imports = [line for line in body.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"

print("PASS: NW-03 guest-first account hub and Drive safety source contracts")
print("UNVERIFIED: Android Gradle compile, JVM execution, Google Play services login, data restore")
