# NW61 - read-only verification of the exact last SUCCESSFUL NightWave APK.
# Never accesses Google accounts, OAuth tokens, passwords, keystore secrets,
# private Android data, or the user's local music. Windows PowerShell 5.1 safe.
[CmdletBinding()]
param(
    [string]$ApkPath = '',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'

function Test-VerifiedApkStatus([string]$Status, [string]$ActualSha256) {
    if ($Status -notmatch '(?m)^SUCCESS - NightWave Android debug APK locally verified\r?$') {
        return $false
    }
    $digest = [regex]::Match($Status, '(?m)^SHA256:\\s*([A-Fa-f0-9]{64})\\r?$')
    if (-not $digest.Success) { return $false }
    return [string]::Equals($digest.Groups[1].Value, $ActualSha256,
        [System.StringComparison]::OrdinalIgnoreCase)
}

function Find-NightWaveBuildTools {
    $sdkCandidates = @()
    if ($env:LOCALAPPDATA) {
        $sdkCandidates += (Join-Path $env:LOCALAPPDATA 'NightWaveBuild\android-sdk')
        $sdkCandidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
    }
    if ($env:ANDROID_SDK_ROOT) { $sdkCandidates += $env:ANDROID_SDK_ROOT }
    if ($env:ANDROID_HOME) { $sdkCandidates += $env:ANDROID_HOME }
    foreach ($sdk in ($sdkCandidates | Select-Object -Unique)) {
        $parent = Join-Path $sdk 'build-tools'
        if (-not (Test-Path -LiteralPath $parent -PathType Container)) { continue }
        # Use the newest *complete* installed set, not a hard-coded version
        # that might differ from the actual Android Gradle configuration.
        $versions = @(Get-ChildItem -LiteralPath $parent -Directory |
            Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } |
            Sort-Object -Property @{ Expression = { [version]$_.Name } } -Descending)
        foreach ($candidate in $versions) {
            $signer = Join-Path $candidate.FullName 'apksigner.bat'
            $aapt = Join-Path $candidate.FullName 'aapt.exe'
            if ((Test-Path -LiteralPath $signer -PathType Leaf) -and
                (Test-Path -LiteralPath $aapt -PathType Leaf)) {
                return @{ Signer = $signer; Aapt = $aapt; Version = $candidate.Name }
            }
        }
    }
    throw 'No complete Android SDK Build Tools installation was found.'
}

if ($SelfTest) {
    $hash = 'A' * 64
    $good = "SUCCESS - NightWave Android debug APK locally verified`r`nSHA256: $hash`r`n"
    if (-not (Test-VerifiedApkStatus $good $hash)) {
        throw 'NW61 verified APK status fixture was rejected.'
    }
    if (Test-VerifiedApkStatus $good ('B' * 64)) {
        throw 'NW61 incorrectly accepted a stale or different APK.'
    }
    if (Test-VerifiedApkStatus ($good.Replace('SUCCESS -', 'FAILED -')) $hash) {
        throw 'NW61 incorrectly accepted a FAILED build.'
    }
    if (Test-VerifiedApkStatus 'SUCCESS - NightWave Android debug APK locally verified' $hash) {
        throw 'NW61 incorrectly accepted a missing APK fingerprint.'
    }
    Write-Host 'PASS: OAuth report requires a successful build and matching APK hash.'
    Write-Host 'PASS: stale APK, failed build and missing checksum fixtures rejected.'
    exit 0
}

$root = (Resolve-Path -LiteralPath (Join-Path -Path $PSScriptRoot -ChildPath '..')).Path
$dist = Join-Path $root 'dist'
$report = Join-Path $dist 'NightWave-google-oauth-setup.txt'
if (-not (Test-Path -LiteralPath $dist -PathType Container)) {
    New-Item -ItemType Directory -Path $dist -Force | Out-Null
}
try {
    $statusFile = Join-Path $dist 'NightWave-build-status.txt'
    if (-not (Test-Path -LiteralPath $statusFile -PathType Leaf)) {
        throw 'No locally verified build status exists. Run build-windows.cmd.'
    }
    # Explicit paths must ALSO match the last successful build. Never let
    # an older copied APK masquerade as the file that just passed Gradle.
    if ([string]::IsNullOrWhiteSpace($ApkPath)) {
        $ApkPath = Join-Path $dist 'NightWave-debug.apk'
    }
    if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
        throw 'No locally verified APK exists. Run build-windows.cmd.'
    }
    $status = [IO.File]::ReadAllText($statusFile)
    $apkSha256 = (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash
    if (-not (Test-VerifiedApkStatus $status $apkSha256)) {
        throw 'The selected APK does not match the last SUCCESSFUL build. Do not use a previous APK or report.'
    }
    $tools = Find-NightWaveBuildTools
    $signOutput = (& $tools.Signer verify --min-sdk-version 26 --print-certs $ApkPath 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'Android APK signature verification failed.' }
    if ($signOutput -match '(?m)^Signer #2 certificate') {
        throw 'Unexpected multiple APK signers. Google OAuth identity is inconclusive.'
    }
    $sha1Match = [regex]::Match($signOutput,
        '(?m)^Signer #1 certificate SHA-1 digest:\\s*([0-9a-fA-F]{40})\\r?$')
    if (-not $sha1Match.Success) { throw 'Unable to verify APK signer SHA-1.' }
    $sha1 = $sha1Match.Groups[1].Value.ToUpperInvariant()
    $sha256Match = [regex]::Match($signOutput,
        '(?m)^Signer #1 certificate SHA-256 digest:\\s*([0-9a-fA-F]{64})\\r?$')
    if (-not $sha256Match.Success) { throw 'Unable to verify APK signer SHA-256.' }
    $sha256 = $sha256Match.Groups[1].Value.ToUpperInvariant()
    $info = (& $tools.Aapt dump badging $ApkPath 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to read APK package identity.' }
    $packageMatch = [regex]::Match($info, "(?m)^package:\\s+name='([^']+)'")
    if (-not $packageMatch.Success) { throw 'Unable to confirm APK package identity.' }
    $package = $packageMatch.Groups[1].Value
    if ($package -ne 'com.nightwave.player') {
        throw 'Unexpected APK package identity. No OAuth report was accepted.'
    }
    $lines = @(
        'NightWave Google Drive OAuth - VERIFIED CANDIDATE APK ONLY',
        'No Google login, access token or private app data was accessed.',
        '',
        "Package name: $package",
        "APK certificate SHA-1: $sha1",
        "APK certificate SHA-256: $sha256",
        "APK file SHA-256: $apkSha256",
        "Android build-tools inspection version: $($tools.Version)",
        '',
        'ACTION REQUIRED IN GOOGLE CLOUD (not automatic):',
        '1. Choose your own Google Cloud project > Google Auth Platform > Clients.',
        '2. Register/check an Android OAuth client with EXACT package and SHA-1 above.',
        '3. If OAuth is in Testing mode, ensure the selected account is a test user.',
        '4. Enable Google Drive API in the same project.',
        '5. Scope must be drive.appdata; do not add broad Drive or Gmail scopes.',
        '6. On the phone, run NightWave > You > Google connection again.',
        '7. If error 8 repeats, copy NightWave app Google setup report too.',
        '',
        'This report identifies the verified APK, not the installed phone APK.',
        'Use check-device-update.cmd before any in-place installation.',
        'It does not verify the Google Cloud configuration or live connection.',
        'Do not share OAuth tokens, passwords, account names or client secrets.'
    )
    $lines | Set-Content -LiteralPath $report -Encoding UTF8
    $lines | ForEach-Object { Write-Host $_ }
    Write-Host "Report: $report" -ForegroundColor Cyan
    exit 0
} catch {
    # Overwrite any report from a previous successful build to avoid false
    # impressions that a stale fingerprint belongs to the current APK.
    @(
        'INCONCLUSIVE - no current verified APK Google OAuth report',
        'Run build-windows.cmd successfully and then check-google-oauth.cmd.',
        'No installed NightWave files or personal music were changed.'
    ) | Set-Content -LiteralPath $report -Encoding UTF8
    Write-Host ('Google OAuth report INCONCLUSIVE: ' + $_.Exception.Message) -ForegroundColor Yellow
    exit 1
}
