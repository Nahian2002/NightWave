# NightWave 6V5 Drive Music Vault source gate (Windows PowerShell 5.1).
# Read-only source check. NOT a compilation, Google API login or phone test.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$problems = New-Object 'System.Collections.Generic.List[string]'

function Read-Src([string]$relative) {
    $file = Join-Path $root ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        $script:problems.Add("Missing source: $relative")
        return ''
    }
    [System.IO.File]::ReadAllText($file, [Text.Encoding]::UTF8)
}
function Require([string]$body, [string]$term, [string]$explanation) {
    if (-not $body.Contains($term)) {
        $script:problems.Add("Missing: $explanation")
    }
}
$base = 'app/src/main/java/com/nightwave/player/v2/locker/'
$client = Read-Src ($base + 'NightWaveMusicDriveClient.kt')
$ui = Read-Src ($base + 'NightWaveLockerActivity.kt')
$policy = Read-Src ($base + 'LockerVaultBrowsePolicy.kt')
$tests = Read-Src 'app/src/test/java/com/nightwave/player/v2/locker/LockerVaultBrowsePolicyTest.kt'
$win = Read-Src 'tools/build-windows.ps1'
$workflow = Read-Src '.github/workflows/android.yml'

Require $policy 'data class LockerCloudInventory(' 'bounded and explicit inventory result'
Require $policy 'val hasMore: Boolean' 'partial-listing marker'
Require $policy 'fun browse(' 'local cloud search and sort'
Require $policy 'fun summary(' 'listed-only storage information'
Require $policy 'fun downloadable(' 'verified local decision'
Require $client 'fun inventory(): LockerCloudInventory' 'bounded Drive inventory'
Require $client 'repeat(LockerVaultBrowsePolicy.MAX_PAGES)' 'bounded paged queries'
Require $client 'findMatchingUpload(parent, song, sha256, md5)' 'complete targeted SHA duplicate check'
Require $client 'LockerVaultPagingPolicy.uploadChecksumQuery(folder, sha256)' 'folder-scoped duplicate search'
Require $client 'LockerVaultPagingPolicy.hasUploadMatch(' 'validate exact remote duplicate metadata'
Require $client 'LockerDriveReceiptPolicy.requireCompletedUpload(' 'verified upload finalization'
Require $ui 'client.inventory()' 'new vault UI backend connected'
Require $ui 'LockerVaultBrowsePolicy.browse(' 'search and sorting bound to policy'
Require $ui 'LockerVaultBrowsePolicy.visible(' 'show-more control bound'
Require $ui 'This is not your total Google Drive usage.' 'accurate storage label'
Require $ui 'LockerVaultBrowsePolicy.downloadable(song)' 'verified row controls'
Require $ui 'client.downloadWithRecovery(' 'existing safe download pipeline'
Require $ui 'offline.importVerifiedCloud(staging, track)' 'checked imported local file'
Require $win 'check-phase6v5-integration.ps1' '6V5 Windows build gate'
Require $win ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'real Gradle test/compile gate'
Require $workflow 'python3 tools/verify_v2_phase6v5.py' 'CI source integration gate'
$testCount = ([regex]::Matches($tests, '@Test\s+fun\s+')).Count
if ($testCount -lt 8) { $problems.Add("6V5 JVM regression tests: found $testCount, expected 8") }
foreach ($unsafe in @('.method("DELETE"', '.method("PATCH"',
    '.addQueryParameter("spaces", "appDataFolder")')) {
    if ($client.Contains($unsafe)) { $problems.Add("Forbidden cloud operation: $unsafe") }
}
if ($problems.Count -gt 0) {
    Write-Host ("FAIL: 6V5 integration source gate found {0} error(s)." -f $problems.Count) -ForegroundColor Red
    $problems | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V5 read-only Drive inventory/browser, verified downloads and bounded upload safety wired.' -ForegroundColor Green
Write-Host 'UNVERIFIED: actual Gradle, unit tests, Android test compile, Google Drive and APK/device safety.' -ForegroundColor Yellow
exit 0
