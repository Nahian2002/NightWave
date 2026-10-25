# NW59: Windows PowerShell 5.1-compatible, no Python/SDK/network required.
# Every inspection is read-only and runs before Gradle. Real Android and
# Google authorization testing is still necessary after this source gate.
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'

function Assert-NW59Has([string]$Body, [string]$Needle, [string]$Context) {
    if ($Body.IndexOf($Needle, [System.StringComparison]::Ordinal) -lt 0) {
        throw ("NW59 missing " + $Context + ": " + $Needle)
    }
}
function Assert-NW59Missing([string]$Body, [string]$Needle, [string]$Context) {
    if ($Body.IndexOf($Needle, [System.StringComparison]::Ordinal) -ge 0) {
        throw ("NW59 regression " + $Context + ": " + $Needle)
    }
}
function Assert-NW59Order([string]$Body, [string[]]$Labels) {
    $cursor = -1
    foreach ($label in $Labels) {
        $next = $Body.IndexOf($label, [System.StringComparison]::Ordinal)
        if ($next -lt 0 -or $next -le $cursor) {
            throw ("NW59 screen order changed or action missing: " + $label)
        }
        $cursor = $next
    }
}
function Assert-NW59SafeGoogleChooser([string]$Body) {
    Assert-NW59Has $Body 'else openExternalFlow(onConnect)' 'Google connect launches outside hub'
    Assert-NW59Has $Body 'openExternalFlow(onConnect)' 'Google retry and account change'
    Assert-NW59Has $Body 'CloudConnectionHelpPolicy.guidance(it.stage, it.code)' 'stage-aware failure help'
    Assert-NW59Missing $Body 'else onConnect()' 'Google chooser stacked behind account modal'
}
function Assert-NW59OpaquePlayer([string]$Body) {
    Assert-NW59Has $Body 'MutableTransitionState(false)' 'player transition state'
    Assert-NW59Has $Body 'LaunchedEffect(expanded) { overlayVisibility.targetState = expanded }' 'safe composition lifetime'
    Assert-NW59Has $Body 'NightWavePlayerOverlayPolicy.retainBackdrop(' 'opaque transition shield'
    Assert-NW59Has $Body 'testTag("nw58-opaque-player-backdrop")' 'opaque shield tagged'
    Assert-NW59Has $Body 'visibleState = overlayVisibility' 'transition lifetime'
    Assert-NW59Missing $Body 'AnimatedVisibility(visible = expanded, enter = enter, exit = exit)' 'non-opaque original player fade'
}
function Assert-NW59SearchCopy([string]$Body) {
    Assert-NW59Has $Body 'item(key = "nw54-public-results-heading")' 'consumer search result heading'
    Assert-NW59Has $Body 'SearchConsumerCopyPolicy.resultCount(state.videos.size)' 'result count policy'
    Assert-NW59Has $Body 'onPlay(selected, listOf(selected))' 'bounded single selected song'
    $start = $Body.IndexOf('item(key = "nw54-public-results-heading")')
    $finish = $Body.IndexOf('if (state.error.isNotBlank()) {', $start)
    if ($start -lt 0 -or $finish -lt $start) { throw 'NW59 cannot isolate Search header' }
    $header = $Body.Substring($start, $finish - $start)
    Assert-NW59Missing $header 'state.provider' 'Search header exposing raw provider details'
}
function Assert-NW59Home([string]$Body) {
    $start = $Body.IndexOf('private fun StudioHome(')
    $finish = $Body.IndexOf('@Composable' + [char]10 + 'private fun MoodTile(', $start)
    if ($start -lt 0 -or $finish -lt $start) {
        throw 'NW59 cannot isolate Compose Home from source'
    }
    $home = $Body.Substring($start, $finish - $start)
    Assert-NW59Order $home @(
        'nightwave-premium-home-header',
        'nw47-home-quick-access',
        'recent-heading',
        'nw47-recent-shelf',
        'nw47-home-library-stats'
    )
    Assert-NW59Missing $home 'ListeningSpotlight(' 'repeated last-played Home hero'
    Assert-NW59Has $home 'onResumeSpotlight(it, recent)' 'Home Continue command'
    Assert-NW59Has $home 'onRemoveRecent(song)' 'Recently Played removal'
    Assert-NW59Has $home '"Discover music", openSearch' 'empty Home Search navigation'
}
if ($SelfTest) {
    Assert-NW59Has 'before after' 'after' 'positive fixture'
    $rejected = $false
    try { Assert-NW59Has 'before after' 'missing' 'broken fixture' } catch { $rejected = $true }
    if (-not $rejected) { throw 'NW59 missing-token test did not fail' }
    $rejected = $false
    try { Assert-NW59Missing 'contains outdated call' 'outdated call' 'broken fixture' } catch { $rejected = $true }
    if (-not $rejected) { throw 'NW59 forbidden-token test did not fail' }
    Assert-NW59Order 'first then second third' @('first', 'second', 'third')
    $rejected = $false
    try { Assert-NW59Order 'first then second third' @('second', 'first') } catch { $rejected = $true }
    if (-not $rejected) { throw 'NW59 bad screen order test did not fail' }
    Write-Host 'NW59 invariant self-test PASS. Android compilation not attempted.'
    exit 0
}
# Resolve the project only after parameter binding (never in param defaults).
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Join-Path -Path $PSScriptRoot -ChildPath '..'
}
$root = (Resolve-Path -LiteralPath $ProjectRoot -ErrorAction Stop).Path
$relative = @(
    'app/src/main/java/com/nightwave/player/v2/ui/NightWaveAccountHub.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt',
    'app/src/main/java/com/nightwave/player/v2/cloud/CloudConnectionHelpPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/SearchConsumerCopyPolicy.kt',
    'app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt',
    'app/src/main/java/com/nightwave/player/offline/CatalogDownloadsActivity.kt',
    'app/src/main/java/com/nightwave/player/offline/OfflineLibraryViewPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/model/NightWaveDesign.kt',
    'app/src/main/java/com/nightwave/player/v2/model/NightWaveContrastPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/NightWavePlayerOverlayPolicy.kt',
    'app/src/test/java/com/nightwave/player/v2/cloud/CloudConnectionHelpPolicyTest.kt',
    'app/src/test/java/com/nightwave/player/v2/ui/SearchConsumerCopyPolicyTest.kt',
    'app/src/test/java/com/nightwave/player/offline/OfflineLibraryViewPolicyTest.kt',
    'app/src/test/java/com/nightwave/player/v2/model/NightWaveContrastPolicyTest.kt',
    'app/src/test/java/com/nightwave/player/v2/ui/NightWavePlayerOverlayPolicyTest.kt',
    'tools/verify_nw53_oauth_ui.py',
    'tools/verify_nw54_search_copy.py',
    'tools/verify_nw55_offline_chrome.py',
    'tools/verify_nw56_contrast.py',
    'tools/verify_nw57_home.py',
    'tools/verify_nw58_overlay.py',
    'docs/NW53_GOOGLE_RECOVERY_UX.md',
    'docs/NW54_SEARCH_CLARITY.md',
    'docs/NW55_OFFLINE_DOWNLOAD_VISUAL_POLISH.md',
    'docs/NW56_THEME_CONTRAST.md',
    'docs/NW57_HOME_FIRST_FOLD.md',
    'docs/NW58_PLAYER_OVERLAY.md'
)
$files = @{}
foreach ($name in $relative) {
    $file = Join-Path -Path $root -ChildPath $name
    if (-not [IO.File]::Exists($file)) { throw ("NW59 incomplete source ZIP: " + $name) }
    if ($name.EndsWith('.kt')) {
        $body = [IO.File]::ReadAllText($file)
        if ($body -match '(?m)^(<<<<<<< |=======|>>>>>>> )') {
            throw ("NW59 unresolved merge markers: " + $name)
        }
    }
    $files[$name] = $file
}
$main = 'app/src/main/java/com/nightwave/player/'
$ui = $main + 'v2/ui/'
$offline = $main + 'offline/'
$model = $main + 'v2/model/'
$cloud = $main + 'v2/cloud/'
$activity = [IO.File]::ReadAllText($files[$ui + 'NightWaveStudioActivity.kt'])
$hub = [IO.File]::ReadAllText($files[$ui + 'NightWaveAccountHub.kt'])
$player = [IO.File]::ReadAllText($files[$ui + 'StudioPlayerScreens.kt'])
$offlineUi = [IO.File]::ReadAllText($files[$offline + 'OfflineLibraryActivity.kt'])
$downloads = [IO.File]::ReadAllText($files[$offline + 'CatalogDownloadsActivity.kt'])
$filter = [IO.File]::ReadAllText($files[$offline + 'OfflineLibraryViewPolicy.kt'])
$palette = [IO.File]::ReadAllText($files[$model + 'NightWaveDesign.kt'])
Assert-NW59SafeGoogleChooser $hub
Assert-NW59SearchCopy $activity
Assert-NW59Home $activity
Assert-NW59OpaquePlayer $player
Assert-NW59Has $player 'color = colors.onSurface)' 'Sound and playback contrast'
Assert-NW59Has $offlineUi 'OfflineLibraryViewPolicy.accessibilityLabel(filter)' 'TalkBack issues filter'
Assert-NW59Has $offlineUi 'library.healthReport()' 'offline file health validation'
Assert-NW59Has $downloads 'OfflineAudioLibrary(this).availableIds(allSavedIds)' 'downloaded file must exist'
Assert-NW59Has $downloads 'Resume restarts from zero' 'honest no-byte-resume label'
Assert-NW59Missing $downloads 'LayoutParams(-1, dp(44))' 'undersized transfer actions'
Assert-NW59Has $filter 'Filter.ATTENTION -> "Issues"' 'compact offline filter'
Assert-NW59Has $filter 'Filter.ATTENTION -> "Needs attention"' 'accessible offline filter'
Assert-NW59Has $palette '0xFF815154' 'readable Pearl accent'
Assert-NW59Has $palette 'else 0xFFF9F7F2' 'readable Pearl action text'
Assert-NW59Has $activity 'openSearch = { navigateToTab(StudioTab.SEARCH) }' 'Home Search action preserved'
Assert-NW59Has $player 'seekToForTrack' 'safe lyric track-bound seek'
Write-Host 'NW59 cumulative NW53-NW58 source guard PASS. Gradle/JVM/device QA still mandatory.'
exit 0
