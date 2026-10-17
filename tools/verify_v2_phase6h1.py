#!/usr/bin/env python3
"""6H1 offline source regression gate for the Windows build diagnostic hotfix.

This gate checks the intent and a Python-equivalent regex regression. The real
PowerShell 5.1 self-test still runs on Windows before Gradle/SDK downloads.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
diag = (root / "tools/diagnose-build.ps1").read_text(encoding="utf-8")
builder = (root / "tools/build-windows.ps1").read_text(encoding="utf-8")
workflow = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")

# The former assignment rule ended in \S+ and inadvertently ate the following
# &name=public after ?access_token was already redacted.
assignment = r"(?i)((?:client_secret|refresh_token|access_token|id_token)\s*[:=]\s*)[^\s&#]+"
assert assignment in diag
assert r"(?i)((?:client_secret|refresh_token|access_token|id_token)\s*[:=]\s*)\S+" not in diag
assert "URL token replacement must preserve non-secret query parameters" in diag
assert "multiple URL query credentials must be redacted independently" in diag
assert "token assignment redaction must stop at ampersand" in diag
assert "PASS: NightWave build diagnostic self-test." in diag
assert "The self-test exited without its PASS marker." in builder
assert "$testFailure" in builder
assert "tools/verify_v2_phase6h1.py" in workflow

query = re.compile(r"(?i)([?&](?:token|access_token|refresh_token|api_key|key|client_secret|signature|sig|code)=)[^\s&#]+")
assignment_re = re.compile(assignment)
def mirror(line: str) -> str:
    line = query.sub(r"\g<1><redacted>", line)
    return assignment_re.sub(r"\g<1><redacted>", line)

samples = [
    ("?access_token=FAKE_SECRET&name=public", "?access_token=<redacted>&name=public"),
    ("?client_secret=FAKE1&name=public&token=FAKE2",
     "?client_secret=<redacted>&name=public&token=<redacted>"),
    ("refresh_token = FAKE3&name=public",
     "refresh_token = <redacted>&name=public"),
]
for raw, wanted in samples:
    sanitized = mirror(raw)
    assert sanitized == wanted, (raw, sanitized, wanted)
    assert "FAKE" not in sanitized

print("PASS: 6H1 source/redaction delimiter regression (mirror checks)")
print("PENDING: actual Windows PowerShell self-test, Android Gradle and APK QA")
