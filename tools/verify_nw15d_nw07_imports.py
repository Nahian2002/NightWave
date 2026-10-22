#!/usr/bin/env python3
"""NW-15D: stop the second PowerShell gate treating Kotlin case variants as duplicates.

Source-contract verification only: no PowerShell interpreter, Gradle,
Android compiler or APK is executed by this script.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
gate = (ROOT / "tools/check-nw07-source-gate.ps1").read_text("ascii")
preflight = (ROOT / "tools/check-kotlin-compat.ps1").read_text("ascii")
timeline = (ROOT / "app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekTimeline.kt").read_text("utf-8")
workflow = (ROOT / ".github/workflows/android.yml").read_text("utf-8")

symbol_class = "androidx.compose.ui.semantics.ProgressBarRangeInfo"
symbol_property = "androidx.compose.ui.semantics.progressBarRangeInfo"
assert symbol_class != symbol_property
assert symbol_class.casefold() == symbol_property.casefold()
imports = re.findall(r"^import\s+(\S+)", timeline, flags=re.MULTILINE)
assert imports.count(symbol_class) == 1
assert imports.count(symbol_property) == 1
assert len(imports) == len(set(imports)), "Actual exact duplicate in seek timeline"

# The independent NW07 gate must compare case-sensitively (not through
# Sort-Object / Group-Object default case-insensitive de-duplication).
check_imports = gate.split("function Check-Imports(", 1)[1].split("\ntry {", 1)[0]
assert "[System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)" in check_imports
assert "if (-not $seen.Add($exact))" in check_imports
assert "Sort-Object -Unique)" not in check_imports
assert "Group-Object" not in check_imports
assert 'throw ("Duplicate Kotlin import: " + $label + ": " + $exact)' in check_imports

# Explicit positive and negative PowerShell self-test fixtures.
assert "selftest-case-distinct" in gate
assert "selftest-case-repeat" in gate
assert "Exact duplicate after case-distinct imports was not rejected." in gate
assert "Duplicate import self-test unexpectedly passed." in gate
assert "Check-Imports $timeline 'SeekTimeline'" in gate

# First Kotlin source check stays fixed too.
assert "[System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)" in preflight
assert "Get-ExactDuplicateImports $imports" in preflight

assert "fix/nightwave-v2-nw15d-nw07-exact-imports" in workflow
assert "python3 tools/verify_nw15d_nw07_imports.py" in workflow
assert "pwsh -NoProfile -File tools/check-nw07-source-gate.ps1 -SelfTest" in workflow
assert "pwsh -NoProfile -File tools/check-nw07-source-gate.ps1" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow

# An independent Python case-sensitive control fixture (not PowerShell proof).
def exact_duplicates(names: list[str]) -> list[str]:
    seen = set()
    found = []
    for name in names:
        if name in seen:
            found.append(name)
        seen.add(name)
    return found

assert exact_duplicates([symbol_class, symbol_property]) == []
assert exact_duplicates([symbol_class, symbol_property, symbol_class]) == [symbol_class]
print("PASS: NW07 and NW15C preflights preserve Kotlin case-sensitive imports")
print("PASS: exact duplicate still rejected by negative source/self-test fixtures")
print("UNVERIFIED: Windows PowerShell execution, Gradle JVM tests, APK signer")
