#!/usr/bin/env python3
"""NW-12 source and actual local compiler evidence boundary.

Our local compiler run used six exact production blobs (hashes below) and 28
assertions, on kotlinc 1.9.0 / Java 21. No Android compilation was attempted.
This script has deterministic JUnit XML regression fixtures that can run
without Java or Android; it never converts source checks into APK readiness.
"""
from pathlib import Path
import hashlib
import xml.etree.ElementTree as ET
import sys

ROOT = Path(__file__).resolve().parents[1]
SMOKE_SHAS = {
    "app/src/main/java/com/nightwave/player/playback/ShuffleModePolicy.kt":
        "02a44ca0cbc13efc1b451473bde09d6358159672",
    "app/src/main/java/com/nightwave/player/v2/playback/PlaybackSeekIdentityPolicy.kt":
        "2915c16bc8da76eb8d1ba1d0b4ad097048a30297",
    "app/src/main/java/com/nightwave/player/v2/cloud/CloudAuthorizationRecoveryPolicy.kt":
        "aba73546782875719a3a9d7254cfedaed67dcebb",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekGesturePolicy.kt":
        "7663db71c6b24ced7fa9990359ac913b87cd41f7",
    "app/src/main/java/com/nightwave/player/v2/ui/NightWaveArtworkPolicy.kt":
        "d098e59c207ef8b292b2c37b90f7ed887b9b9323",
    "app/src/main/java/com/nightwave/player/playback/SoundStudioPolicy.kt":
        "f1b56e571dc41912563ed58d6567594ad08f1c62",
}

def fixture() -> None:
    # Adjacent failures must all appear, not alternate cases skipped by
    # read-then-advance, and a missing message attribute cannot discard a case.
    xml = """<testsuite><testcase name="A" classname="Suite"><failure message="one"/></testcase>
    <testcase name="B" classname="Suite"><error message="two"/></testcase>
    <testcase name="C" classname="Suite"><failure>third</failure></testcase>
    <testcase name="D" classname="Suite"/>
    <testcase name="E" classname="Suite"><failure message="five"/></testcase></testsuite>"""
    tree = ET.fromstring(xml)
    fail = [n for n in tree.findall(".//testcase")
            if n.find("failure") is not None or n.find("error") is not None]
    assert [n.attrib["name"] for n in fail] == ["A", "B", "C", "E"]
    assert fail[2].find("failure").get("message") is None
    assert len(tree.findall(".//testcase")) == 5
    print("PASS: four adjacent/near-adjacent failing JUnit cases retained")

def git_blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()

def repo_check() -> None:
    for path, expected in SMOKE_SHAS.items():
        source = ROOT / path
        assert source.is_file(), "missing compiler-tested policy " + path
        actual = git_blob_sha(source.read_bytes())
        assert actual == expected, f"Policy changed since actual NW12 compiler run: {path}"
    smoke = (ROOT / "tools/kotlin/NW12VerifiedSmoke.kt").read_text(encoding="utf-8")
    runner = (ROOT / "tools/nw12_verified_kotlin.py").read_text(encoding="utf-8")
    assert smoke.count('expect("') == 28
    assert "PASS NW-12 hash-matched source compilation" in smoke
    assert "kotlinc" in runner and "java" in runner
    assert "return 2" in runner, "missing compiler must never claim PASS"
    report = (ROOT / "tools/nw11-junit-report.ps1").read_text(encoding="utf-8")
    assert "$doc.Load($reader)" in report
    assert "SelectNodes('//testcase[failure or error]')" in report
    assert "DtdProcessing]::Prohibit" in report
    assert "$found.Count -ne 4" in report
    assert "ReadOuterXml()" not in report.split("function Collect-TestFailures", 1)[1].split(
        "function Read-JUnit", 1)[0].replace("# ReadOuterXml followed by Read() skips the next sibling test case.", "")
    enumerator = (ROOT / "tools/get-nw08-source-inputs.ps1").read_text(encoding="utf-8")
    assert "Get-ChildItem -LiteralPath $tools -File -Recurse -Force" in enumerator
    assert "'.ps1', '.py', '.cmd', '.kt'" in enumerator
    attestation = (ROOT / "tools/verify_nw08_attestation.py").read_text(encoding="utf-8")
    assert 'tools.rglob("*")' in attestation
    assert 'tools/kotlin/NewSmoke.kt' in attestation
    ci = (ROOT / ".github/workflows/android.yml").read_text(encoding="utf-8")
    assert "fix/nightwave-v2-nw12-test-failure-parser" in ci
    assert "python3 tools/verify_nw12_integration.py" in ci
    assert "python3 tools/nw12_verified_kotlin.py" in ci
    assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in ci
    print("PASS: six original GitHub policy blobs still match compiler-tested SHA identities")
    print("PASS: JUnit parser and nested source inventory contracts preserved")
    print("UNVERIFIED: Windows PowerShell execution; full Android Gradle build and APK")

if __name__ == "__main__":
    fixture()
    if "--self-test" not in sys.argv:
        repo_check()
