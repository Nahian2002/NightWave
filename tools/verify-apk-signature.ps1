# Verify NightWave debug APK identity and signing before delivery.
# No APK installation, keystore mutation, network calls or device changes.
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$ApkPath,
    [Parameter(Mandatory=$true)][string]$BuildToolsDir,
    [Parameter(Mandatory=$true)][string]$VersionName,
    [Parameter(Mandatory=$true)][string]$VersionCode,
    [string]$PreviousApkPath = ''
)

$ErrorActionPreference = 'Stop'

function Read-Signer([string]$File, [string]$Apksigner) {
    $output = (& $Apksigner verify --min-sdk-version 26 --verbose --print-certs $File 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
        throw "APK signature verification failed: $([IO.Path]::GetFileName($File))"
    }
    if ($output -notmatch '(?m)^Signer #1 certificate SHA-256 digest:\s*([0-9A-Fa-f]{64})\s*$') {
        throw 'Cannot determine the APK signer certificate digest.'
    }
    $digest = $Matches[1].ToUpperInvariant()
    if ($output -match '(?m)^Signer #2 certificate SHA-256 digest:') {
        throw 'Unexpected multiple signers; debug APK requires one signer.'
    }
    return $digest
}

try {
    if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
        throw 'APK file is missing.'
    }
    if ((Get-Item -LiteralPath $ApkPath).Length -lt 100000) {
        throw 'APK is unexpectedly small; refusing to publish.'
    }

    $aapt = Join-Path $BuildToolsDir 'aapt.exe'
    $apksigner = Join-Path $BuildToolsDir 'apksigner.bat'
    if (-not (Test-Path -LiteralPath $aapt) -or
        -not (Test-Path -LiteralPath $apksigner)) {
        throw 'Android Build Tools aapt.exe or apksigner.bat is missing.'
    }

    $badging = (& $aapt dump badging $ApkPath 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'Android aapt could not inspect the APK.' }
    if ($badging -notmatch "(?m)^package:\s+name='([^']+)'\s+versionCode='([^']+)'\s+versionName='([^']+)'") {
        throw 'APK package identity information was not readable.'
    }
    $package = $Matches[1]
    $code = $Matches[2]
    $version = $Matches[3]
    if ($package -ne 'com.nightwave.player' -or
        $code -ne $VersionCode -or $version -ne $VersionName) {
        throw "Wrong APK package/version: $package, code=$code, name=$version."
    }

    $newSigner = Read-Signer $ApkPath $apksigner
    $continuity = 'NOT CHECKED - no previous APK supplied. Installed-device signer must be checked separately.'
    if (-not [string]::IsNullOrWhiteSpace($PreviousApkPath) -and
        (Test-Path -LiteralPath $PreviousApkPath -PathType Leaf)) {
        $previousBadging = (& $aapt dump badging $PreviousApkPath 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0 -or
            $previousBadging -notmatch "(?m)^package:\\s+name='com\\.nightwave\\.player'") {
            throw 'The previous APK is not a verifiable NightWave package.'
        }
        $previousSigner = Read-Signer $PreviousApkPath $apksigner
        if ($previousSigner -ne $newSigner) {
            throw 'APK signing certificate changed. Keep the existing app; do NOT uninstall or clear data.'
        }
        $continuity = 'MATCH - same signer as previous APK.'
    }

    Write-Output 'NightWave APK verification PASSED'
    Write-Output "Package: $package"
    Write-Output "Version: $version"
    Write-Output "Version code: $code"
    Write-Output "Signer SHA256: $newSigner"
    Write-Output "Signing continuity: $continuity"
    exit 0
} catch {
    Write-Host ('NightWave APK verification FAILED: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
