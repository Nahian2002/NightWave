#!/usr/bin/env python3
"""NW-08 independent source tree and builder evidence checks.

Pure Python, no Android SDK/network/user files. Does NOT run Gradle or
authenticate an APK. Includes negative fixtures to exercise SHA manifests.
"""
from pathlib import Path
import hashlib
import tempfile

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = (
    "app/build.gradle.kts",
    "app/proguard-rules.pro",
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "build-windows.cmd",
)

def input_paths(root: Path) -> list[str]:
    root = root.resolve()
    app = root / "app/src"
    if not app.is_dir():
        raise ValueError("app/src missing")
    files: set[str] = set()
    for child in app.rglob("*"):
        if child.is_symlink():
            raise ValueError("Symlink source not allowed")
        if child.is_file():
            files.add(child.relative_to(root).as_posix())
    for rel in REQUIRED:
        if not (root / rel).is_file():
            raise ValueError(f"Missing mandatory build file: {rel}")
        files.add(rel)
    tools = root / "tools"
    if not tools.is_dir():
        raise ValueError("tools missing")
    for child in tools.rglob("*"):
        if child.is_symlink():
            raise ValueError("Symlinked tooling not allowed")
        if child.is_file() and child.suffix in (".py", ".ps1", ".cmd", ".kt"):
            files.add(child.relative_to(root).as_posix())
    return sorted(files)

def sha_lines(root: Path, paths: list[str]) -> list[str]:
    return [
        hashlib.sha256((root / p).read_bytes()).hexdigest().upper() + "  " + p
        for p in paths
    ]

def fixtures() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-nw08-") as scratch:
        root = Path(scratch)
        for rel in (*REQUIRED, "app/src/main/java/Example.kt", "tools/gate.ps1"):
            p = root / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("original", encoding="utf-8")
        first = input_paths(root)
        digest = sha_lines(root, first)
        assert first == sorted(first) and len(first) == len(set(first))
        assert first == input_paths(root) and digest == sha_lines(root, first)
        # Changes to either Kotlin or tests must fail evidence comparison.
        example = root / "app/src/main/java/Example.kt"
        example.write_text("modified", encoding="utf-8")
        assert digest != sha_lines(root, input_paths(root))
        example.write_text("original", encoding="utf-8")
        assert digest == sha_lines(root, input_paths(root))
        additional = root / "app/src/test/java/NewTest.kt"
        additional.parent.mkdir(parents=True)
        additional.write_text("added", encoding="utf-8")
        assert first != input_paths(root)
        additional.unlink()
        assert first == input_paths(root)
        nested = root / "tools/kotlin/NewSmoke.kt"
        nested.parent.mkdir(parents=True, exist_ok=True)
        nested.write_text("fun main() {}", encoding="utf-8")
        assert "tools/kotlin/NewSmoke.kt" in input_paths(root)
        assert first != input_paths(root)
        nested.unlink()
        assert first == input_paths(root)
        (root / REQUIRED[0]).unlink()
        try:
            input_paths(root)
        except ValueError:
            pass
        else:
            raise AssertionError("Missing config was not rejected")

def repo_checks() -> None:
    sources = input_paths(ROOT)
    assert len(sources) >= 100, "incomplete source ZIP"
    assert any(p.startswith("app/src/main/") for p in sources)
    assert any(p.startswith("app/src/test/") for p in sources)
    assert any(p.startswith("app/src/androidTest/") for p in sources)
    assert "tools/get-nw08-source-inputs.ps1" in sources
    assert "tools/check-nw07-source-gate.ps1" in sources
    assert "tools/build-windows.ps1" in sources
    enumerator = (ROOT / "tools/get-nw08-source-inputs.ps1").read_text(encoding="utf-8")
    builder = (ROOT / "tools/build-windows.ps1").read_text(encoding="utf-8")
    syntax = (ROOT / "tools/check-builder-syntax.ps1").read_text(encoding="utf-8")
    workflow = (ROOT / ".github/workflows/android.yml").read_text(encoding="utf-8")
    assert "Get-ChildItem -LiteralPath $source -File -Recurse -Force" in enumerator
    assert "Get-ChildItem -LiteralPath $tools -File -Recurse -Force" in enumerator
    assert "'.cmd', '.kt'" in enumerator
    assert "tools/kotlin/NW12VerifiedSmoke.kt" in sources
    assert "Sort-Object -Unique" in enumerator
    assert "get-nw08-source-inputs.ps1" in syntax
    assert "$inputs = @(& powershell.exe" in builder
    assert "$finalInputs = @(& powershell.exe" in builder
    assert "Source file list changed during Gradle build." in builder
    assert "Source files changed during Gradle build." in builder
    assert "Source fingerprint manifest changed during build." in builder
    assert "NightWave-source-fingerprint.txt" in builder
    assert "Source file count: $($inputs.Count)" in builder
    assert "get-nw08-source-inputs.ps1 -SelfTest" in workflow
    assert "python3 tools/verify_nw08_attestation.py" in workflow
    print(f"PASS: NW08 full-source SHA policy and negative fixtures ({len(sources)} tracked files)")
    print("UNVERIFIED: PowerShell execution, Gradle tests, APK signer, Android runtime")

if __name__ == "__main__":
    fixtures()
    repo_checks()
