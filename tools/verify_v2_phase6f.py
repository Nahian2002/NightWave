#!/usr/bin/env python3
"""Phase 6F repository preflight. Structural only; Gradle owns compilation.

Deliberately no downloads, shell execution, app installation or user data I/O.
Checks exactly the classes of regressions seen in earlier Windows build logs.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
app = root / "app"
kotlin_files = sorted((app / "src").rglob("*.kt"))
assert kotlin_files, "No Kotlin source files were found"
bad_imports = {
    # These are semantics methods on Compose test nodes, NOT top-level imports.
    "androidx.compose.ui.test.assertExists",
    "androidx.compose.ui.test.assertDoesNotExist",
    # weight is a RowScope/ColumnScope extension; not a public top-level import.
    "androidx.compose.foundation.layout.weight",
}
for file in kotlin_files:
    text = file.read_text(encoding="utf-8-sig")
    imports = [s.strip().split(" as ")[0] for s in text.splitlines()
               if s.startswith("import ")]
    imports = [s.removeprefix("import ").strip() for s in imports]
    assert len(imports) == len(set(imports)), f"Duplicate Kotlin import: {file.relative_to(root)}"
    invalid = bad_imports.intersection(imports)
    assert not invalid, f"Invalid Compose import in {file.relative_to(root)}: {sorted(invalid)}"
    # Phase 6H1 failed only at three .fillMaxSize() calls, all missing the
    # same Kotlin extension import. Detect the class of failure before Gradle.
    if re.search(r"\.\s*fillMaxSize\s*\(", text):
        assert ("androidx.compose.foundation.layout.fillMaxSize" in imports or
                "androidx.compose.foundation.layout.*" in imports), (
                    f"Missing Compose fillMaxSize import: {file.relative_to(root)}")

gradle = (app / "build.gradle.kts").read_text(encoding="utf-8")
settings = (root / "build.gradle.kts").read_text(encoding="utf-8")
workflow = (root / ".github/workflows/android.yml").read_text(encoding="utf-8")
builder = (root / "tools/build-windows.ps1").read_text(encoding="utf-8")
test_dirs = (app / "src/test/java", app / "src/androidTest/java")
assert all(p.exists() for p in test_dirs), "JVM or Android tests missing"
assert "id(\"org.jetbrains.kotlin.plugin.compose\")" in gradle
assert 'id("com.google.devtools.ksp")' in gradle
assert 'id("com.android.application") version "8.9.2"' in settings
assert 'id("org.jetbrains.kotlin.android") version "2.0.21"' in settings
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert "testImplementation(\"junit:junit:4.13.2\")" in gradle
assert 'androidTestImplementation("androidx.compose.ui:ui-test-junit4")' in gradle
assert "sourceCompatibility = JavaVersion.VERSION_17" in gradle
assert ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' in workflow
assert ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' in builder
assert 'python3 tools/verify_v2_phase6f.py' in workflow
assert 'verify-v2-phase6f' not in workflow.lower(), "Use the exact Python gate filename"
assert 'powershell' in (root / "build-windows.cmd").read_text(encoding="utf-8").lower()

# Kotlin/JUnit API stability for the newest feature stack.
yt = (app / "src/main/java/com/nightwave/player/net/YouTubeGateway.kt").read_text()
assert re.search(r'data class VideoPreview\([^)]*durationSeconds: Long', yt)
policy = (app / "src/main/java/com/nightwave/player/v2/playback/PlaybackHealthPolicy.kt").read_text()
assert "object PlaybackHealthPolicy" in policy
studio = (app / "src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt").read_text()
assert 'SectionHeading("Made for you"' in studio
legacy = (root / "tools/verify_v2_phase5y.py").read_text()
assert 'SectionHeading("Made for you"' in legacy
assert 'SectionHeading("Made for your listening"' not in legacy

# Preservation of source and saved-user-data invariants.
service = (app / "src/main/java/com/nightwave/player/playback/PlaybackService.kt").read_text()
assert 'ThreadPoolExecutor(2, 2, 30L' in service
assert 'listeningMemory.record(pulse, revision)' in service
assert 'cancelPendingResolution()' in service
assert (root / "check-device-update.cmd").exists()
print(f"PASS: 6F Kotlin/import preflight ({len(kotlin_files)} source/test files); "
      "stale Home assertion and identity/build contracts")
print("NOT VERIFIED: Android/Gradle compilation, JUnit, APK signing or real-device playback")
