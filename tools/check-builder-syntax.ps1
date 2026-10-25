# Local Windows PowerShell parse gate. Fail closed before any Gradle/build work.
# It also prevents an old APK being mistaken for a successful new build.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$dist = Join-Path $project 'dist'
New-Item -ItemType Directory -Path $dist -Force | Out-Null
$scripts = @('build-windows.ps1', 'verify-apk-signature.ps1', 'check-device-update.ps1', 'capture-nightwave-crash.ps1', 'diagnose-build.ps1', 'check-kotlin-compat.ps1', 'check-phase6p-integration.ps1', 'check-phase6v-integration.ps1', 'check-phase6v4-integration.ps1', 'check-phase6v5-integration.ps1', 'check-phase6v5b-integration.ps1', 'check-phase6v5c-integration.ps1', 'check-phase6v6a-integration.ps1', 'check-phase6v6b-integration.ps1', 'check-phase6v6c-integration.ps1', 'check-phase6v6d-integration.ps1', 'check-phase6v6e-integration.ps1', 'check-phase6v6f-integration.ps1', 'smoke-lyrics-kotlin.ps1', 'check-kotlin-bytearray-fixtures.ps1', 'check-phase6v7a-integration.ps1', 'check-phase6v7b-integration.ps1', 'check-nw07-source-gate.ps1', 'get-nw08-source-inputs.ps1', 'nw11-junit-report.ps1', 'check-nw27-integrity.ps1', 'check-nw34-offline-integrity.ps1', 'check-google-oauth.ps1', 'check-nw40-stability.ps1', 'check-nw52-integration.ps1', 'check-nw59-integration.ps1', 'check-builder-syntax.ps1')
$issues = @()

foreach ($name in $scripts) {
    $path = Join-Path $PSScriptRoot $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $issues += ("Missing Windows script: " + $name)
        continue
    }
    # Windows PowerShell 5.1 may decode UTF-8 without a BOM as the system
    # ANSI code page. UTF-8 punctuation can turn into smart quote tokens.
    # This project intentionally ships ASCII-only .ps1 files, including
    # diagnostics, so the file means the same thing on all Windows PCs.
    $unsafeEncoding = $false
    foreach ($byte in [System.IO.File]::ReadAllBytes($path)) {
        if ($byte -gt 127) {
            $unsafeEncoding = $true
            break
        }
    }
    if ($unsafeEncoding) {
        $issues += ('Windows PowerShell 5.1 unsafe non-ASCII script: ' + $name)
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


# Prevent regression of the optional trusted-signer argument bug:
# passing a named -TrustedSignerSHA256 with no value makes powershell.exe -File
# fail with "Missing an argument" before the signer verifier can run.
$builderPath = Join-Path $PSScriptRoot 'build-windows.ps1'
if (Test-Path -LiteralPath $builderPath -PathType Leaf) {
    $builderSource = [System.IO.File]::ReadAllText($builderPath)
    $guardedTrust = $builderSource.Contains('if (-not [string]::IsNullOrWhiteSpace($env:NIGHTWAVE_TRUSTED_SIGNER_SHA256))')
    $optionalAppend = $builderSource.Contains('$verifierArgs += @(''-TrustedSignerSHA256'', $env:NIGHTWAVE_TRUSTED_SIGNER_SHA256)')
    $arrayInvocation = $builderSource.Contains('& powershell.exe @verifierArgs 2>&1 | Out-String')
    $unsafeInvocation = $builderSource.Contains('-TrustedSignerSHA256 $env:NIGHTWAVE_TRUSTED_SIGNER_SHA256 2>&1')
    if (-not $guardedTrust -or -not $optionalAppend -or
        -not $arrayInvocation -or $unsafeInvocation) {
        $issues += 'The APK verifier must omit the optional trusted-signer parameter when unset.'
    }
}

# NW52 preflight must not evaluate $PSScriptRoot in param() defaults.
# powershell.exe -File may leave it empty while binding -SelfTest. Scan
# preflight source here so the same startup bug cannot return unnoticed.
$nw52Preflight = Join-Path $PSScriptRoot 'check-nw52-integration.ps1'
if (Test-Path -LiteralPath $nw52Preflight -PathType Leaf) {
    $nw52Source = [System.IO.File]::ReadAllText($nw52Preflight)
    if ($nw52Source.Contains('$ProjectRoot = (Join-Path $PSScriptRoot')) {
        $issues += 'NW52 must defer default ProjectRoot evaluation until after parameter binding.'
    }
    if (-not $nw52Source.Contains("if ([string]::IsNullOrWhiteSpace(" + '$ProjectRoot' + "))")) {
        $issues += 'NW52 must resolve an omitted ProjectRoot inside the script body.'
    }
    if (-not $nw52Source.Contains('if ($SelfTest)')) {
        $issues += 'NW52 source guard self-test branch has been removed.'
    }
}

# NW59 must keep the same safe ordering as NW52. Never resolve
# ProjectRoot through PSScriptRoot during parameter binding. Also ensure the
# Windows builder runs the newly added read-only source check before Gradle.
$nw59Preflight = Join-Path $PSScriptRoot 'check-nw59-integration.ps1'
if (Test-Path -LiteralPath $nw59Preflight -PathType Leaf) {
    $nw59Source = [System.IO.File]::ReadAllText($nw59Preflight)
    if ($nw59Source.Contains('$ProjectRoot = (Join-Path $PSScriptRoot')) {
        $issues += 'NW59 prematurely resolves ProjectRoot in param defaults.'
    }
    if (-not $nw59Source.Contains('if ($SelfTest)')) {
        $issues += 'NW59 source check must expose a standalone negative self-test.'
    }
}
if (Test-Path -LiteralPath $builderPath -PathType Leaf) {
    $builder = [System.IO.File]::ReadAllText($builderPath)
    if (-not $builder.Contains("-File "+'$nw59Gate'+" -SelfTest") -or
        -not $builder.Contains("-File "+'$nw59Gate'+" -ProjectRoot "+'$project')) {
        $issues += 'NW59 Windows source self-test and project check must run before Gradle.'
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
