# NightWave fast local Kotlin source preflight.
# Standard Windows PowerShell 5.1; no Python, Gradle, SDK or network needed.
# This is a guard against known bad imports, NOT a Kotlin compiler.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$sourceRoot = Join-Path $root 'app\src'
if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) {
    throw 'NightWave app/src is missing; use the complete branch ZIP.'
}
$files = @(Get-ChildItem -LiteralPath $sourceRoot -Filter '*.kt' -Recurse -File)
if ($files.Count -lt 1) { throw 'No Kotlin source files were found.' }
$invalid = @(
    'androidx.compose.ui.test.assertExists',
    'androidx.compose.ui.test.assertDoesNotExist',
    'androidx.compose.foundation.layout.weight'
)
$issues = New-Object System.Collections.Generic.List[string]
foreach ($file in $files) {
    $imports = @()
    foreach ($line in [System.IO.File]::ReadAllLines($file.FullName)) {
        if (-not $line.StartsWith('import ')) { continue }
        $name = $line.Substring(7).Trim().Split(' ')[0]
        $imports += $name
        if ($invalid -contains $name) {
            $issues.Add(('Invalid Compose import in {0}: {1}' -f
                $file.FullName.Substring($root.Length + 1), $name))
        }
    }
    # Compose sizing methods are top-level extension functions. A reference
    # compiles only when its foundation.layout import is in the same file.
    # Check every Kotlin file, not just the three positions from the 6H1 log.
    $source = [System.IO.File]::ReadAllText($file.FullName)
    if ($source -match '\.\s*fillMaxSize\s*\(' -and
        -not ($imports -contains 'androidx.compose.foundation.layout.fillMaxSize') -and
        -not ($imports -contains 'androidx.compose.foundation.layout.*')) {
        $issues.Add(('Missing androidx.compose.foundation.layout.fillMaxSize import in {0}' -f
            $file.FullName.Substring($root.Length + 1)))
    }
    $duplicates = @($imports | Group-Object | Where-Object { $_.Count -gt 1 })
    foreach ($duplicate in $duplicates) {
        $issues.Add(('Duplicate Kotlin import in {0}: {1}' -f
            $file.FullName.Substring($root.Length + 1), $duplicate.Name))
    }
}
# Known Kotlin compiler errors from the 2026-10-19 Windows build.
# Keep the preflight narrow: this does not replace API/type checking by Gradle.
$playbackPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\playback\PlaybackService.kt'
$previewPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\net\YouTubeGateway.kt'
$smartPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\v2\library\SmartLibraryPolicy.kt'
if (Test-Path -LiteralPath $playbackPath -PathType Leaf) {
    $playbackSource = [System.IO.File]::ReadAllText($playbackPath)
    if ($playbackSource -match 'AcceptedResultBuilder\s*\(\s*session\s*,\s*controller\s*\)') {
        $issues.Add('Media3 1.6.1 AcceptedResultBuilder requires one MediaSession argument, not session + controller.')
    }
    if ($playbackSource.Contains('MediaSession.ConnectionResult.AcceptedResultBuilder') -and
        -not $playbackSource.Contains('MediaSession.ConnectionResult.AcceptedResultBuilder(session)')) {
        $issues.Add('Missing verified Media3 1.6.1 AcceptedResultBuilder(session) invocation.')
    }
}
if ((Test-Path -LiteralPath $smartPath -PathType Leaf) -and
    (Test-Path -LiteralPath $previewPath -PathType Leaf)) {
    $smartSource = [System.IO.File]::ReadAllText($smartPath)
    $previewSource = [System.IO.File]::ReadAllText($previewPath)
    if (-not $previewSource.Contains('val durationSeconds: Long')) {
        $issues.Add('VideoPreview durationSeconds model contract changed; review Smart Library duration handling.')
    }
    if ($smartSource -match '\b(?:it|song)\.duration\s*(?:>=|>|<=|<|==)') {
        $issues.Add('SmartLibraryPolicy uses VideoPreview.duration, which does not exist; use durationSeconds.')
    }
    if (-not $smartSource.Contains('it.durationSeconds >= 240L')) {
        $issues.Add('Smart Library long-listen durationSeconds boundary check is missing.')
    }
}

$gradlePath = Join-Path $root 'app\build.gradle.kts'
if (-not (Test-Path -LiteralPath $gradlePath)) {
    $issues.Add('Missing app/build.gradle.kts.')
} else {
    $gradle = [System.IO.File]::ReadAllText($gradlePath)
    foreach ($marker in @(
        'applicationId = "com.nightwave.player"',
        'versionCode = 35',
        'versionName = "2.0.0-alpha5s"',
        'id("org.jetbrains.kotlin.plugin.compose")',
        'androidTestImplementation("androidx.compose.ui:ui-test-junit4")'
    )) {
        if (-not $gradle.Contains($marker)) { $issues.Add("Build identity/dependency marker missing: $marker") }
    }
}
if ($issues.Count -gt 0) {
    Write-Host 'NightWave preflight found source/build contract errors:' -ForegroundColor Red
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    throw ('Kotlin preflight failed with {0} error(s).' -f $issues.Count)
}
Write-Host ('PASS: Kotlin source/import preflight ({0} files); no outdated Compose test imports.' -f $files.Count) -ForegroundColor Green
Write-Host 'Android Gradle compilation and instrumentation sources still require real build verification.'
exit 0
