# NightWave 6P: local source-only integration preflight.
# Windows PowerShell 5.1, no SDK/Gradle/network/phone or private files.
[CmdletBinding()]
param([switch]$SelfTest)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$script:issues = New-Object System.Collections.Generic.List[string]

function Require-Token([string]$Content, [string]$Token, [string]$Label) {
    if (-not $Content.Contains($Token)) { $script:issues.Add("Missing $Label") }
}
if ($SelfTest) {
    Require-Token 'hello' 'hello' 'positive synthetic check'
    if ($script:issues.Count -ne 0) { throw 'Positive 6P self-test failed.' }
    Require-Token 'hello' 'unavailable' 'negative synthetic check'
    if ($script:issues.Count -ne 1) { throw 'Negative 6P self-test failed.' }
    Write-Host 'PASS: 6P synthetic source-check tests (no Android compilation).'
    exit 0
}
function Source([string]$Relative) {
    $path = Join-Path $root $Relative.Replace('/', [IO.Path]::DirectorySeparatorChar)
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $script:issues.Add("Missing source $Relative")
        return ''
    }
    return [IO.File]::ReadAllText($path)
}

$a = @{
    gradle = Source 'app/build.gradle.kts'
    ci = Source '.github/workflows/android.yml'
    svc = Source 'app/src/main/java/com/nightwave/player/playback/PlaybackService.kt'
    queue = Source 'app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt'
    bridge = Source 'app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt'
    ui = Source 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt'
    level = Source 'app/src/main/java/com/nightwave/player/playback/ListeningLevelPolicy.kt'
    store = Source 'app/src/main/java/com/nightwave/player/playback/ListeningLevelStore.kt'
    search = Source 'app/src/main/java/com/nightwave/player/net/SearchRelevancePolicy.kt'
    saved = Source 'app/src/main/java/com/nightwave/player/net/SavedTrackSearch.kt'
    gateway = Source 'app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt'
    creators = Source 'app/src/main/java/com/nightwave/player/net/SavedCreatorBrowser.kt'
    browsing = Source 'app/src/main/java/com/nightwave/player/v2/library/LibraryBrowsePolicy.kt'
    quick = Source 'app/src/main/java/com/nightwave/player/v2/library/LibraryQuickPlayPolicy.kt'
    library = Source 'app/src/main/java/com/nightwave/player/library/MusicLibrary.kt'
    windows = Source 'tools/build-windows.ps1'
}
Require-Token $a.gradle 'applicationId = "com.nightwave.player"' 'app identity'
Require-Token $a.gradle 'versionCode = 35' 'app version code'
Require-Token $a.svc 'playWhenReady = false' 'no-start-autoplay rule'
Require-Token $a.svc 'PlaybackTransitionPolicy' 'transition fence'
Require-Token $a.svc 'EXTRA_QUEUE_DISPLAY_ORDER' 'Library queue-order command'
Require-Token $a.svc 'queue.useExplicitDisplayOrder()' 'service queue-order selection'
Require-Token $a.queue 'fun useExplicitDisplayOrder()' 'queue order transition'
Require-Token $a.queue 'shuffledSeen.clear()' 'shuffle-cycle reset'
Require-Token $a.bridge 'fun playLibraryCollection(collection: List<VideoPreview>)' 'scoped Library playback'
Require-Token $a.bridge 'explicitDisplayOrder = false' 'ordinary song playback semantics'
Require-Token $a.bridge 'explicitDisplayOrder = true' 'Library playback semantics'
Require-Token $a.bridge '.putExtra(PlaybackService.EXTRA_QUEUE_DISPLAY_ORDER, explicitDisplayOrder)' 'service handoff'
Require-Token $a.ui 'onLibraryQuickPlay = { collection -> playback.playLibraryCollection(collection) }' 'quick-play navigation'
Require-Token $a.ui 'onQuickPlay = onLibraryQuickPlay' 'Library quick-play callback'
Require-Token $a.ui 'plan.first?.let { onQuickPlay(plan.songs) }' 'safe queue nonempty check'
Require-Token $a.ui 'enabled = quickPlayableTracks.isNotEmpty()' 'no empty quick-play button'
Require-Token $a.level 'presetsPercent = listOf(100, 85, 70)' 'attenuation presets'
Require-Token $a.store 'ListeningLevelPolicy.accepts(percent)' 'bounded persistent level'
Require-Token $a.search 'Normalizer.Form.NFKD' 'accent-aware matching'
Require-Token $a.saved 'SearchRelevancePolicy.savedMatches(prepared, track)' 'local saved search'
Require-Token $a.gateway 'SearchRelevancePolicy.rankPublic(query, videos)' 'public relevance'
Require-Token $a.creators 'VideoLink.normalize(video.url) != video.url' 'canonical creator references'
Require-Token $a.creators 'MAX_QUEUE_TRACKS = 100' 'creator queue capacity'
Require-Token $a.browsing 'fun playlists(' 'read-only playlist view'
Require-Token $a.browsing 'fun songs(' 'read-only song view'
Require-Token $a.quick 'VideoLink.normalize(it.url) == it.url' 'canonical quick playback'
Require-Token $a.quick 'ordered.take(MAX_QUEUE)' 'bounded quick queue'
Require-Token $a.library 'nightwave_library_v1' 'existing library file'
Require-Token $a.windows ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Gradle tests/compile'
Require-Token $a.windows 'verify-apk-signature.ps1' 'signer gate'
Require-Token $a.ci 'python3 tools/verify_v2_phase6p.py' '6P CI preflight'
Require-Token $a.ci 'feature/nightwave-v2-phase6o4-library-quick-actions' 'new phase CI target'
if ([regex]::Matches($a.svc, 'ExoPlayer\.Builder\(').Count -ne 1) {
    $script:issues.Add('PlaybackService must retain exactly one ExoPlayer builder')
}
if ($a.quick.Contains('SharedPreferences') -or $a.browsing.Contains('SharedPreferences')) {
    $script:issues.Add('View-only library policies cannot write preferences')
}
if ($a.search.Contains('HttpURLConnection') -or $a.creators.Contains('HttpURLConnection')) {
    $script:issues.Add('Local search and creator policies must not contact external services')
}
if ($script:issues.Count -gt 0) {
    Write-Host ("FAIL: 6P integration source preflight: {0} issue(s)" -f $script:issues.Count) -ForegroundColor Red
    $script:issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: NightWave 6P cross-phase source integration preflight.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Android compilation, JVM test execution, APK signing and device QA.'
exit 0
