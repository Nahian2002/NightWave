# NightWave 6V5C read-only Windows source gate (PowerShell 5.1).
# Not an APK, emulator, Google account or Kotlin compiler test.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$problems = New-Object 'System.Collections.Generic.List[string]'
function Src([string]$rel) {
    $p = Join-Path $root ($rel -replace '/', '\')
    if (-not (Test-Path -LiteralPath $p -PathType Leaf)) {
        $script:problems.Add("Missing $rel")
        return ''
    }
    [IO.File]::ReadAllText($p, [Text.Encoding]::UTF8)
}
function Need([string]$source, [string]$token, [string]$why) {
    if (-not $source.Contains($token)) { $script:problems.Add($why) }
}
function Tests([string]$path, [int]$atLeast) {
    $txt = Src $path
    $n = ([regex]::Matches($txt, '@Test\s+fun\s+')).Count
    if ($n -lt $atLeast) {
        $script:problems.Add("$path only $n tests; expected at least $atLeast")
    }
}
$base = 'app/src/main/java/com/nightwave/player/v2/locker/'
$client = Src ($base + 'NightWaveMusicDriveClient.kt')
$policy = Src ($base + 'LockerVaultPagingPolicy.kt')
$browse = Src ($base + 'LockerVaultBrowsePolicy.kt')
$ui = Src ($base + 'NightWaveLockerActivity.kt')
$build = Src 'tools/build-windows.ps1'
$ci = Src '.github/workflows/android.yml'

Need $client 'fun inventoryMore(previous: LockerCloudInventory)' 'Missing explicit next-page client'
Need $client 'folder == previous.folderId' 'Missing Drive folder continuity'
Need $client 'repeat(minOf(LockerVaultBrowsePolicy.MAX_PAGES, remaining))' 'Page request is unbounded'
Need $client 'LockerVaultPagingPolicy.acceptNextToken(next, seenTokens)' 'Missing repeated-cursor fence'
Need $client 'findMatchingUpload(parent, song, sha256, md5)' 'Upload must use SHA-targeted query'
Need $client 'LockerVaultPagingPolicy.hasUploadMatch(' 'Targeted match metadata check missing'
Need $client 'Cloud duplicate search incomplete; upload not started.' 'Failed duplicate scan must stop upload'
Need $client 'require(current == selected &&' 'Single download exact metadata validation missing'
Need $client 'require(fresh == selected' 'Resumed download exact metadata validation missing'
Need $client 'metadata.optJSONArray("parents")' 'Download must verify folder parent'
Need $client 'LockerDriveReceiptPolicy.requireCompletedUpload(' 'Drive upload receipt gate missing'
Need $policy 'fun uploadChecksumQuery(' 'Safe checksum query policy missing'
Need $policy 'fun acceptNextToken(' 'Safe cursor policy missing'
Need $browse 'MAX_TOTAL_PAGES = 40' 'Maximum 40 pages per session not bounded'
Need $browse 'MAX_DUPLICATE_QUERY_PAGES = 20' 'Duplicate matching search is not bounded'
Need $browse 'fun canLoadMore(' 'Continuation state gate missing'
Need $browse 'val verifiedBytes: Long' 'Verified storage reporting missing'
Need $ui 'else if (action == "more") client.inventoryMore(inventory)' 'Cloud UI continuation not wired'
Need $ui 'nextCloudPageButton' 'Load next batch button missing'
Need $ui 'summary.needsAttentionBytes' 'Storage attention reporting missing'
Need $ui 'client.downloadWithRecovery(' 'Verified download implementation disconnected'
Need $build 'check-phase6v5c-integration.ps1' 'Windows build preflight not enabled'
Need $build ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Real Gradle build gate missing'
Need $ci 'python3 tools/verify_v2_phase6v5c.py' 'GitHub source audit not enabled'
Tests 'app/src/test/java/com/nightwave/player/v2/locker/LockerVaultPagingPolicyTest.kt' 9
Tests 'app/src/test/java/com/nightwave/player/v2/locker/LockerVaultBrowsePolicyTest.kt' 8

foreach ($unsafe in @('listInFolder(parent).firstOrNull', 'require(!cloud.hasMore)',
    '.method("DELETE"', '.method("PATCH"', '.addQueryParameter("spaces", "appDataFolder")')) {
    if ($client.Contains($unsafe)) { $problems.Add("Unsafe old scan or mutation: $unsafe") }
}
if ($problems.Count -gt 0) {
    Write-Host "FAIL: 6V5C preflight found $($problems.Count) issue(s)" -ForegroundColor Red
    $problems | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V5C cursor pagination, safe SHA duplicate queries and stored-file checks wired.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Gradle/Android tests, Google OAuth, cloud API behavior, APK signing.' -ForegroundColor Yellow
exit 0
