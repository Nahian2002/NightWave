# NW52 Windows-local source consistency preflight. No Python, network,
# credentials, Android SDK, application install or mutation required.
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'

function Assert-NW52Marker([string]$Data, [string]$Marker, [string]$Label) {
    if ($Data.IndexOf($Marker, [StringComparison]::Ordinal) -lt 0) {
        throw "NW52 source marker missing: $Label"
    }
}

if ($SelfTest) {
    Assert-NW52Marker 'alpha-beta-stable' 'beta' 'positive synthetic fixture'
    $rejected = $false
    try {
        Assert-NW52Marker 'alpha-beta-stable' 'not-present' 'negative fixture'
    } catch { $rejected = $true }
    if (-not $rejected) { throw 'NW52 checker accepted broken synthetic fixture.' }
    Write-Output 'NW52 PowerShell source preflight self-test PASS (NOT Android compilation).'
    exit 0
}

# Resolve the default only *after* PowerShell binds parameters. During
# -File invocation, $PSScriptRoot is not reliable inside param defaults,
# especially when -SelfTest is called with no -ProjectRoot.
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Join-Path -Path $PSScriptRoot -ChildPath '..'
}
$root = (Resolve-Path -LiteralPath $ProjectRoot -ErrorAction Stop).Path
if (-not (Test-Path -LiteralPath $root -PathType Container)) {
    throw "NW52 project root is not a directory: $root"
}
$required = @(
    'app/build.gradle.kts',
    'app/src/main/AndroidManifest.xml',
    'app/src/main/java/com/nightwave/player/playback/SoundStudioTuningPolicy.kt',
    'app/src/main/java/com/nightwave/player/offline/DownloadCenterViewPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/NightWaveMotionPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/LibraryUiRefreshPolicy.kt',
    'docs/NW52_FINAL_INTEGRATION.md',
    'RELEASE_READINESS.md',
    'tools/verify_nw52_ui_prebuild.py'
)
43..52 | ForEach-Object { $required += ('tools/verify_nw' + $_ + '*') }
foreach ($item in $required) {
    $match = @(Get-ChildItem -Path (Join-Path $root $item) -File -ErrorAction SilentlyContinue)
    if ($match.Count -eq 0) {
        throw "NW52 incomplete integrated source ZIP: missing $item"
    }
}
$gradle = [IO.File]::ReadAllText((Join-Path $root 'app/build.gradle.kts'))
$manifest = [IO.File]::ReadAllText((Join-Path $root 'app/src/main/AndroidManifest.xml'))
$activity = [IO.File]::ReadAllText((Join-Path $root 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt'))
$offlineActivity = [IO.File]::ReadAllText((Join-Path $root 'app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt'))
$recentCard = [IO.File]::ReadAllText((Join-Path $root 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveEverydaySurfaces.kt'))
$player = [IO.File]::ReadAllText((Join-Path $root 'app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt'))
$readme = [IO.File]::ReadAllText((Join-Path $root 'docs/NW52_FINAL_INTEGRATION.md'))
Assert-NW52Marker $gradle 'applicationId = "com.nightwave.player"' 'unchanged package ID'
Assert-NW52Marker $gradle 'versionCode = 35' 'private candidate versionCode'
Assert-NW52Marker $gradle 'targetSdk = 35' 'private candidate target SDK'
Assert-NW52Marker $manifest 'android:allowBackup="false"' 'private-data backup policy'
Assert-NW52Marker $manifest 'android:usesCleartextTraffic="false"' 'HTTPS-only policy'
Assert-NW52Marker $player 'seekToForTrack' 'track-bound seek protection'
Assert-NW52Marker $player 'enabled = state.hasTrack,' 'disable inert offline action with no track'
Assert-NW52Marker $activity 'openSearch = { navigateToTab(StudioTab.SEARCH) }' 'Home search destination'
Assert-NW52Marker $activity '"Discover music", openSearch' 'Home empty-state real Search navigation'
Assert-NW52Marker $activity 'Icons.Outlined.Search, "nw47-home-search", openSearch)' 'Home Search shortcut'
Assert-NW52Marker $activity '.height(48.dp)) {' 'restorable mini-player tap targets'
Assert-NW52Marker $recentCard 'onClick = onRemove' 'recent removal distinct from playback'
Assert-NW52Marker $offlineActivity 'OfflineLibraryViewPolicy.includes(' 'offline file-health filter'
Assert-NW52Marker $offlineActivity 'contentDescription = "Sort offline music' 'sort accessibility'
if ($activity.Contains('Icons.Outlined.Search, "nw47-home-search") { onSearch("music") }')) {
    throw 'NW52 Home Search shortcut regressed to a generic music query.'
}
Assert-NW52Marker $readme 'NO-GO' 'no misleading public-release claim'
Assert-NW52Marker $readme 'feature/nightwave-nw52-final-integration-release-gates' 'NW52 source identification'

foreach ($source in @(
    'app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt',
    'app/src/main/java/com/nightwave/player/playback/PlaybackService.kt'
)) {
    $body = [IO.File]::ReadAllText((Join-Path $root $source))
    if ($body -match '(?m)^(<<<<<<< |>>>>>>> )') {
        throw "NW52 unresolved merge markers in $source"
    }
}
Write-Output 'NW52 source integration gate PASS. Real Gradle/JUnit/Android tests still mandatory.'
exit 0
