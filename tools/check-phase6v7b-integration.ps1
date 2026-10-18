# NightWave 6V7B premium Search and Library static preflight.
# PowerShell 5.1; no music, APK, cloud, Gradle or device data modified.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$issues = New-Object 'System.Collections.Generic.List[string]'
function Source([string]$rel) {
    $file = Join-Path $root ($rel -replace '/', '\')
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        $script:issues.Add("Missing: $rel")
        return ''
    }
    [IO.File]::ReadAllText($file, [Text.Encoding]::UTF8)
}
function Require([string]$source, [string]$token, [string]$description) {
    if (-not $source.Contains($token)) { $script:issues.Add($description) }
}
function Count-Tests([string]$rel, [int]$minimum) {
    $n = ([regex]::Matches((Source $rel), '@Test\s+fun\s+')).Count
    if ($n -lt $minimum) {
        $script:issues.Add(('{0}: {1} tests; expected {2}' -f $rel, $n, $minimum))
    }
}
$prefix = 'app/src/main/java/com/nightwave/player/v2/ui/'
$policy = Source ($prefix + 'NightWaveBrowseChromePolicy.kt')
$browse = Source ($prefix + 'NightWavePremiumBrowse.kt')
$tiles = Source ($prefix + 'StudioComponents.kt')
$rootUi = Source ($prefix + 'NightWaveStudioActivity.kt')
$windows = Source 'tools/build-windows.ps1'
$ci = Source '.github/workflows/android.yml'
Require $policy 'COMPACT_WIDTH_DP = 440f' 'Missing mobile breakpoint'
Require $policy 'Source.SAVED_LIBRARY' 'Saved items need accurate source label'
Require $policy 'Source.PUBLIC_VIDEO' 'Public results need internet warning'
Require $policy 'fun libraryCounts(' 'Library collection summary missing'
Require $browse 'fun NightWaveBrowseHeader(' 'Premium browse header absent'
Require $browse 'fun NightWaveSearchSourceHeading(' 'Search source grouping absent'
Require $browse 'nightwave-search-header' 'Search header test tag absent'
Require $browse 'nightwave-library-header' 'Library header test tag absent'
Require $tiles 'TrackArtwork(video,' 'Real thumbnail display path removed'
Require $tiles 'TrackArtwork(cover,' 'Real playlist thumbnail display path removed'
Require $tiles 'IconButton(onClick = onOptions' 'Track options action removed'
Require $tiles 'NightWaveBrowseChromePolicy.MIN_TILE_HEIGHT_DP.dp' 'Compact track tile bounds missing'
Require $rootUi 'NightWaveSearchSourceHeading(' 'Search results not source labeled'
Require $rootUi 'responseMatchesQuery' 'Old-query stale result guard missing'
Require $rootUi 'NightWaveBrowseChromePolicy.showCompactCollections(' 'Mobile collection stacking missing'
Require $rootUi 'LibraryBrowsePolicy.playlists(' 'Existing playlist browse filters missing'
Require $rootUi 'LibraryQuickPlayPolicy.plan(' 'Existing playlist playback missing'
Require $rootUi 'onSharePlaylist(selectedPlaylist, tracks)' 'Existing playlist share action missing'
Require $rootUi 'onImportSharedPlaylist' 'Existing shared playlist import missing'
Require $rootUi 'onOffline' 'Existing offline music entry missing'
Require $windows 'check-phase6v7b-integration.ps1' 'Windows integration missing 6V7B gate'
Require $windows ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Real Android compile pipeline missing'
Require $ci 'python3 tools/verify_v2_phase6v7b.py --self-test' 'GitHub source self-test missing'
Count-Tests 'app/src/test/java/com/nightwave/player/v2/ui/NightWaveBrowseChromePolicyTest.kt' 9
Count-Tests 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWavePremiumBrowseInstrumentedTest.kt' 4
Count-Tests 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWavePremiumChromeInstrumentedTest.kt' 3
Count-Tests 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveSwipeTabsInstrumentedTest.kt' 2
if ($issues.Count -gt 0) {
    Write-Host ('FAIL: 6V7B browse preflight has {0} issue(s)' -f $issues.Count) -ForegroundColor Red
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: NightWave 6V7B Search/Library source and playback safeguards connected.' -ForegroundColor Green
Write-Host 'NOT VERIFIED: Android/Compose runtime, downloaded music or APK signer.' -ForegroundColor Yellow
exit 0
