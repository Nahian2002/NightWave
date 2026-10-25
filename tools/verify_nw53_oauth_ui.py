#!/usr/bin/env python3
"""NW53 source gate: safe, non-destructive Google OAuth recovery.

No live Google login, APK signing, Drive access or credential checks occur.
"""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLOUD = ROOT / "app/src/main/java/com/nightwave/player/v2/cloud"
UI = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
TEST = ROOT / "app/src/test/java/com/nightwave/player/v2/cloud"


def check(help_policy: str, account_hub: str, test: str, activity: str) -> None:
    rules = {
        "policy": (
            "fun guidance(stage: CloudAuthorizationFailurePolicy.Stage, code: Int?)",
            "CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK",
            "8 -> Guidance(",
            "10 -> Guidance(",
            "16, 12501 -> Guidance(",
            "true, true)",
            "false, true)",
            "does not mean Drive connected",
            "installed APK signing SHA-1",
        ),
        "hub": (
            "CloudConnectionHelpPolicy.guidance(it.stage, it.code)",
            "openExternalFlow(onConnect)",
            "Retry Google connection",
            "guidance.offerSingleRetry",
            "Copy Google setup report",
            "InstalledApkOAuthIdentity.report(appContext)",
            "CloudAccountPresentation.stage(cloud)",
            "CloudAuthorizationFailureStore(appContext).read()",
            "onExportLocal",
        ),
        "test": (
            "errorEightIsRetryableButDoesNotImplyConnected",
            "configurationTenDoesNotOfferBlindRetry",
            "cancellationKeepsLocalLibraryAndDoesNotAutoRetry",
            "driveHttpCodesAreNotConfusedWithGoogleSignInStatuses",
            "copiedFailureStatusDoesNotShowTokensOrEmails",
        ),
        "activity": (
            "NightWaveCloudAuthorization.request(name)",
            "CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER",
            "CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK",
            "cloudFailureStore.record(stage, code)",
            "finishVerifiedCloudAuthorization(action, token, account)",
        ),
    }
    sources = {"policy": help_policy, "hub": account_hub,
               "test": test, "activity": activity}
    for name, required in rules.items():
        for token in required:
            if token not in sources[name]:
                raise AssertionError(name + ": missing " + repr(token))
    if 'else onConnect()' in account_hub:
        raise AssertionError("Google chooser must not open behind the account hub")
    if "confirmChangeAccount = false\n                    onConnect()" in account_hub:
        raise AssertionError("Google chooser must dismiss change-account dialog")
    if "authToken" in help_policy or "accessToken" in help_policy:
        raise AssertionError("No Google access token in recovery guidance")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    files = [
        (CLOUD / "CloudConnectionHelpPolicy.kt").read_text(encoding="utf-8"),
        (UI / "NightWaveAccountHub.kt").read_text(encoding="utf-8"),
        (TEST / "CloudConnectionHelpPolicyTest.kt").read_text(encoding="utf-8"),
        (UI / "NightWaveStudioActivity.kt").read_text(encoding="utf-8"),
    ]
    check(*files)
    if args.self_test:
        for i, needle, replacement in (
            (0, "does not mean Drive connected", "Drive connected"),
            (1, "openExternalFlow(onConnect)", "onConnect()"),
            (2, "configurationTenDoesNotOfferBlindRetry", "retriesAreAlwaysGood"),
            (3, "cloudFailureStore.record(stage, code)", "cloudFailureStore.clear()"),
        ):
            damaged = files[:]
            assert needle in damaged[i]
            damaged[i] = damaged[i].replace(needle, replacement)
            try:
                check(*damaged)
            except AssertionError:
                continue
            raise AssertionError("NW53 source gate accepted broken " + needle)
        print("NW53 negative OAuth/credential flow fixtures: PASS")
    print("NW53 Google recovery UI source contracts: PASS")
    print("UNVERIFIED: actual Google OAuth error 8, SHA-1 registration and Drive APIs")


if __name__ == "__main__":
    main()
