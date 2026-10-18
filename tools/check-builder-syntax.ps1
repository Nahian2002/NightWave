# Local Windows PowerShell parse gate. Fail closed before any Gradle/build work.
# It also prevents an old APK being mistaken for a successful new build.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$dist = Join-Path $project 'dist'
New-Item -ItemType Directory -Path $dist -Force | Out-Null
$scripts = @('build-windows.ps1', 'verify-apk-signature.ps1', 'check-device-update.ps1', 'capture-nightwave-crash.ps1', 'diagnose-build.ps1', 'check-kotlin-compat.ps1', 'check-phase6p-integration.ps1', 'check-phase6v-integration.ps1', 'check-phase6v4-integration.ps1')
$issues = @()

foreach ($name in $scripts) {
    $path = Join-Path $PSScriptRoot $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $issues += ("Missing Windows script: " + $name)
        continue
    }
    $tokens = $null
    $parseErrors = $null
    [System.Management.Automation.Language.Parser]::ParseFile(
        $path, [ref]$tokens, [ref]$parseErrors
    ) | Out-Null
    foreach ($item in @($parseErrors)) {
        if ($null -eq $item) { continue }
        $issues += ('{0} line {1}, column {2}: {3}' -f
            $name, $item.Extent.StartLineNumber,
            $item.Extent.StartColumnNumber, $item.Message)
    }
}

if ($issues.Count -gt 0) {
    # Preserve a previous local build, only within the generated dist folder.
    $apk = Join-Path $dist 'NightWave-debug.apk'
    $backup = Join-Path $dist 'NightWave-debug-previous.apk'
    try {
        if (Test-Path -LiteralPath $apk -PathType Leaf) {
            Copy-Item -LiteralPath $apk -Destination $backup -Force
            Remove-Item -LiteralPath $apk -Force
        }
    } catch {
        $issues += "Unable to archive the prior APK: $($_.Exception.Message)"
    }

    $report = Join-Path $dist 'NightWave-build-errors.txt'
    $status = Join-Path $dist 'NightWave-build-status.txt'
    @('NightWave Windows PowerShell syntax check failed.', '') + $issues |
        Set-Content -LiteralPath $report -Encoding UTF8
    @('FAILED - builder syntax/preflight.', 'No new verified APK was published.',
        "Diagnostics: $report") |
        Set-Content -LiteralPath $status -Encoding UTF8
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}

Write-Host 'NightWave Windows builder, signer, device-check, crash-capture and diagnostic scripts syntax: PASS.' -ForegroundColor Green
exit 0
