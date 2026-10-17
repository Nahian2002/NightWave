# NightWave Phase 5S: offline, read-only and privacy-reduced build failure triage.
# No Android/ADB access. No network. Does not touch a build APK or user music.
[CmdletBinding()]
param(
    [string]$BuildLog = '',
    [string]$ErrorReport = '',
    [string]$BuildStatus = '',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$dist = Join-Path $project 'dist'
if (-not $BuildLog) { $BuildLog = Join-Path $dist 'NightWave-build-full.log' }
if (-not $ErrorReport) { $ErrorReport = Join-Path $dist 'NightWave-build-errors.txt' }
if (-not $BuildStatus) { $BuildStatus = Join-Path $dist 'NightWave-build-status.txt' }

function Redact-Line([string]$line) {
    $clean = $line -replace '(?i)file:///([A-Z]:)/Users/[^/]+/', 'file:///$1/Users/<user>/'
    $clean = $clean -replace '(?i)([A-Z]:\\Users\\)[^\\]+\\', '$1<user>\'
    # Never surface auth header values or common URL credential parameters.
    $clean = $clean -replace '(?i)(Authorization:\s*(?:Bearer|Basic)\s+)\S+', '$1<redacted>'
    $clean = $clean -replace '(?i)([?&](?:token|access_token|refresh_token|api_key|key|client_secret|signature|sig|code)=)[^\s&#]+', '$1<redacted>'
    $clean = $clean -replace '(?i)((?:client_secret|refresh_token|access_token|id_token)\s*[:=]\s*)\S+', '$1<redacted>'
    return $clean.TrimEnd()
}
function Summarize([string[]]$lines) {
    $findings = [Collections.Generic.List[string]]::new()
    $patterns = @(
        '^\s*e:\s', ':\s+error:', 'Unresolved reference', 'Type mismatch',
        'Overload resolution ambiguity', 'No value passed for parameter',
        'Cannot infer', 'KSP.*error', 'Execution failed for task',
        '^> Task .* FAILED', '^FAILURE:', 'Could not resolve',
        'Could not find', 'SSLHandshakeException', 'SDK location not found',
        'License for package', 'licenses have not been accepted',
        'BUILD FAILED', 'There were failing tests', 'tests completed, .*failed'
    )
    foreach ($line in $lines) {
        foreach ($pattern in $patterns) {
            if ($line -match $pattern) {
                $findings.Add((Redact-Line ([string]$line)))
                break
            }
        }
        if ($findings.Count -ge 65) { break }
    }
    return @($findings.ToArray())
}
if ($SelfTest) {
    $sample = @(
        "e: file:///C:/Users/demo/Downloads/NightWave/app/src/main/Example.kt:7:1 Unresolved reference 'Surface'.",
        "> Task :app:compileDebugKotlin FAILED",
        "Authorization: Bearer imaginary-test-secret"
    )
    $got = @(Summarize -lines $sample) -join [Environment]::NewLine
    $credentialCheck = Redact-Line 'Authorization: Bearer imaginary-test-secret'
    $urlCheck = Redact-Line 'Could not resolve https://example.org/file?access_token=imaginary-secret&name=public'
    if ($urlCheck -match 'imaginary-secret' -or
        $urlCheck -notmatch 'access_token=<redacted>' -or
        $urlCheck -notmatch 'name=public') {
        Write-Host 'FAIL: URL credential redaction.' -ForegroundColor Red
        exit 1
    }
    if ($got -notmatch 'Unresolved reference' -or $got -notmatch 'compileDebugKotlin FAILED' -or
        $got -match 'Users/demo|imaginary-test-secret' -or $got -notmatch 'Users/<user>' -or
        $credentialCheck -match 'imaginary-test-secret' -or
        $credentialCheck -notmatch '<redacted>') {
        Write-Host 'FAIL: Build report filtering or redaction.' -ForegroundColor Red
        exit 1
    }
    Write-Host 'PASS: NightWave build diagnostic self-test.' -ForegroundColor Green
    exit 0
}
try {
    New-Item -ItemType Directory -Path $dist -Force | Out-Null
    $status = if (Test-Path -LiteralPath $BuildStatus -PathType Leaf) {
        @((Get-Content -LiteralPath $BuildStatus -TotalCount 16))
    } else { @('No build-status file was produced by this attempt.') }
    $errors = if (Test-Path -LiteralPath $ErrorReport -PathType Leaf) {
        @((Get-Content -LiteralPath $ErrorReport -TotalCount 500))
    } else { @() }
    $log = if (Test-Path -LiteralPath $BuildLog -PathType Leaf) {
        @((Get-Content -LiteralPath $BuildLog -Tail 25000))
    } else { @() }
    $failureLines = @(Summarize -lines ($errors + $log))
    if ($failureLines.Count -eq 0) {
        if ($errors.Count -gt 0) {
            $failureLines = @($errors | Select-Object -First 14 | ForEach-Object {
                Redact-Line ([string]$_)
            })
        } else {
            $failureLines = @('No compiler/test failure lines were available.',
                'A missing log does not mean compilation succeeded.')
        }
    }
    $report = Join-Path $dist 'NightWave-build-diagnostic.txt'
    @(
        'NightWave build recovery summary (Phase 6G)',
        'Read-only: no installed APK, account, playlist or phone data modified.',
        'Important: CI artifacts may not match the installed app signing key.',
        'Build outcome/status (do not treat an old APK as a new one):'
    ) + @($status | ForEach-Object { Redact-Line ([string]$_) }) +
        @('', 'First relevant build/compiler/dependency failures:') +
        @($failureLines) |
        Set-Content -LiteralPath $report -Encoding UTF8
    Write-Host ('Build report: ' + $report) -ForegroundColor Cyan
    Write-Host ('Summary: ' + [Math]::Min($failureLines.Count, 65) + ' matching lines')
    exit 0
} catch {
    Write-Host ('Diagnostic collector failed: ' + $_.Exception.Message) -ForegroundColor Yellow
    exit 1
}
