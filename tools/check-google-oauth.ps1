# Read-only OAuth registration guide for a locally built NightWave APK.
# Never reads account tokens, app data or keystore private keys.
[CmdletBinding()]
param([string]$ApkPath = '')
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Path $dist -Force | Out-Null
$report = Join-Path $dist 'NightWave-google-oauth-setup.txt'
try {
    if (-not $ApkPath) {
        $candidates = @(
            (Join-Path $dist 'NightWave-debug.apk'),
            (Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk')
        )
        $ApkPath = @($candidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
            Select-Object -First 1)[0]
    }
    if (-not $ApkPath -or -not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
        throw 'No locally built NightWave APK was found. Run build-windows.cmd first.'
    }
    $buildTools = Join-Path $env:LOCALAPPDATA 'NightWaveBuild\android-sdk\build-tools\35.0.0'
    $signer = Join-Path $buildTools 'apksigner.bat'
    $aapt = Join-Path $buildTools 'aapt.exe'
    if (-not (Test-Path -LiteralPath $signer -PathType Leaf) -or
        -not (Test-Path -LiteralPath $aapt -PathType Leaf)) {
        throw 'Android Build Tools 35.0.0 unavailable. Run the Windows builder first.'
    }
    $signature = (& $signer verify --min-sdk-version 26 --print-certs $ApkPath 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    if ($signature -notmatch '(?m)^Signer #1 certificate SHA-1 digest:\s*([0-9a-fA-F]{40})\s*$') {
        throw 'APK signer SHA-1 fingerprint could not be determined.'
    }
    $sha1 = $Matches[1].ToUpperInvariant()
    if ($signature -notmatch '(?m)^Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]{64})\s*$') {
        throw 'APK signer SHA-256 fingerprint could not be determined.'
    }
    $sha256 = $Matches[1].ToUpperInvariant()
    $info = (& $aapt dump badging $ApkPath 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or
        $info -notmatch "(?m)^package:\s+name='([^']+)'") {
        throw 'APK package name could not be verified.'
    }
    $package = $Matches[1]
    if ($package -ne 'com.nightwave.player') {
        throw 'Unexpected package identity. No OAuth registration information generated.'
    }
    $lines = @(
        'NightWave Google Drive OAuth registration diagnostics - LOCAL APK',
        'No credentials, access tokens or private data were read.',
        '',
        "Package name: $package",
        "Signer certificate SHA-1: $sha1",
        "Signer certificate SHA-256: $sha256",
        '',
        'In Google Cloud Console > Google Auth Platform / OAuth Clients:',
        '1. Select your own Google Cloud project (no GitHub Actions required).',
        '2. Configure External OAuth app branding / audience and add yourself as a test user if Testing.',
        '3. Create or check the ANDROID OAuth client.',
        '4. The Android client package name MUST equal the package name above.',
        '5. Its SHA-1 certificate fingerprint MUST match the exact SHA-1 above.',
        '6. Enable Google Drive API for this Google Cloud project.',
        '7. Request only drive.appdata permission for hidden NightWave backup.',
        '8. Reopen NightWave and complete the Google permission screen.',
        '',
        'Important: the APK signing certificate is tied to this local build.',
        'A different PC/debug keystore can require another Android OAuth client.',
        'This is a local certificate/configuration check, not live Google authorization.'
    )
    $lines | Set-Content -LiteralPath $report -Encoding UTF8
    $lines | ForEach-Object { Write-Host $_ }
    Write-Host "Report: $report" -ForegroundColor Cyan
    exit 0
} catch {
    Write-Host ('Google OAuth check inconclusive: ' + $_.Exception.Message) -ForegroundColor Yellow
    exit 1
}
