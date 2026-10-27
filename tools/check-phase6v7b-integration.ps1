# Read-only 6V7B Search & Library UI preflight, PowerShell 5.1.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$issues = New-Object 'System.Collections.Generic.List[string]'
function File([string]$rel) {
    $p = Join-Path $root ($rel -replace '/', '\')
    if (-not (Test-Path -LiteralPath $p -PathType Leaf)) {
        $script:issues.Add("Missing: $rel")
        return ''
    }
    return [IO.File]::ReadAllText($p, [Text.Encoding]::UTF8)
}
function Need([string]$src,[string]$match,[string]$reason) {
    if (-not $src.Contains($match)) { $script:issues.Add($reason) }
}
function Tests([string]$rel,[int]$minimum) {
    $count = ([regex]::Matches((File $rel), '@Test\s+fun\s+')).Count
    if ($count -lt $minimum) { $script:issues.Add(("{0}: only {1} tests" -f $rel,$count)) }
}
$p='app/src/main/java/com/nightwave/player/v2/ui/'
$chrome=File ($p+'NightWaveCollectionChrome.kt')
$headers=File ($p+'NightWaveDiscoveryChrome.kt')
$tiles=File ($p+'StudioComponents.kt')
$screen=File ($p+'NightWaveStudioActivity.kt')
$builder=File 'tools/build-windows.ps1'
$ci=File '.github/workflows/android.yml'
Need $chrome 'enum class NightWaveTrackSurface' 'Track source types absent'
Need $chrome 'fun lineCount(' 'Compact visual width policy absent'
Need $headers 'NightWaveDiscoveryHeader(' 'Premium Search header absent'
Need $headers 'NightWaveCollectionHeader(' 'Premium Library header absent'
Need $headers 'testTag("nightwave-save-offline-search")' 'Offline entry action lost its accessibility test tag'
Need $headers 'FreeAudioCatalogActivity::class.java' 'Offline audio catalog shortcut no longer opens'
Need $screen 'NightWaveDiscoveryHeader(searchQuery = query)' 'Search header not receiving current query'
# NW83 unifies row sizing under premium design tokens; require the current binding
# and the token definition rather than the retired collection chrome constant.
Need $tiles 'NightWavePremiumDesignTokens.ROW_ARTWORK_DP.dp' 'Track artwork sizing not wired'
Need (File ($p+'NightWavePremiumDesignTokens.kt')) 'const val ROW_ARTWORK_DP = 52' 'Premium row artwork sizing definition missing'
Need $tiles '"nightwave-library-collection"' 'Collection test tag missing'
Need $tiles 'IconButton(onClick = onOptions' 'Track option action removed'
Need $screen 'surface = NightWaveTrackSurface.SEARCH' 'Search result cards not wired'
Need $screen 'surface = NightWaveTrackSurface.LIBRARY' 'Library song cards not wired'
Need $screen 'SavedTrackSearch.match(query, savedTracks)' 'Local search behavior changed'
Need $screen 'LibraryBrowsePolicy.songs(' 'Library order policy missing'
Need $screen 'onSelectPlaylist(collection.name)' 'Playlist open action missing'
Need $builder 'check-phase6v7b-integration.ps1' 'Windows preflight not registered'
Need $builder ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Full Gradle tasks removed'
Need $ci 'python3 tools/verify_v2_phase6v7b.py' 'GitHub CI check missing'
Tests 'app/src/test/java/com/nightwave/player/v2/ui/NightWaveCollectionChromeTest.kt' 8
Tests 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveSearchLibraryChromeInstrumentedTest.kt' 3
Tests 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveSwipeTabsInstrumentedTest.kt' 2
if ($issues.Count -gt 0) {
    Write-Host ("FAIL: 6V7B UI preflight reports {0} issue(s)" -f $issues.Count) -ForegroundColor Red
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V7B Search/Library chrome source and regression declarations present.' -ForegroundColor Green
Write-Host 'NOT VERIFIED: Kotlin/Compose compilation, device tests, APK signature.' -ForegroundColor Yellow
exit 0
