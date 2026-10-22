#!/usr/bin/env python3
"""Guard NightWave's Windows PowerShell 5.1 scripts against ANSI mis-decoding.

Windows PowerShell 5.1 may parse BOM-less UTF-8 .ps1 source as its default
ANSI code page. In particular, UTF-8 em dash -> CP1252 smart double quote
breaks its parser. This deliberately requires plain ASCII for all .ps1 files.
It verifies source contracts; actual Windows/Gradle execution is separate.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
scripts = sorted((ROOT / "tools").rglob("*.ps1"))
assert len(scripts) >= 20, "Incomplete scripts folder / source ZIP"

for script in scripts:
    data = script.read_bytes()
    unsafe = [(index, octet) for index, octet in enumerate(data) if octet >= 128]
    assert not unsafe, (
        f"PowerShell 5.1 unsafe bytes in {script.relative_to(ROOT)} "
        f"(first at byte {unsafe[0][0]}, value 0x{unsafe[0][1]:02x}). "
        "Use ASCII or explicitly add a supported BOM."
    )

checker = (ROOT / "tools/check-builder-syntax.ps1").read_text("ascii")
report = (ROOT / "tools/nw11-junit-report.ps1").read_text("ascii")
nw07 = (ROOT / "tools/check-nw07-source-gate.ps1").read_text("ascii")
workflow = (ROOT / ".github/workflows/android.yml").read_text("utf-8")
launcher = (ROOT / "build-windows.cmd").read_text("utf-8")

assert "[System.IO.File]::ReadAllBytes($path)" in checker
assert "if ($byte -gt 127)" in checker
assert "if ($unsafeEncoding)" in checker
assert "continue" in checker
assert "[System.Management.Automation.Language.Parser]::ParseFile(" in checker

# Prevent the original NW11 em dash from coming back as an ANSI smart quote.
assert "$found.Add(('{0}.{1} - {2}' -f $className, $name, $message))" in report
assert "if ($found.Count -ne 4" in report
assert "adjacentFailure" in report
assert "JUnit failure extraction self-test failed." in report
assert "PASS: NW07 complete source tree and NW01-NW06 integration preflight" in nw07

# Verify all PowerShell scripts are registered in the Windows preflight.
import re
registered = set(re.findall(r"'([a-zA-Z0-9_-]+\.ps1)'", checker))
paths = {p.name for p in scripts}
assert registered == paths, (
    "Windows syntax preflight script set mismatch: "
    f"missing={sorted(paths - registered)}, stale={sorted(registered - paths)}"
)
assert "tools\\check-builder-syntax.ps1" in launcher
assert "fix/nightwave-v2-nw15b-windows-powershell-hotfix" in workflow
assert "python3 tools/verify_nw15b_windows_encoding.py" in workflow
assert "pwsh -NoProfile -File tools/check-builder-syntax.ps1" in workflow
assert "pwsh -NoProfile -File tools/nw11-junit-report.ps1 -SelfTest" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

print(f"PASS: {len(scripts)} PowerShell build scripts ASCII-only, registered and protected")
print("PASS: NW11 JUnit string formatting and NW07 source gate use safe ASCII")
print("UNVERIFIED: Windows PowerShell 5.1 execution and full Android Gradle build")
