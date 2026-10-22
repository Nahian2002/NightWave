#!/usr/bin/env python3
"""NW-15C: Kotlin imports are case-sensitive; PowerShell Group-Object is not.

Check all actual Kotlin source files and guard the Windows preflight against
conflating Compose ProgressBarRangeInfo with progressBarRangeInfo. Source-only:
this does NOT compile Android/Media3 or produce an APK.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
preflight = (ROOT / "tools/check-kotlin-compat.ps1").read_text(encoding="ascii")
seek = (ROOT / "app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekTimeline.kt").read_text(encoding="utf-8")
workflow = (ROOT / ".github/workflows/android.yml").read_text(encoding="utf-8")
all_sources = sorted((ROOT / "app/src").rglob("*.kt"))
assert len(all_sources) >= 100, "incomplete project source tree"

def kotlin_imports(source: str) -> list[str]:
    return re.findall(r"^import +([^ \t\r\n]+)", source, flags=re.MULTILINE)

def duplicates(imports: list[str]) -> list[str]:
    seen = set()
    repeated = []
    for name in imports:
        if name in seen:
            repeated.append(name)
        seen.add(name)
    return repeated

upper = "androidx.compose.ui.semantics.ProgressBarRangeInfo"
lower = "androidx.compose.ui.semantics.progressBarRangeInfo"
assert upper != lower and upper.casefold() == lower.casefold()
assert duplicates([upper, lower]) == []
assert duplicates([upper, lower, upper]) == [upper]
assert seek.count("import " + upper) == 1
assert seek.count("import " + lower) == 1

source_errors = []
for file in all_sources:
    for repeated in duplicates(kotlin_imports(file.read_text(encoding="utf-8"))):
        source_errors.append(str(file.relative_to(ROOT)) + ": " + repeated)
assert not source_errors, "Genuine exact import duplicates: " + "; ".join(source_errors[:20])

# Prevent reintroduction of case-insensitive Group-Object detection.
assert "[System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)" in preflight
assert "Get-ExactDuplicateImports $imports" in preflight
assert "Group-Object" not in preflight.split("function Get-ExactDuplicateImports",1)[-1].split("if ($SelfTest)",1)[0].replace(
    "# Group-Object and -contains use case-insensitive comparison by default.", ""
)
assert "ProgressBarRangeInfo" in preflight and "progressBarRangeInfo" in preflight
assert "Kotlin exact duplicate import was not detected." in preflight
assert "if ($SelfTest)" in preflight
assert "-ccontains" in preflight

assert "fix/nightwave-v2-nw15c-case-sensitive-import-preflight" in workflow
assert "python3 tools/verify_nw15c_kotlin_imports.py" in workflow
assert "pwsh -NoProfile -File tools/check-kotlin-compat.ps1 -SelfTest" in workflow
assert "pwsh -NoProfile -File tools/check-kotlin-compat.ps1" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
print(f"PASS: {len(all_sources)} Kotlin source files have no exact duplicate imports")
print("PASS: valid case-distinct Compose imports retained, true duplicates rejected")
print("UNVERIFIED: Windows PowerShell 5.1 run, full Gradle Kotlin compilation, APK")
