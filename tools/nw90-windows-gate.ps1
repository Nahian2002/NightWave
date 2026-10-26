# NW90 offline Windows integrity and evidence check. No installed-app changes.
[CmdletBinding()]
param([switch]$AfterBuild)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Path $dist -Force | Out-Null
$report = Join-Path $dist 'NightWave-NW90-gate.txt'
$lines = New-Object System.Collections.Generic.List[string]

function Require-File([string]$relative) {
    $path = Join-Path $root $relative
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing required NW90 source file: $relative"
    }
    return $path
}
function Record([string]$text) {
    $lines.Add($text)
    Write-Host $text
}

try {
    Record 'NightWave NW90 local evidence gate'
    Record 'This is NOT public-release approval or a live phone test.'
    $matrixPath = Require-File 'docs\NW90_EXPANDED_139_CASE_MATRIX.csv'
    $rows = @(Import-Csv -LiteralPath $matrixPath -Encoding UTF8)
    if ($rows.Count -ne 139) {
        throw "Acceptance matrix must define exactly 139 cases; got $($rows.Count)."
    }
    $ids = @($rows | Select-Object -ExpandProperty case_id)
    if (@($ids | Select-Object -Unique).Count -ne 139) {
        throw 'Acceptance case IDs are not unique.'
    }
    for ($i = 0; $i -lt $rows.Count; $i++) {
        $expectedId = 'NW90-{0:D3}' -f ($i + 1)
        $row = $rows[$i]
        if ($row.case_id -ne $expectedId -or
            $row.status -notin @('NOT_RUN', 'PASS', 'FAIL', 'BLOCKED') -or
            $row.severity -notin @('P0', 'P1', 'P2') -or
            [string]::IsNullOrWhiteSpace($row.area) -or
            [string]::IsNullOrWhiteSpace($row.scenario) -or
            [string]::IsNullOrWhiteSpace($row.expected) -or
            ($row.status -eq 'PASS' -and [string]::IsNullOrWhiteSpace($row.evidence))) {
            throw "Invalid or unevidenced acceptance row: $expectedId"
        }
    }
    $pending = @($rows | Where-Object { $_.status -eq 'NOT_RUN' }).Count
    $failed = @($rows | Where-Object { $_.status -eq 'FAIL' }).Count
    Record "139-case matrix: STRUCTURE PASS, pending=$pending, failures=$failed"
    foreach ($n in 81..89) {
        $matches = @(
            Get-ChildItem -LiteralPath (Join-Path $root 'tools') -File -Filter ("verify_nw{0}_*.py" -f $n)
        )
        if ($matches.Count -lt 1) { throw "NW$n cumulative source contract missing" }
    }
    Require-File 'tools\nw90_certification.py' | Out-Null
    Require-File 'tools\nw77_public_release_audit.py' | Out-Null
    Require-File 'app\build.gradle.kts' | Out-Null
    # Check our new source attestation on synthetic positive and negative
    # fixtures before Gradle, then revalidate real source after the build.
    $nw101Verifier = Require-File 'tools\nw101-source-attestation.ps1'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $nw101Verifier -SelfTest
    if ($LASTEXITCODE -ne 0) {
        throw 'NW101 source attestation negative/positive self-test failed.'
    }
    Record 'NW101 source attestation fixture tests: PASS (not build certification)'
    Record 'NW81-NW89 source-contract files: PRESENT (not executed by this PowerShell check)'
    if ($AfterBuild) {
        $statusPath = Require-File 'dist\NightWave-build-status.txt'
        $status = Get-Content -LiteralPath $statusPath -Raw -Encoding UTF8
        if ($status -notmatch '(?m)^SUCCESS - NightWave Android debug APK locally verified') {
            throw 'No current-attempt successful Windows build report. No APK certification.'
        }
        if ($status -notmatch 'Signing continuity' -and
            $status -notmatch 'Installed-device signer compatibility') {
            Record 'NOTICE: Installed-app signer still requires separate ADB verification.'
        }
        $junitPath = Require-File 'dist\NightWave-jvm-test-proof.txt'
        $proof = Get-Content -LiteralPath $junitPath -Raw
        if ($proof -notmatch '(?m)^PASS - NightWave NW76 current-attempt JVM test receipt') {
            throw 'No fresh passing >=1000-test JVM proof.'
        }
        $apk = Require-File 'dist\NightWave-debug.apk'
        $digest = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
        if ($status -notmatch [regex]::Escape($digest)) {
            throw 'APK SHA256 does not match local build certification report.'
        }
        # The status receipt must match the actual on-disk source manifest,
        # not only a previously built APK and a possibly stale status file.
        $manifest = Require-File 'dist\NightWave-source-fingerprint.txt'
        $fingerprint = [regex]::Match($status, '(?m)^Source fingerprint SHA256:\s*([a-fA-F0-9]{64})\s*$')
        if (-not $fingerprint.Success) {
            throw 'Build status has no valid source fingerprint digest.'
        }
        $actualFingerprint = (Get-FileHash -LiteralPath $manifest -Algorithm SHA256).Hash
        if ($actualFingerprint -ne $fingerprint.Groups[1].Value) {
            throw 'Build status source fingerprint and manifest content disagree.'
        }
        # Critical NW101 check: a matching manifest file digest alone does not
        # prove that the current source still matches its recorded file hashes.
        # Re-enumerate all tracked app/test/build inputs and hash every file.
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $nw101Verifier -ProjectRoot $root -ManifestPath $manifest
        if ($LASTEXITCODE -ne 0) {
            throw 'NW101 source files differ from this APK build fingerprint. No certification.'
        }
        Record 'NW101 current source file contents/inventory: EXACT BUILD SNAPSHOT'
        Record 'Local Windows Gradle/JVM/APK evidence: PASS (developer/debug only)'
        Record "APK SHA256: $digest"
        Record 'IMPORTANT: APK is NOT cleared to install over existing app until signer check.'
    } else {
        Record 'Build/JUnit/APK: NOT VERIFIED by this preflight.'
    }
    Record 'Public release: NO-GO (NW77 music source rights, Play API review and device gates).'
    Record 'No APK installed; no music, local app data or account state modified.'
    $lines | Set-Content -LiteralPath $report -Encoding UTF8
    exit 0
} catch {
    Record ('NW90 GATE FAIL: ' + $_.Exception.Message)
    Record 'No release or on-device installation is authorized by this result.'
    $lines | Set-Content -LiteralPath $report -Encoding UTF8
    exit 2
}
