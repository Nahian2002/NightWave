#!/usr/bin/env python3
"""NW45 Google Drive auth regression gate, not a Gradle or real Google test."""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLOUD = "app/src/main/java/com/nightwave/player/v2/cloud/"
FILES = {
    "flow": "app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt",
    "help": "app/src/main/java/com/nightwave/player/v2/ui/NightWaveAccountHub.kt",
    "scope": CLOUD + "NightWaveCloudAuthorization.kt",
    "identity": CLOUD + "InstalledApkOAuthIdentity.kt",
    "diagnostics": CLOUD + "CloudAuthorizationFailurePolicy.kt",
    "store": CLOUD + "CloudAuthorizationFailureStore.kt",
    "backup": CLOUD + "CloudBackupState.kt",
    "worker": CLOUD + "NightWaveCloudWorker.kt",
    "report_tests": "app/src/androidTest/java/com/nightwave/player/v2/cloud/CloudAuthorizationFailureStoreInstrumentedTest.kt",
    "backup_tests": "app/src/androidTest/java/com/nightwave/player/v2/cloud/CloudFirstBackupConsentInstrumentedTest.kt",
    "policy_tests": "app/src/test/java/com/nightwave/player/v2/cloud/CloudAuthorizationFailurePolicyTest.kt",
}
NEEDS = {
    "flow": [
        "CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER",
        "CloudAuthorizationFailurePolicy.Stage.REQUEST",
        "CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK",
        "cloudFailureStore.record(stage, code)",
        "cloudFailureStore.clear()",
        "cloudStore.successIfCurrent(",
        "CloudBackupConsentPolicy.same(operationConsent,",
        "NightWaveCloudAuthorization.request(name)",
        "NightWaveDriveClient(token).authorizedEmailAddress()",
        "cloudBusy = false",
    ],
    "help": [
        "CloudAuthorizationFailureStore(appContext).read()",
        "CloudAuthorizationFailurePolicy.shortMessage(",
        "InstalledApkOAuthIdentity.signingSha1(appContext)",
        "InstalledApkOAuthIdentity.report(appContext)",
        "https://console.cloud.google.com/auth/clients",
    ],
    "scope": [
        'const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"',
        ".setRequestedScopes(listOf(Scope(SCOPE)))",
    ],
    "identity": [
        "Last failure status: $lastCode",
        "Last failure stage: $lastStage",
        "APK signing certificate SHA-1",
        "Google Play services: $serviceStatus",
    ],
    "diagnostics": [
        "enum class Stage { REQUEST, ACCOUNT_PICKER, DRIVE_CHECK }",
        "8 -> \"Google internal error (8)",
        "10 -> \"Google configuration error (10)",
        "fun canSuggestRetry(code: Int?)",
    ],
    "store": [
        '"nightwave_cloud_auth_diagnostic_v1"',
        '.putString("stage", stage.name)',
        '.putInt("status_code",',
        "fun clear(): Boolean",
    ],
    "backup": [
        "fun successIfCurrent(",
        "CloudBackupConsentPolicy.same(consent, readLocked().consentSnapshot())",
        "fun disconnect(): Boolean",
    ],
    "worker": [
        "CloudAuthorizationFailurePolicy.canSuggestRetry(googleCode)",
        "store.statusIfCurrent(consent,",
        "if (runAttemptCount < 3) Result.retry() else Result.failure()",
    ],
}
def audit(d):
    problems = []
    for key, needles in NEEDS.items():
        for needle in needles:
            if needle not in d.get(key, ""):
                problems.append(f"{key}: missing {needle!r}")
    flow = d.get("flow", "")
    if "cloudStore.success(expectedAccount, System.currentTimeMillis())" in flow:
        problems.append("Manual verified backup still bypasses exact consent revision")
    if "putString(\"access_token\"" in d.get("store", ""):
        problems.append("Diagnostic store must never persist access tokens")
    for k, minimum in (("report_tests", 2), ("backup_tests", 4), ("policy_tests", 4)):
        if d.get(k, "").count("@Test fun ") < minimum:
            problems.append(f"{k}: missing regression declarations")
    if "CloudAuthorizationFailurePolicy.shortMessage(" not in flow:
        problems.append("OAuth errors must be concise and stage-specific")
    return problems

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--root", type=Path, default=ROOT)
    p.add_argument("--self-test", action="store_true")
    args = p.parse_args()
    if args.self_test:
        fake = {k: "\n".join(v) for k,v in NEEDS.items()}
        fake["report_tests"] = "@Test fun test() {}\n" * 2
        fake["backup_tests"] = "@Test fun test() {}\n" * 4
        fake["policy_tests"] = "@Test fun test() {}\n" * 4
        fake["flow"] += "\nCloudAuthorizationFailurePolicy.shortMessage("
        assert not audit(fake), audit(fake)
        bad = dict(fake)
        bad["flow"] += "\ncloudStore.success(expectedAccount, System.currentTimeMillis())"
        assert audit(bad)
        print("PASS: NW45 gate catches non-atomic verified backup regression")
        return 0
    data = {}
    for key, path in FILES.items():
        fp = args.root / path
        if not fp.is_file():
            print("FAIL missing", fp)
            return 1
        data[key] = fp.read_text(encoding="utf-8")
    problems = audit(data)
    for problem in problems:
        print("FAIL:", problem)
    if problems:
        return 1
    print("PASS: NW45 Google authorization, stage diagnostics and safe backup integration")
    print("NOT VERIFIED: Google Cloud registration, Gradle/Android compile or actual account connection")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
