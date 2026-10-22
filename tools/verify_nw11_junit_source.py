#!/usr/bin/env python3
"""NW-11 integration gate: JUnit failure evidence must be current and redacted.

These are source contracts and Python XML fixtures; Windows PowerShell execution
and Android Gradle/JUnit results are separately required.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

def source(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def self_test() -> None:
    fake = """<testsuite><testcase classname="OfflinePlaybackQueueTest"
name="jumpHandlesBoundsWithoutDamagingSelection">
<failure message="expected: b but was: c"/></testcase>
<testcase classname="Second" name="passing"/>
<testcase classname="Third" name="withoutMessage"><failure>assertion failed</failure></testcase>
</testsuite>"""
    root = ET.fromstring(fake)
    failed = [node for node in root.findall(".//testcase")
              if node.find("failure") is not None or node.find("error") is not None]
    assert [x.attrib["name"] for x in failed] == [
        "jumpHandlesBoundsWithoutDamagingSelection", "withoutMessage"
    ]
    assert failed[0].find("failure").get("message") == "expected: b but was: c"
    assert failed[1].find("failure").get("message") is None
    assert len([x for x in root.findall(".//testcase") if x.find("failure") is None]) == 1
    print("PASS: NW11 JUnit XML fixtures distinguish failed and passing tests")

def repo_contract() -> None:
    ps = source("tools/nw11-junit-report.ps1")
    builder = source("tools/build-windows.ps1")
    syntax = source("tools/check-builder-syntax.ps1")
    workflow = source(".github/workflows/android.yml")
    assert "ReadOuterXml()" in ps
    assert "DtdProcessing]::Prohibit" in ps
    assert "$settings.XmlResolver = $null" in ps
    assert "Get-ChildItem -LiteralPath $ResultsDirectory -Filter 'TEST-*.xml'" in ps
    assert "Select-Object -First 300" in ps
    assert "$found.Count -lt 25" in ps and "$totalFailures -ge 50" in ps
    assert "Clean-Field" in ps and "Authorization:" in ps and "<redacted>" in ps
    assert "NO TEST XML: JVM tests did not run" in ps
    assert "NO FAILURES FOUND in sampled XML. This is not proof all tests ran." in ps
    assert "withoutMessage" in ps and "SelfTest" in ps
    assert "nw11-junit-report.ps1" in syntax
    assert "nw11-junit-report.ps1" in builder
    assert "NightWave-jvm-failure-summary.txt" in builder
    assert "Remove-Item -LiteralPath $junitSummary -Force" in builder
    assert "Remove-Item -LiteralPath $testXmlDirectory -Recurse -Force" in builder
    assert builder.index("Remove-Item -LiteralPath $testXmlDirectory -Recurse -Force") < builder.index("& $gradleBat --no-daemon")
    assert "if ($stage -eq 'Gradle compile / unit tests')" in builder
    assert "exit 1" in builder[builder.rfind("} catch {"):]
    assert "fix/nightwave-v2-nw11-offline-compiler-proof" in workflow
    assert "python3 tools/verify_nw11_junit_source.py" in workflow
    for phase in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub",
                  "nw04_premium_player", "nw05_integrated_stability",
                  "nw06_discovery_search", "nw07_release_gate",
                  "nw08_attestation", "nw09_device_reliability"):
        assert f"python3 tools/verify_{phase}.py" in workflow
    assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
    print("PASS: NW11 source contracts for current-build, bounded JVM diagnostics")
    print("UNVERIFIED: PowerShell execution, full Gradle JUnit run, signer or device")

if __name__ == "__main__":
    self_test()
    repo_contract()
