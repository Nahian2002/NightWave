#!/usr/bin/env python3
"""NW109 API 36, build-tool, exported surface and private-transfer gates.
Read-only/static; passing never certifies Android compilation or release.
"""
import argparse
import re
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
DOMAINS = {"root", "device_root", "file", "device_file", "database",
           "device_database", "sharedpref", "device_sharedpref", "external"}

def inspect(root: Path) -> list[str]:
    errors = []
    def read(relative):
        path = root / relative
        if not path.is_file():
            errors.append("missing:" + relative)
            return ""
        return path.read_text(encoding="utf-8")
    app = read("app/build.gradle.kts")
    build = read("build.gradle.kts")
    win = read("tools/build-windows.ps1")
    ci = read(".github/workflows/current-android.yml")
    manifest = read("app/src/main/AndroidManifest.xml")
    extraction = read("app/src/main/res/xml/nightwave_data_extraction_rules.xml")
    styles = read("app/src/main/res/values/styles.xml")
    for marker, name in (
        (r"\bcompileSdk\s*=\s*36\b", "compile-sdk"),
        (r"\btargetSdk\s*=\s*36\b", "target-sdk"),
        (r'applicationId\s*=\s*"com\.nightwave\.player"', "package"),
        (r"\bminSdk\s*=\s*26\b", "min-sdk"),
        (r"\bversionCode\s*=\s*35\b", "version-code"),
        (r'\bversionName\s*=\s*"2\.0\.0-alpha5s"', "version-name"),
    ):
        if not re.search(marker, app): errors.append(name)
    if 'id("com.android.application") version "8.10.1"' not in build:
        errors.append("agp-version")
    if "gradle-8.11.1-bin.zip" not in win or "gradle-version: '8.11.1'" not in ci:
        errors.append("gradle-alignment")
    for text, name in ((win, "windows"), (ci, "ci")):
        if "'platforms;android-36'" not in text or "'platforms;android-35'" in text:
            errors.append(name + "-sdk36")
        if "'build-tools;35.0.0'" not in text:
            errors.append(name + "-build-tools")
    if ":app:testDebugUnitTest" not in win or ":app:compileDebugAndroidTestKotlin" not in ci:
        errors.append("junit-android-test-gates")
    if "windowOptOutEdgeToEdgeEnforcement" in styles:
        errors.append("api36-edge-to-edge-optout")
    try:
        xml = ET.fromstring(manifest)
        application = xml.find("application")
        if application is None: raise ValueError("Missing application")
        for key, expected in (
            ("allowBackup", "false"),
            ("fullBackupContent", "false"),
            ("dataExtractionRules", "@xml/nightwave_data_extraction_rules"),
            ("usesCleartextTraffic", "false"),
        ):
            if application.get(ANDROID + key) != expected:
                errors.append("manifest-" + key)
        exported = {node.get(ANDROID + "name") for node in application.iter()
                    if node.tag in ("activity", "receiver", "service", "provider")
                    and node.get(ANDROID + "exported") == "true"}
        allowed = {".v2.ui.NightWaveStudioActivity",
                   ".playback.PlaybackService", ".widget.NightWaveWidgetProvider"}
        if exported != allowed: errors.append("exported-components")
        permissions = {node.get(ANDROID + "name") for node in xml.findall("uses-permission")}
        required = {"android.permission.INTERNET",
                    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"}
        prohibited = {"android.permission.MANAGE_EXTERNAL_STORAGE",
                      "android.permission.READ_SMS", "android.permission.QUERY_ALL_PACKAGES"}
        if "android.permission.REQUEST_INSTALL_PACKAGES" in permissions:
            updater = application.find("activity[@" + ANDROID + "name='.updates.WeeklyUpdateActivity']")
            providers = [n for n in application.findall("provider") if n.get(ANDROID + "authorities") == "${applicationId}.updates"]
            if updater is None or updater.get(ANDROID + "exported") != "false" or len(providers) != 1:
                errors.append("weekly-updater-private-components")
            elif providers[0].get(ANDROID + "exported") != "false" or providers[0].get(ANDROID + "name") != "androidx.core.content.FileProvider":
                errors.append("weekly-updater-provider")
            paths = read("app/src/main/res/xml/weekly_update_paths.xml")
            if 'path="approved-updates/"' not in paths or "root-path" in paths or 'path="."' in paths:
                errors.append("weekly-updater-file-scope")
        if not required.issubset(permissions) or permissions.intersection(prohibited):
            errors.append("permission-surface")
        services = [node for node in application.findall("service")
                    if node.get(ANDROID + "name") == ".playback.PlaybackService"]
        if len(services) != 1 or services[0].get(ANDROID + "foregroundServiceType") != "mediaPlayback":
            errors.append("foreground-media-service")
    except (ET.ParseError, ValueError):
        errors.append("manifest-xml")
    try:
        xml = ET.fromstring(extraction)
        if xml.tag != "data-extraction-rules":
            errors.append("extraction-root")
        for section in ("cloud-backup", "device-transfer"):
            node = xml.find(section)
            if node is None:
                errors.append("missing-rule-section-" + section)
                continue
            if node.findall("include"): errors.append("unsafe-include-" + section)
            excludes = {(item.get("domain"), item.get("path"))
                        for item in node.findall("exclude")}
            for domain in sorted(DOMAINS):
                if (domain, ".") not in excludes:
                    errors.append("missing-exclude-" + section + "-" + domain)
    except ET.ParseError:
        errors.append("extraction-xml")
    return errors

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nw109-policy-") as d:
        root = Path(d)
        files = {
            "app/build.gradle.kts": (
                'compileSdk = 36\ntargetSdk = 36\napplicationId = "com.nightwave.player"\n'
                'minSdk = 26\nversionCode = 35\nversionName = "2.0.0-alpha5s"'),
            "build.gradle.kts": 'id("com.android.application") version "8.10.1"',
            "tools/build-windows.ps1": (
                "gradle-8.11.1-bin.zip 'platforms;android-36' "
                "'build-tools;35.0.0' :app:testDebugUnitTest"),
            ".github/workflows/current-android.yml": (
                "gradle-version: '8.11.1'\n'platforms;android-36' "
                "'build-tools;35.0.0'\n:app:compileDebugAndroidTestKotlin"),
            "app/src/main/res/values/styles.xml": "<resources/>",
            "app/src/main/AndroidManifest.xml": (
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
                '<uses-permission android:name="android.permission.INTERNET"/>'
                '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>'
                '<application android:allowBackup="false" android:fullBackupContent="false" '
                'android:usesCleartextTraffic="false" '
                'android:dataExtractionRules="@xml/nightwave_data_extraction_rules">'
                '<activity android:name=".v2.ui.NightWaveStudioActivity" android:exported="true"/>'
                '<service android:name=".playback.PlaybackService" android:exported="true" '
                'android:foregroundServiceType="mediaPlayback"/>'
                '<receiver android:name=".widget.NightWaveWidgetProvider" android:exported="true"/>'
                '</application></manifest>'),
            "app/src/main/res/xml/nightwave_data_extraction_rules.xml": (
                "<data-extraction-rules>" + "".join(
                    "<" + section + ">" + "".join(
                        '<exclude domain="' + domain + '" path="."/>'
                        for domain in sorted(DOMAINS)) + "</" + section + ">"
                    for section in ("cloud-backup", "device-transfer"))
                + "</data-extraction-rules>"),
        }
        for name, value in files.items():
            target = root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(value, encoding="utf-8")
        assert inspect(root) == [], inspect(root)
        count = 1
        mutations = (
            ("app/build.gradle.kts", "targetSdk = 36", "targetSdk = 35", "target-sdk"),
            ("build.gradle.kts", "8.10.1", "8.9.2", "agp-version"),
            ("tools/build-windows.ps1", "android-36", "android-35", "windows-sdk36"),
            ("app/src/main/AndroidManifest.xml", 'android:allowBackup="false"',
             'android:allowBackup="true"', "manifest-allowBackup"),
            ("app/src/main/AndroidManifest.xml", 'android:dataExtractionRules="@xml/nightwave_data_extraction_rules"',
             'android:dataExtractionRules="@xml/other"', "manifest-dataExtractionRules"),
            ("app/src/main/AndroidManifest.xml", 'android:exported="true"',
             'android:exported="false"', "exported-components"),
            ("app/src/main/res/xml/nightwave_data_extraction_rules.xml",
             "device-transfer", "missing-transfer", "missing-rule-section-device-transfer"),
            ("app/src/main/res/xml/nightwave_data_extraction_rules.xml",
             'domain="root"', 'domain="unknown"', "missing-exclude-cloud-backup-root"),
        )
        for filename, old, new, expected in mutations:
            target = root / filename
            before = target.read_text(encoding="utf-8")
            target.write_text(before.replace(old, new), encoding="utf-8")
            assert expected in inspect(root), expected
            target.write_text(before, encoding="utf-8")
            count += 1
        assert inspect(root) == [], inspect(root)
        print(f"NW109_ANDROID36_PRIVACY_NEGATIVE_SELFTEST_PASS cases={count + 1}")

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--project-root", type=Path, default=ROOT)
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    errors = inspect(args.project_root)
    if errors:
        print("NW109_CONFIG_AUDIT_FAIL " + ", ".join(errors))
        return 2
    print("NW109_ANDROID36_PRIVACY_SOURCE_GATE_PASS (not Android/device certification)")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
