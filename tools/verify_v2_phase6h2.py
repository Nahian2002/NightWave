#!/usr/bin/env python3
"""Phase 6H2: detect the exact Kotlin compiler regression from the user's 6H1 log.

Source-only: never substitutes for ':app:compileDebugKotlin', JVM tests or APK QA.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
source_path = root / "app/src/main/java/com/nightwave/player/v2/ui/StudioComponents.kt"
source = source_path.read_text(encoding="utf-8")
imports = set(re.findall(r"^import\s+([^\s]+)", source, re.MULTILINE))
uses = len(re.findall(r"\.fillMaxSize\s*\(", source))
assert uses == 3, f"Review spotlight sizing usage; found {uses} calls, expected 3"
assert "androidx.compose.foundation.layout.fillMaxSize" in imports, (
    "StudioComponents.kt uses fillMaxSize without the Compose layout extension import"
)
assert "Modifier.fillMaxSize().padding(horizontal = 16.dp" in source
assert "Modifier.fillMaxSize().padding(4.dp)" in source
assert "Modifier.fillMaxSize(.78f)" in source

preflight = (root / "tools/verify_v2_phase6f.py").read_text(encoding="utf-8")
powershell = (root / "tools/check-kotlin-compat.ps1").read_text(encoding="utf-8")
workflow = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
assert r"\.\s*fillMaxSize\s*\(" in preflight
assert "Missing Compose fillMaxSize import" in preflight
assert "Missing androidx.compose.foundation.layout.fillMaxSize import" in powershell
assert r"\.\s*fillMaxSize\s*\(" in powershell
assert "python3 tools/verify_v2_phase6h2.py" in workflow

# A missing extension import must fail the same pure preflight condition.
def has_required_import(text: str) -> bool:
    imports = set(re.findall(r"^import\s+([^\s]+)", text, re.MULTILINE))
    return ("androidx.compose.foundation.layout.fillMaxSize" in imports or
            "androidx.compose.foundation.layout.*" in imports)

assert not has_required_import(
    source.replace("import androidx.compose.foundation.layout.fillMaxSize\n", "")
)
assert has_required_import(source)
print("PASS: 6H2 fills all 3 logged fillMaxSize compiler sites and guards missing imports")
print("PENDING: complete Android Gradle/JUnit/instrumentation, signer and device QA")
