#!/usr/bin/env python3
"""NW52 fail-closed, read-only release configuration and evidence inventory.

A passing preflight DOES NOT authorize publication. Local real-device,
Google OAuth, copyright/YouTube permissions, key/commit-history reviews and
Gradle build evidence are not inferable from source files.
No secrets, tokens, personal data or network are read by this script.
"""
from __future__ import annotations

import argparse
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
EXPECTED_PACKAGE = "com.nightwave.player"
MIN_PLAY_SDK = 36
BASELINE_BATCHES = tuple(f"nw{i}" for i in range(43, 53))


def analyse(build: str, manifest: str, workflow: str,
            branch: str, docs: str, checks: dict[str, bool]) -> dict:
    """Pure inputs keep the release gate testable without the Android SDK."""
    issues: list[dict[str, str]] = []
    def block(code: str, detail: str) -> None:
        issues.append({"code": code, "reason": detail})
    def read_version(name: str) -> int | None:
        match = re.search(r"\b" + re.escape(name) + r"\s*=\s*(\d+)", build)
        return int(match.group(1)) if match else None

    code, target, compiled = (read_version(n) for n in
                              ("versionCode", "targetSdk", "compileSdk"))
    if not re.search(r'applicationId\s*=\s*"com\.nightwave\.player"', build):
        block("package-identity", "Unexpected app identity: do not install or ship.")
    if code is None or code < 1:
        block("version-code", "No positive Android versionCode.")
    if target is None or target < MIN_PLAY_SDK:
        block("play-api-36", "Target API 36 is required for a new 2026 Play phone app.")
    if compiled is None or compiled < MIN_PLAY_SDK:
        block("compile-api-36", "Compile Android API 36 for a Play candidate.")
    if target is not None and compiled is not None and target > compiled:
        block("sdk-order", "targetSdk must not exceed compileSdk.")
    try:
        root = ET.fromstring(manifest)
        application = root.find("application")
        if application is None:
            block("android-application", "No Android application element.")
        else:
            if application.get(ANDROID + "allowBackup") != "false":
                block("local-backup", "Private audio and account data must not be implicitly backed up.")
            if application.get(ANDROID + "usesCleartextTraffic") != "false":
                block("cleartext", "Cleartext traffic must remain disabled.")
            if application.get(ANDROID + "name") != ".NightWaveApp":
                block("application-owner", "Unexpected global application class.")
        for component in root.iter():
            if component.tag in ("activity", "service", "receiver", "provider"):
                exported = component.get(ANDROID + "exported")
                if exported == "true":
                    name = component.get(ANDROID + "name", "")
                    if name not in {".v2.ui.NightWaveStudioActivity",
                                    ".playback.PlaybackService",
                                    ".widget.NightWaveWidgetProvider"}:
                        block("exported-component", "Unreviewed exported component: " + name[:80])
        permissions = {p.get(ANDROID + "name") for p in
                       root.findall("uses-permission")}
        for forbidden in ("android.permission.MANAGE_EXTERNAL_STORAGE",
                          "android.permission.QUERY_ALL_PACKAGES",
                          "android.permission.READ_SMS",
                          "android.permission.REQUEST_INSTALL_PACKAGES"):
            if forbidden in permissions:
                block("broad-permission", "Broad Android permission needs manual review: " + forbidden)
    except ET.ParseError:
        block("manifest-xml", "Android manifest cannot be parsed.")
    if "feature/nightwave-nw52-final-integration-release-gates" not in workflow:
        block("ci-branch", "NW52 branch is not covered by Android CI.")
    if "testDebugUnitTest" not in workflow or "compileDebugAndroidTestKotlin" not in workflow:
        block("ci-tests", "Android unit or instrumentation-source test gate missing.")
    if re.search(r"(?m)^\s+(?:publish|deploy):", workflow) or "release/upload-release-asset" in workflow:
        block("unexpected-publication", "Build verification workflow must not publish.")
    if EXPECTED_PACKAGE not in build:
        block("identity", "Expected package not found.")
    if "NO-GO" not in docs or "NW52" not in docs:
        block("release-disclosure", "Release notes must explicitly record NW52 and NO-GO.")
    if not branch.startswith("feature/nightwave-nw52-"):
        block("unsafe-release-ref", "Inspect the exact NW52 integration branch before release.")
    for key, reason in (
        ("full_android_build", "Successful full Gradle build/JUnit/instrumentation evidence missing."),
        ("signer_and_bundle", "Production upload signer, AAB and update continuity unverified."),
        ("physical_device", "Real phone acceptance and crash/ANR testing not proven."),
        ("google_oauth", "Google OAuth Drive upload/readback/disconnect not proven."),
        ("media_rights", "YouTube permissions and recording rights not independently confirmed."),
        ("privacy", "Published privacy policy and accurate data safety declarations missing."),
        ("history_secrets", "Full Git history/refs secrets review not provided."),
        ("dependencies_licenses", "Current dependency/asset license and CVE review not provided."),
    ):
        if not checks.get(key, False):
            block(key, reason)
    seen = set()
    deduped = []
    for issue in issues:
        if issue["code"] not in seen:
            deduped.append(issue)
            seen.add(issue["code"])
    return {
        "batch": "NW52", "branch": branch, "package": EXPECTED_PACKAGE,
        "version_code": code, "target_sdk": target, "compile_sdk": compiled,
        "source_preflight": "REVIEWED_CONFIG" if not any(x["code"] in {
            "package-identity", "version-code", "sdk-order", "manifest-xml",
            "android-application", "application-owner", "cleartext",
            "local-backup", "exported-component", "broad-permission",
            "ci-branch", "ci-tests", "unexpected-publication", "identity",
            "release-disclosure", "unsafe-release-ref"
        } for x in deduped) else "NEEDS_FIXES",
        "release_decision": "NO_GO" if deduped else "REQUIRES_MANUAL_RELEASE_APPROVAL",
        "blockers": deduped,
        "note": "Source-only inspection; unknown evidence always counts as missing.",
    }


def self_test() -> None:
    base = '''android {
        defaultConfig {
            applicationId = "com.nightwave.player"
            versionCode = 42
            compileSdk = 36
            targetSdk = 36
        }
    }'''
    manifest = '''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
        <application android:name=".NightWaveApp"
            android:allowBackup="false" android:usesCleartextTraffic="false">
            <activity android:name=".v2.ui.NightWaveStudioActivity" android:exported="true"/>
        </application></manifest>'''
    workflow = '''name: verify
    # feature/nightwave-nw52-final-integration-release-gates
    run: testDebugUnitTest compileDebugAndroidTestKotlin'''
    ready = {k: True for k in ("full_android_build", "signer_and_bundle",
        "physical_device", "google_oauth", "media_rights", "privacy",
        "history_secrets", "dependencies_licenses")}
    def run(b=base, m=manifest, w=workflow, flags=ready):
        return analyse(b, m, w,
            "feature/nightwave-nw52-final-integration-release-gates",
            "# NW52 / NO-GO until verified", flags)
    assert run()["release_decision"] == "REQUIRES_MANUAL_RELEASE_APPROVAL"
    assert run(flags={})["release_decision"] == "NO_GO"
    assert any(x["code"] == "play-api-36" for x in
               run(b=base.replace("targetSdk = 36", "targetSdk = 35"))["blockers"])
    assert any(x["code"] == "local-backup" for x in
               run(m=manifest.replace('android:allowBackup="false"',
                                      'android:allowBackup="true"'))["blockers"])
    assert any(x["code"] == "exported-component" for x in
               run(m=manifest.replace("</application>",
                   '<service android:name=".SensitiveService" android:exported="true"/></application>'))["blockers"])
    assert any(x["code"] == "unexpected-publication" for x in
               run(w=workflow + "\n  deploy: forbidden")["blockers"])
    print("NW52 synthetic fail-closed release checks: PASS")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--require-release-go", action="store_true",
                        help="Always block unless externally verified/manual approval completed.")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    workflow = (ROOT / ".github/workflows/android.yml").read_text(encoding="utf-8")
    docs = (ROOT / "docs/NW52_FINAL_INTEGRATION.md").read_text(encoding="utf-8")
    # Source control cannot prove live runtime, rights, licensed assets or
    # secrets in Git history. No uploaded attestation is trusted by default.
    report = analyse(build, manifest, workflow,
        "feature/nightwave-nw52-final-integration-release-gates", docs, {})
    if args.json:
        print(json.dumps(report, sort_keys=True, indent=2))
    else:
        print("NW52 release:", report["release_decision"])
        for issue in report["blockers"]:
            print("BLOCKER:", issue["code"], "-", issue["reason"])
    if args.require_release_go:
        return 2 if report["release_decision"] != "REQUIRES_MANUAL_RELEASE_APPROVAL" else 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
