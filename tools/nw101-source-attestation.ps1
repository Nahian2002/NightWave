# NW101: independently re-hash the source files claimed by a Windows build.
# Offline and read-only. No APK installation, user data, credentials or network.
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [string]$ManifestPath = '',
    [switch]$SelfTest
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-FingerprintLines([string[]]$Recorded, [string[]]$Actual) {
    if ($Recorded.Count -eq 0 -or $Recorded.Count -ne $Actual.Count) {
        throw "NW101 source inventory differs from build manifest."
    }
    for ($i = 0; $i -lt $Recorded.Count; $i++) {
        if ($Recorded[$i] -cnotmatch '^[0-9A-Fa-f]{64}  [^\r\n]+$') {
            throw ("NW101 invalid manifest record at row " + ($i + 1))
        }
        if (-not [string]::Equals($Recorded[$i], $Actual[$i],
                [StringComparison]::Ordinal)) {
            throw ("NW101 changed, missing or reordered source at row " + ($i + 1))
        }
    }
}

function Expect-Rejection([string[]]$Recorded, [string[]]$Actual) {
    $failed = $false
    try {
        Assert-FingerprintLines -Recorded $Recorded -Actual $Actual
    } catch {
        $failed = $true
    }
    if (-not $failed) { throw "NW101 negative fixture was incorrectly accepted." }
}

try {
    if ($SelfTest) {
        $aa = ('A' * 64) + '  app/src/main/A.kt'
        $bb = ('B' * 64) + '  tools/build-windows.ps1'
        $good = @($aa, $bb)
        Assert-FingerprintLines -Recorded $good -Actual $good
        Expect-Rejection -Recorded $good -Actual @((('C' * 64) + '  app/src/main/A.kt'), $bb)
        Expect-Rejection -Recorded $good -Actual @($aa)
        Expect-Rejection -Recorded $good -Actual @($aa, $bb, (('D' * 64) + '  tools/new.kt'))
        Expect-Rejection -Recorded $good -Actual @($bb, $aa)
        Expect-Rejection -Recorded @('not-a-valid-record', $bb) -Actual @('not-a-valid-record', $bb)
        Write-Host 'NW101_SOURCE_ATTESTATION_SELFTEST_PASS cases=6'
        exit 0
    }

    if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
        $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
    }
    $ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
    if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
        $ManifestPath = Join-Path $ProjectRoot 'dist\NightWave-source-fingerprint.txt'
    }
    if (-not (Test-Path -LiteralPath $ManifestPath -PathType Leaf)) {
        throw 'NW101 fingerprint manifest missing; no current successful build evidence.'
    }

    $sourceEnumerator = Join-Path $PSScriptRoot 'get-nw08-source-inputs.ps1'
    if (-not (Test-Path -LiteralPath $sourceEnumerator -PathType Leaf)) {
        throw 'NW101 source enumerator missing from the complete source ZIP.'
    }
    $paths = @(& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $sourceEnumerator -ProjectRoot $ProjectRoot)
    if ($LASTEXITCODE -ne 0 -or $paths.Count -lt 50) {
        throw 'NW101 source enumeration failed or the source tree is incomplete.'
    }
    $actual = @(
        foreach ($relative in $paths) {
            $full = Join-Path $ProjectRoot $relative.Replace('/', [IO.Path]::DirectorySeparatorChar)
            if (-not (Test-Path -LiteralPath $full -PathType Leaf)) {
                throw ('NW101 tracked source missing: ' + $relative)
            }
            $hash = (Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash
            "$hash  $relative"
        }
    )
    $recorded = @(Get-Content -LiteralPath $ManifestPath -Encoding UTF8)
    Assert-FingerprintLines -Recorded $recorded -Actual $actual
    Write-Host ("NW101_SOURCE_SNAPSHOT_MATCH files=" + $actual.Count)
    exit 0
} catch {
    [Console]::Error.WriteLine('NW101_SOURCE_ATTESTATION_FAIL: ' + $_.Exception.Message)
    exit 2
}
