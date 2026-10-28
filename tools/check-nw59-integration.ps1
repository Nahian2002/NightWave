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
    $start = $Body.IndexOf('internal fun StudioHome(')
    $finish = $Body.IndexOf('private fun MoodTile(', [Math]::Max(0, $start))
    if ($start -lt 0 -or $finish -lt $start) {
        throw 'NW59 cannot isolate Compose Home from source'
    }
    # $HOME is PowerShell's read-only automatic environment variable,
    # and PowerShell variable names are case-insensitive. Never assign $home.
    $homeSource = $Body.Substring($start, $finish - $start)
    Assert-NW59Order $homeSource @(
        'nightwave-premium-home-header',
        'nw47-home-quick-access',
        'recent-heading',
        'nw47-recent-shelf',
        'discovery-shelf'
    )
    Assert-NW59Missing $homeSource 'ListeningSpotlight(' 'repeated last-played Home hero'
    Assert-NW59Has $homeSource 'onResumeSpotlight(last, recentShelf)' 'Home Continue command'
    Assert-NW59Has $homeSource 'onRemoveRecent(song)' 'Recently Played removal'
    Assert-NW59Has $homeSource 'NightWaveHomeLayoutPolicy.EmptyRecentDestination.SEARCH ->' 'empty Home Search navigation'
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
    # Real exercise of the Home isolation function: this previously failed
    # because assigning $home also tries to overwrite read-only $HOME.
    $homeFixture = 'internal fun StudioHome(' + ' nightwave-premium-home-header' +
        ' nw47-home-quick-access recent-heading nw47-recent-shelf' +
        ' discovery-shelf onResumeSpotlight(last, recentShelf)' +
        ' onRemoveRecent(song) NightWaveHomeLayoutPolicy.EmptyRecentDestination.SEARCH -> private fun MoodTile('
    Assert-NW59Home $homeFixture
    $rejected = $false
    try {
        Assert-NW59Home ($homeFixture.Replace('discovery-shelf', 'missing-discovery'))
    } catch { $rejected = $true }
    if (-not $rejected) { throw 'NW59 Home broken fixture was not rejected' }
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
    'app/src/main/java/com/nightwave/player/v2/cloud/CloudAccountSessionPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupFreshnessPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/StudioUiStateSafetyPolicy.kt',
    'app/src/test/java/com/nightwave/player/v2/ui/StudioUiStateSafetyPolicyTest.kt',
    'tools/NW74UiStateSmoke.kt',
    'docs/NW74_UI_STATE_INTEGRATION_GUARDS.md',
    'tools/NW75IntegrationSmoke.kt',
    'tools/nw76-junit-proof.ps1',
    'tools/build-windows.ps1',
    'docs/NW76_BUILD_CERTIFICATION.md',
    'docs/NW75_FINAL_INTEGRATION.md',
    'app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupState.kt',
    'app/src/main/java/com/nightwave/player/v2/cloud/CloudAccountPresentation.kt',
    'app/src/main/java/com/nightwave/player/v2/cloud/CloudConnectionHelpPolicy.kt',
    'app/src/test/java/com/nightwave/player/v2/cloud/CloudBackupFreshnessPolicyTest.kt',
    'app/src/test/java/com/nightwave/player/v2/cloud/CloudAccountPresentationTest.kt',
    'app/src/test/java/com/nightwave/player/v2/cloud/CloudConnectionHelpPolicyTest.kt',
    'tools/NW73CloudStatusSmoke.kt',
    'docs/NW73_BACKUP_FRESHNESS_OAUTH_GUIDANCE.md',
    'app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupConsentPolicy.kt',
    'app/src/test/java/com/nightwave/player/v2/cloud/CloudAccountSessionPolicyTest.kt',
    'tools/NW72CloudSessionSmoke.kt',
    'docs/NW72_GOOGLE_SESSION_RECOVERY.md',
    'app/src/main/java/com/nightwave/player/v2/ui/SearchConsumerCopyPolicy.kt',
    'app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt',
    'app/src/main/java/com/nightwave/player/offline/CatalogDownloadsActivity.kt',
    'app/src/main/java/com/nightwave/player/offline/OfflineLibraryViewPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/model/NightWaveDesign.kt',
    'app/src/main/java/com/nightwave/player/v2/model/NightWaveContrastPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/ui/NightWavePlayerOverlayPolicy.kt',
    'app/src/main/java/com/nightwave/player/playback/PlaybackService.kt',
    'app/src/main/java/com/nightwave/player/playback/OfflineDecodeRecoveryPolicy.kt',
    'app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt',
    'app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt',
    'app/src/main/java/com/nightwave/player/net/SearchScriptNormalization.kt',
    'app/src/main/java/com/nightwave/player/v2/library/SmartDiscoveryPoolPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/library/SmartLibraryPolicy.kt',
    'app/src/main/java/com/nightwave/player/v2/library/LibraryBrowsePolicy.kt',
    'app/src/test/java/com/nightwave/player/v2/library/SmartDiscoveryPoolPolicyTest.kt',
    'app/src/test/java/com/nightwave/player/v2/library/SmartLibraryPolicyTest.kt',
    'app/src/test/java/com/nightwave/player/v2/library/LibraryBrowsePolicyTest.kt',
    'tools/NW71SmartDiscoverySmoke.kt',
    'docs/NW71_SMART_LIBRARY_DISCOVERY.md',
    'app/src/main/java/com/nightwave/player/net/SearchRelevancePolicy.kt',
    'app/src/main/java/com/nightwave/player/net/SavedTrackSearch.kt',
    'app/src/main/java/com/nightwave/player/net/MusicIdentity.kt',
    'app/src/main/java/com/nightwave/player/net/LocalSearchSuggestions.kt',
    'app/src/test/java/com/nightwave/player/net/SearchRelevancePolicyTest.kt',
    'app/src/test/java/com/nightwave/player/net/SavedTrackSearchTest.kt',
    'app/src/test/java/com/nightwave/player/net/LocalSearchSuggestionsTest.kt',
    'app/src/test/java/com/nightwave/player/net/SearchResultPresentationPolicyTest.kt',
    'tools/NW70SearchSmoke.kt',
    'docs/NW70_LOCAL_SEARCH_UNICODE_PERFORMANCE.md',
    'app/src/test/java/com/nightwave/player/offline/OfflineShuffleForwardNavigationTest.kt',
    'app/src/test/java/com/nightwave/player/offline/OfflinePlaybackQueueTest.kt',
    'app/src/test/java/com/nightwave/player/playback/PublicQueuePastPriorityTest.kt',
    'tools/NW69QueueSmoke.kt',
    'docs/NW69_QUEUE_TRANSITION_CONSISTENCY.md',
    'app/src/main/java/com/nightwave/player/playback/AudioEngineOwnershipPolicy.kt',
    'app/src/main/java/com/nightwave/player/playback/EqualizerCapabilities.kt',
    'app/src/test/java/com/nightwave/player/playback/AudioEngineOwnershipPolicyTest.kt',
    'docs/NW68_AUDIO_ENGINE_OWNERSHIP.md',
    'app/src/test/java/com/nightwave/player/playback/ShuffleForwardNavigationTest.kt',
    'app/src/test/java/com/nightwave/player/playback/OfflineDecodeRecoveryPolicyTest.kt',
    'docs/NW67_PLAYBACK_TRANSITIONS.md',
    'app/src/test/java/com/nightwave/player/playback/OfflineDecodeRecoveryPolicyTest.kt',
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
    'tools/verify_nw60_offline_recovery.py',
    'tools/verify_nw61_oauth_report.py',
    'tools/verify_nw62_download_health.py',
    'app/src/main/java/com/nightwave/player/offline/DownloadFileVerificationPolicy.kt',
    'app/src/main/java/com/nightwave/player/offline/DownloadHistoryPagePolicy.kt',
    'app/src/main/java/com/nightwave/player/offline/DownloadRefreshPolicy.kt',
    'app/src/main/java/com/nightwave/player/offline/DownloadRecoveryPolicy.kt',
    'app/src/main/java/com/nightwave/player/offline/OfflineTransferCompletionPolicy.kt',
    'app/src/test/java/com/nightwave/player/offline/OfflineTransferCompletionPolicyTest.kt',
    'app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloader.kt',
    'app/src/main/java/com/nightwave/player/offline/OfflineSaveStoragePolicy.kt',
    'app/src/main/java/com/nightwave/player/offline/CatalogDownloadWorker.kt',
    'app/src/main/java/com/nightwave/player/offline/DirectAudioDownloadWorker.kt',
    'app/src/main/java/com/nightwave/player/offline/SearchTrackSaveWorker.kt',
    'docs/NW66_OFFLINE_INTEGRITY_AND_RECOVERY.md',
    'app/src/test/java/com/nightwave/player/offline/DownloadRecoveryPolicyTest.kt',
    'docs/NW65_DOWNLOAD_RECOVERY_SAFETY.md',
    'app/src/test/java/com/nightwave/player/offline/DownloadRefreshPolicyTest.kt',
    'docs/NW64_DOWNLOAD_RESPONSIVENESS.md',
    'app/src/test/java/com/nightwave/player/offline/DownloadHistoryPagePolicyTest.kt',
    'docs/NW63_DOWNLOAD_HISTORY_NAVIGATION.md',
    'app/src/test/java/com/nightwave/player/offline/DownloadFileVerificationPolicyTest.kt',
    'tools/check-google-oauth.ps1',
    'check-google-oauth.cmd',
    'docs/NW53_GOOGLE_RECOVERY_UX.md',
    'docs/NW54_SEARCH_CLARITY.md',
    'docs/NW55_OFFLINE_DOWNLOAD_VISUAL_POLISH.md',
    'docs/NW56_THEME_CONTRAST.md',
    'docs/NW57_HOME_FIRST_FOLD.md',
    'docs/NW58_PLAYER_OVERLAY.md',
    'docs/NW61_GOOGLE_OAUTH_SETUP.md'
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
Assert-NW59Has $downloads 'OfflineAudioLibrary(applicationContext).availabilitySnapshot(ids)' 'NW62 background verification of actual saved files'
Assert-NW59Has $downloads 'Resume restarts from zero' 'honest no-byte-resume label'
Assert-NW59Missing $downloads 'LayoutParams(-1, dp(44))' 'undersized transfer actions'
Assert-NW59Has $filter 'Filter.ATTENTION -> "Issues"' 'compact offline filter'
Assert-NW59Has $filter 'Filter.ATTENTION -> "Needs attention"' 'accessible offline filter'
Assert-NW59Has $palette '0xFF1C47AD' 'approved expressive Pearl accent; actual contrast also required by JVM tests'
Assert-NW59Has $palette 'else 0xFFF9F7F2' 'readable Pearl action text'
Assert-NW59Has $activity 'openSearch = { navigateToTab(StudioTab.SEARCH) }' 'Home Search action preserved'
Assert-NW59Has $player 'seekToForTrack' 'safe lyric track-bound seek'
# NW60 recovers once per local ID instead of silently ignoring decoder errors.
$playback = [IO.File]::ReadAllText($files[$main + 'playback/PlaybackService.kt'])
$decode = [IO.File]::ReadAllText($files[$main + 'playback/OfflineDecodeRecoveryPolicy.kt'])
Assert-NW59Has $playback 'OfflineDecodeRecoveryPolicy.belongsToSelection(' 'decoder error belongs to active selection'
Assert-NW59Has $playback 'offlineDecodeFailedIds.add(selectedId!!)' 'bounded failed ID receipt'
Assert-NW59Has $playback 'atErrorGeneration = generation' 'capture deferred recovery generation before post'
Assert-NW59Has $playback 'OfflineDecodeRecoveryPolicy.nextLinearIndex(' 'private linear fallback'
Assert-NW59Has $playback 'OfflineDecodeRecoveryPolicy.safeShuffledIndex(' 'Media3 shuffle navigation'
Assert-NW59Has $playback 'if (!automatic) offlineDecodeFailedIds.clear()' 'manual retry'
Assert-NW59Has $playback 'if (player.playbackState == Player.STATE_IDLE) player.prepare()' 'manual jump after failure'
Assert-NW59Has $decode 'ids[at] !in alreadyFailed' 'no repeated failed offline ID'
Assert-NW59Has $decode 'ids[it] !in alreadyFailed' 'shuffle does not retry failed ID'
Assert-NW59Missing $playback 'if (offlinePlaying || queue.current?.url != video) return' 'silent offline decode error'
# NW61 local OAuth fingerprint may only describe a verified, current APK.
$oauthSource = [IO.File]::ReadAllText($files['tools/check-google-oauth.ps1'])
Assert-NW59Has $oauthSource 'Test-VerifiedApkStatus $status $apkSha256' 'verified APK hash required'
Assert-NW59Has $oauthSource 'Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256' 'APK hash check'
Assert-NW59Has $oauthSource 'Get-ChildItem -LiteralPath $parent -Directory' 'flexible SDK Build Tools search'
Assert-NW59Has $oauthSource 'INCONCLUSIVE - no current verified APK' 'failed-build report invalidation'
Assert-NW59Missing $oauthSource 'build-tools\35.0.0' 'hardcoded diagnostic SDK version'
# NW62: Downloads file availability is checked off the UI thread,
# and pending checks can NEVER mark saved files Ready or broken.
$downloadsPolicy = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/DownloadFileVerificationPolicy.kt'])
Assert-NW59Has $downloads 'private val fileVerifier = ThreadPoolExecutor(' 'bounded file verification'
Assert-NW59Has $downloads 'ArrayBlockingQueue<Runnable>(1)' 'bounded pending verification'
Assert-NW59Has $downloads 'OfflineAudioLibrary(applicationContext).availabilitySnapshot(ids)' 'background file inspection'
Assert-NW59Has $downloads 'fileCheckFailed = snapshot?.indexReadable != true' 'unknown index cannot imply missing files'
Assert-NW59Has $downloads 'DownloadFileVerificationPolicy.canPublish(' 'discard stale file results'
Assert-NW59Has $downloads 'fileVerifier.shutdownNow()' 'clean up verifier on destroy'
Assert-NW59Has $downloads 'DownloadFileVerificationPolicy.ready(' 'do not trust WorkManager success alone'
Assert-NW59Has $downloads 'DownloadFileVerificationPolicy.needsAttention(' 'honest Issues filter'
Assert-NW59Has $downloadsPolicy '!checking && !verificationFailed' 'pending files are not Ready'
Assert-NW59Missing $downloads 'val availableIds = OfflineAudioLibrary(this).availableIds(allSavedIds)' 'old main-thread storage check'
# NW63: browsing all history must not be truncated to the earliest 60 results.
$pages = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/DownloadHistoryPagePolicy.kt'])
$pagesTest = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/offline/DownloadHistoryPagePolicyTest.kt'])
Assert-NW59Has $downloads 'val historyJobs = displayInfos.sortedWith(' 'retry dedup before paging'
Assert-NW59Has $downloads 'val filteredJobs = historyJobs.filter' 'filter before paging'
Assert-NW59Has $downloads 'DownloadHistoryPagePolicy.normalizedPage(' 'clamp old page after deletions'
Assert-NW59Has $downloads 'val pageStart = DownloadHistoryPagePolicy.offset(' 'all filtered history remains reachable'
Assert-NW59Has $downloads 'historyPage, filteredJobs.size)' 'pagination uses filtered count'
Assert-NW59Has $downloads 'filteredJobs.subList(pageStart, pageEnd)' 'memory-light page view'
Assert-NW59Has $downloads 'val pageEnd = pageStart + minOf(DownloadHistoryPagePolicy.PAGE_SIZE,' 'bounded overflow-safe page view'
Assert-NW59Has $downloads 'DownloadHistoryPagePolicy.summary(' 'honest page caption'
Assert-NW59Has $downloads 'historyPage += 1' 'Older navigation'
Assert-NW59Has $downloads 'historyPage = (historyPage - 1).coerceAtLeast(0)' 'Newer navigation'
Assert-NW59Has $downloads 'outState.putInt("nw63-download-page", historyPage)' 'rotation restores history page'
Assert-NW59Has $pages 'const val PAGE_SIZE = 60' 'bounded per-page inflation'
Assert-NW59Has $pages 'requestedPage.coerceIn(0,' 'index clamp'
Assert-NW59Has $pagesTest 'changingFiltersOrShrinkingHistoryClampsOutdatedPage' 'paging regression tests'
Assert-NW59Missing $downloads '}.take(60)' 'old hard-coded history truncation'
# NW64: progress events are coalesced, never allowed to mutate WorkManager.
$refreshPolicy = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/DownloadRefreshPolicy.kt'])
$refreshTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/offline/DownloadRefreshPolicyTest.kt'])
Assert-NW59Has $downloads 'private val coalescedRefresh = Runnable {' 'single deferred refresh'
Assert-NW59Has $downloads 'DownloadRefreshPolicy.delayMs(' 'bounded progress burst delay'
Assert-NW59Has $downloads 'private fun requestObserverRefresh()' 'latest-state coalescer'
Assert-NW59Has $downloads 'uiHandler.postDelayed(coalescedRefresh, delay)' 'eventual deferred repaint'
Assert-NW59Has $downloads 'uiHandler.removeCallbacks(coalescedRefresh)' 'discard stale pending paints'
Assert-NW59Has $downloads 'progressSamples.keys.retainAll(runningIds)' 'prune obsolete transfer samples'
Assert-NW59Has $downloads 'observedSpeed.keys.retainAll(runningIds)' 'prune obsolete speed samples'
Assert-NW59Has $downloads 'if (displayedFilter != currentFilter) {' 'do not rebuild filter chips for each tick'
Assert-NW59Has $downloads 'val visibleJobs = filteredJobs.subList(' 'no extra copying for page slicing'
Assert-NW59Has $downloads 'scrollTicket == scrollRestoreGeneration' 'no stale scroll correction after new render'
Assert-NW59Has $downloads 'scroll.post {' 'defer scroll restore after layout'
Assert-NW59Has $refreshPolicy 'const val MIN_UI_INTERVAL_MS = 400L' 'bounded paint delay'
Assert-NW59Has $refreshTests 'burstsAreSpacedAndEventuallyFlushed' 'clock and progress tests'
Assert-NW59Missing $downloads '}.take(60)' 'never truncate full history'
# NW65: statuses must never echo untrusted worker exceptions or mislabel a
# completed/failed job as Paused. Keep NW62's source-of-truth file check.
$recoveryPolicy = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/DownloadRecoveryPolicy.kt'])
$recoveryTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/offline/DownloadRecoveryPolicyTest.kt'])
Assert-NW59Has $recoveryPolicy 'internal object DownloadRecoveryPolicy' 'redacted recovery policy exists'
Assert-NW59Has $recoveryPolicy 'fun failedMessage(raw: String?): String' 'typed failure classification'
Assert-NW59Has $recoveryPolicy 'state == "CANCELLED" && pauseMarked' 'pause only after WorkManager cancelled'
Assert-NW59Has $recoveryPolicy 'eligibleSource && !pauseMarked' 'no redundant pause requests'
Assert-NW59Has $downloads 'DownloadRecoveryPolicy.failedMessage(info.outputData.getString(' 'never display raw worker exception'
Assert-NW59Has $downloads 'DownloadRecoveryPolicy.isPaused(' 'honest cancelled marker state'
Assert-NW59Has $downloads 'DownloadRecoveryPolicy.isPausing(' 'active cancellation acknowledged'
Assert-NW59Has $downloads 'DownloadRecoveryPolicy.mayRequestPause(' 'stale dialog guard'
Assert-NW59Has $downloads 'DownloadRecoveryPolicy.retryLabel(' 'accurate restart wording'
Assert-NW59Has $downloads '!verifyingFiles && !fileCheckFailed' 'no false missing-file or retry before health scan'
Assert-NW59Has $downloads 'latest?.state != WorkInfo.State.CANCELLED' 'resume must wait for cancellation'
Assert-NW59Has $downloads 'Existing in-progress work may be reused.' 'honest unique-work keep message'
Assert-NW59Has $recoveryTests 'workFailureCannotDisplaySecretUrlsOrLocalPaths' 'secret redaction tests'
Assert-NW59Has $recoveryTests 'pausedOnlyAfterWorkManagerReportsCancelled' 'cancel lifecycle tests'
Assert-NW59Missing $downloads 'else CatalogDownloadQueue.KEY_ERROR\n                        }).orEmpty()' 'raw failed worker message'
# NW66: all source types preserve space for the verified second private copy;
# every WorkManager success needs an independently indexed and readable file.
$completion = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/OfflineTransferCompletionPolicy.kt'])
$completionTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/offline/OfflineTransferCompletionPolicyTest.kt'])
$downloader = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloader.kt'])
$catalogWorker = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/CatalogDownloadWorker.kt'])
$directWorker = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/DirectAudioDownloadWorker.kt'])
$searchedWorker = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/SearchTrackSaveWorker.kt'])
$storagePolicy = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/OfflineSaveStoragePolicy.kt'])
Assert-NW59Has $completion '!wasStopped && !savedId.isNullOrBlank()' 'stopped or missing result cannot succeed'
Assert-NW59Has $completion 'savedId in verifiedAvailableIds' 'indexed file proof'
Assert-NW59Has $completionTests 'stopAfterImportCannotFalselyMarkWorkSuccessful' 'post-import race regression'
Assert-NW59Has $downloader 'OfflineSaveStoragePolicy.canStart(' 'all-type transfer start storage guard'
Assert-NW59Has $downloader 'OfflineSaveStoragePolicy.canContinue(' 'all-type transfer continuation guard'
Assert-NW59Missing $downloader 'if (searchAudioOnly &&' 'search-only headroom check'
Assert-NW59Has $storagePolicy 'availableBytes >= alreadyStagedBytes + MIN_FREE_BYTES' 'second copy reserve'
foreach ($worker in @($catalogWorker, $directWorker, $searchedWorker)) {
    Assert-NW59Has $worker 'OfflineAudioLibrary(applicationContext)' 'worker verifies indexed library'
    Assert-NW59Has $worker '.availableIds(setOf(saved.id))' 'worker checks saved ID'
    Assert-NW59Has $worker 'OfflineTransferCompletionPolicy.canCommit(isStopped, saved.id, verified)' 'verified completion gate'
    Assert-NW59Has $worker 'OfflineTransferCompletionPolicy.NOT_VERIFIED' 'non-destructive failure guidance'
}
Assert-NW59Has $recoveryPolicy '"saved audio could not be verified" in text' 'post-import warning category'
Assert-NW59Has $recoveryPolicy '"not enough private storage" in text' 'direct/catalog storage messages'
# NW67: a shuffled backtrack must restore its real forward song. A delayed
# offline Media3 decoder recovery must not override a pause or timer expiry.
$publicQueue = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt'])
$forwardTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/playback/ShuffleForwardNavigationTest.kt'])
$offlineRecoveryTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/playback/OfflineDecodeRecoveryPolicyTest.kt'])
Assert-NW59Has $publicQueue 'private val forwardVisits = ArrayDeque<Int>()' 'session-only forward history'
Assert-NW59Has $publicQueue 'forwardVisits.add(index)' 'save shuffle forward step with Kotlin-compatible ArrayDeque'
Assert-NW59Has $publicQueue 'forwardVisits.removeLast()' 'retrace real shuffle order'
Assert-NW59Has $publicQueue 'forwardVisits.clear()' 'invalidate stale trail on edits'
Assert-NW59Has $publicQueue 'queuedNext != null -> {' 'manual Play Next outranks redo'
Assert-NW59Has $forwardTests 'multiplePreviousStepsCanBeRetracedForwardInOrder' 'multi-step shuffle history'
Assert-NW59Has $forwardTests 'explicitPlayNextOutranksForwardAndClearsStaleTrail' 'manual priority over forward'
Assert-NW59Has $playback 'OfflineDecodeRecoveryPolicy.acceptsPostedRecovery(' 'fenced posted decoder retry'
Assert-NW59Has $playback 'wantsPlayback = playerNow.playWhenReady' 'pause suppresses automatic restart'
Assert-NW59Has $playback 'sleepTimerPaused = sleepTimerPaused' 'timer suppresses restart'
Assert-NW59Has $decode 'atErrorGeneration == currentGeneration' 'no old generation recovery'
Assert-NW59Has $decode 'wantsPlayback && !sleepTimerPaused' 'play intent required'
Assert-NW59Has $offlineRecoveryTests 'deferredDecodeRecoveryCannotUndoUserPauseOrStaleSelection' 'pause and stale selection tests'
# NW68: equalizer hardware capability must be owned by the current playback
# audio session; a paused or stale crossfade deck cannot replace MediaSession.
$ownership = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/playback/AudioEngineOwnershipPolicy.kt'])
$ownershipTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/playback/AudioEngineOwnershipPolicyTest.kt'])
$eqCapabilities = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/playback/EqualizerCapabilities.kt'])
Assert-NW59Has $ownership 'internal object EqualizerSessionPolicy' 'EQ ownership policy'
Assert-NW59Has $ownership 'sessionId > 0 && hasControl && bandCount in 1..32' 'capabilities need actual control'
Assert-NW59Has $ownership 'effectSessionId == playbackSessionId && enabled && hasControl' 'active effect session ownership'
Assert-NW59Has $eqCapabilities 'EqualizerSessionPolicy.canPublish(' 'capability store checks actual control'
Assert-NW59Has $eqCapabilities 'clear() // Never leave an old "ready" receipt after control loss.' 'stale EQ state cleared'
Assert-NW59Has $playback 'EqualizerSessionPolicy.canReuse(' 'service cannot reuse stolen EQ'
Assert-NW59Has $playback 'val controlsEffect = probe.hasControl()' 'EQ discovery checks ownership'
Assert-NW59Has $playback 'if (controlsEffect) eqCapabilities.publish(probe, session)' 'never advertise other effect'
Assert-NW59Has $ownership 'internal object CrossfadeHandoffPolicy' 'crossfade ownership policy'
Assert-NW59Has $ownership 'timerPaused && outgoingPlaying' 'crossfade guard checks playing decks'
Assert-NW59Has $ownership 'incomingMediaId == "offline:$nextId"' 'exact next private media identity'
Assert-NW59Has $playback 'CrossfadeHandoffPolicy.mayHandoff(' 'service gates session handoff'
Assert-NW59Has $playback 'outgoingPlaying = old.isPlaying, incomingPlaying = new.isPlaying' 'never handoff paused deck'
Assert-NW59Has $ownershipTests 'eqInstanceMustStillOwnSameLiveSession' 'EQ session ownership tests'
Assert-NW59Has $ownershipTests 'staleMediaOrSelectionNeverReceivesSession' 'crossfade stale ID tests'
# NW69: legacy offline shuffle must retrace real forwards; public Play Next
# cannot jump backwards to an already passed priority. Also prevent old
# Kotlin ArrayDeque.addFirst/addLast compile regressions from prior sources.
$offlineQueue = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt'])
$offlineForwardTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/offline/OfflineShuffleForwardNavigationTest.kt'])
# NW75: a legacy pre-NW69 test incorrectly skipped the exact Shuffle Previous
# redo target. An invalid jump must preserve A -> B redo before fresh C.
$offlineLegacyTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/offline/OfflinePlaybackQueueTest.kt'])
$legacyJumpStart = $offlineLegacyTests.IndexOf('fun invalidShuffleJumpKeepsHistoryAndUnplayedOrder()')
if ($legacyJumpStart -lt 0) { throw 'NW75 offline invalid jump regression missing' }
$legacyJumpEnd = $offlineLegacyTests.IndexOf('@Test fun linearPreviousRepeatAllWrapsAtTheStart()', $legacyJumpStart)
if ($legacyJumpEnd -le $legacyJumpStart) { throw 'NW75 offline invalid jump regression boundary missing' }
$legacyJumpBlock = $offlineLegacyTests.Substring($legacyJumpStart, $legacyJumpEnd - $legacyJumpStart)
Assert-NW59Order $legacyJumpBlock @(
    'assertEquals("a", queue.previous())',
    'assertEquals("b", queue.next())',
    'assertEquals("c", queue.next())',
    'assertNull(queue.next())',
    'assertEquals("b", queue.previous())',
    'assertEquals("c", queue.next()) // retrace the latest Previous'
) 'NW75 offline shuffle redo before unplayed random tracks'

$publicPriorityTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/playback/PublicQueuePastPriorityTest.kt'])
Assert-NW59Has $offlineQueue 'private val forwardVisits = ArrayDeque<Int>()' 'offline shuffle forward history'
Assert-NW59Has $offlineQueue 'forwardVisits.add(index)' 'offline Previous records next target'
Assert-NW59Has $offlineQueue 'forwardVisits.removeLast()' 'offline Next replays real forward target'
Assert-NW59Has $offlineQueue 'unplayed.remove(forward)' 'forward target not replayed randomly'
Assert-NW59Has $offlineQueue 'forwardVisits.clear() // explicit new branch' 'new jump drops old redo'
Assert-NW59Has $offlineForwardTests 'multiplePreviousThenNextRetracesExactlyTheSongsHeard' 'offline shuffle redo test'
Assert-NW59Has $offlineForwardTests 'repeatOneDoesNotConsumeForwardHistoryDuringAutoReplay' 'repeat-one preservation'
Assert-NW59Has $publicQueue 'private fun discardPastPriorities()' 'past Play Next cleanup'
Assert-NW59Has $publicQueue 'manualNext.removeAll { wanted ->' 'remove only no-longer-upcoming priorities'
Assert-NW59Has $publicQueue 'discardPastPriorities()' 'priority cleanup invoked on navigation'
Assert-NW59Has $publicQueue 'manualNext.add(0, entry.url)' 'Kotlin-compatible first-priority insertion'
Assert-NW59Has $publicQueue 'manualNext.add(wanted)' 'Kotlin-compatible priority restore'
Assert-NW59Missing $publicQueue 'manualNext.addFirst(entry.url)' 'unsupported Kotlin ArrayDeque.addFirst'
Assert-NW59Missing $publicQueue 'manualNext.addLast(wanted)' 'unsupported Kotlin ArrayDeque.addLast'
Assert-NW59Missing $publicQueue 'forwardVisits.addLast(index)' 'unsupported Kotlin ArrayDeque.addLast'
Assert-NW59Missing $offlineQueue 'forwardVisits.addLast(index)' 'unsupported offline ArrayDeque.addLast'
Assert-NW59Has $publicPriorityTests 'jumpingPastSeveralPlayNextEntriesDoesNotAutoRewind' 'skip priority regression'
Assert-NW59Has $publicPriorityTests 'validFuturePrioritySurvivesJumpToEarlierSong' 'future priority preservation'
$queueSmoke = [IO.File]::ReadAllText($files['tools/NW69QueueSmoke.kt'])
Assert-NW59Has $queueSmoke 'fun main() {' 'reproducible pure Kotlin smoke'
Assert-NW59Has $queueSmoke 'repeat(5_000)' 'deterministic queue index stress'
# NW70: Bangla vowel signs may never be dropped from saved/public search
# keys. Local text indexing runs off the UI thread with stale-result fencing.
$normalizer = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/net/SearchScriptNormalization.kt'])
$relevance = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/net/SearchRelevancePolicy.kt'])
$savedSearch = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/net/SavedTrackSearch.kt'])
$creators = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/net/MusicIdentity.kt'])
$suggestions = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/net/LocalSearchSuggestions.kt'])
$searchSmoke = [IO.File]::ReadAllText($files['tools/NW70SearchSmoke.kt'])
$relevanceTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/net/SearchRelevancePolicyTest.kt'])
$savedSearchTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/net/SavedTrackSearchTest.kt'])
$suggestionTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/net/LocalSearchSuggestionsTest.kt'])
Assert-NW59Has $normalizer 'private val latinDiacritic' 'Latin-only accent folding'
Assert-NW59Has $normalizer 'Normalizer.Form.NFC' 'recompose Bangla vowel marks'
Assert-NW59Has $relevance 'SearchScriptNormalization.normalize(value)' 'shared search key'
Assert-NW59Missing $relevance 'private val combining = Regex(' 'old all-script mark stripper'
# NW129 replaces per-query preparation and arbitrary caller caps with a
# complete immutable index plus synchronous query/revision acceptance tags.
$localController = [IO.File]::ReadAllText((Join-Path $root 'app/src/main/java/com/nightwave/player/net/LocalMusicSearchController.kt'))
$localCorpus = [IO.File]::ReadAllText((Join-Path $root 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveLocalSearchCorpus.kt'))
Assert-NW59Has $savedSearch 'SearchRelevancePolicy.prepareSaved(track)' 'prepared full metadata'
Assert-NW59Has $savedSearch 'SearchRelevancePolicy.savedScore(it, identities[i], typo)' 'single prepared score'
Assert-NW59Has $savedSearch 'val previous = best[track.url]' 'rank duplicates before choosing best URL metadata'
Assert-NW59Has $savedSearch 'bestFirst.compare(hit, previous) < 0' 'strongest duplicate wins stable ties'
Assert-NW59Has $savedSearch 'if (limit <= 0 || !SearchRelevancePolicy.prepare(query).valid) return emptyList()' 'zero/invalid query remains empty'
Assert-NW59Has $activity 'produceState<LocalMusicSearchResult?>' 'cancel stale local query producer'
Assert-NW59Has $activity 'value = null' 'producer releases old result'
Assert-NW59Has $activity 'it.accepts(query, index)' 'synchronous query and index acceptance'
Assert-NW59Has $activity 'LocalMusicQueryRunner.run(index, query)' 'screen uses production background runner'
Assert-NW59Has $localController 'delay(120)' 'same production typing debounce'
Assert-NW59Has $localController 'withContext(Dispatchers.Default)' 'preparation/ranking off UI thread'
Assert-NW59Has $localController 'requests.collectLatest' 'conflated cancel-and-join builds'
Assert-NW59Has $localCorpus 'withContext(Dispatchers.IO)' 'full snapshots off UI thread'
Assert-NW59Has $activity 'val savedMatches = acceptedLocal?.saved.orEmpty()' 'only accepted rows are visible'
Assert-NW59Has $creators 'scanLimit: Int = 350' 'Home preview retains its default budget'
Assert-NW59Has $creators 'scanLimit.coerceAtMost(15_600)' 'all supported public references fit'
Assert-NW59Has $suggestions 'prepareCreators(MusicIdentity.allCreatorsFrom(saved))' 'no first-5000 creator blind spot'
Assert-NW59Has $searchSmoke 'repeat(1_200)' 'repeatable Unicode normalization stress'
Assert-NW59Has $relevanceTests 'banglaVowelSignsArePreservedAndRemainDistinct' 'Bangla vowel identity test'
Assert-NW59Has $savedSearchTests 'duplicateUrlKeepsTheHighestRelevanceMetadata' 'saved URL duplicate ranking test'
Assert-NW59Has $suggestionTests 'creatorsPastTheOld350ItemCutoffAreDiscoverableLocally' 'older creator regression'
# NW71: read-only playlist discovery and fair, authentic Smart Library cards.
$discoveryPool = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/library/SmartDiscoveryPoolPolicy.kt'])
$smartCollections = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/library/SmartLibraryPolicy.kt'])
$browse = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/library/LibraryBrowsePolicy.kt'])
$discoveryTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/v2/library/SmartDiscoveryPoolPolicyTest.kt'])
$smartTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/v2/library/SmartLibraryPolicyTest.kt'])
$browseTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/v2/library/LibraryBrowsePolicyTest.kt'])
$smoke = [IO.File]::ReadAllText($files['tools/NW71SmartDiscoverySmoke.kt'])
Assert-NW59Has $discoveryPool 'const val FAVORITE_QUOTA = 200' 'favorites reserved budget'
Assert-NW59Has $discoveryPool 'const val RECENT_QUOTA = 150' 'recent reserved budget'
Assert-NW59Has $discoveryPool 'const val PLAYLIST_QUOTA = 250' 'playlist-only reserved budget'
Assert-NW59Has $discoveryPool 'playlists.asSequence().take(60).flatMap' 'bounded lazy playlist collection scan'
Assert-NW59Has $discoveryPool 'id in favoriteKeys && id !in recentKeys' 'rediscover not recently heard favorites'
Assert-NW59Has $smartCollections 'SmartDiscoveryPoolPolicy.gather(' 'collection uses fair pool'
Assert-NW59Has $smartCollections 'VideoLink.normalize(song.url) == song.url' 'saved public reference identity'
Assert-NW59Has $smartCollections 'SmartDiscoveryPoolPolicy.rediscover(' 'actual liked but not recently played'
Assert-NW59Has $smartCollections 'recent.asSequence().filter(eligible)' 'canonical recent shelf'
Assert-NW59Has $browse 'matchContainedSongs: Boolean = false' 'opt-in playlist song lookup'
Assert-NW59Has $browse '.take(MAX_PLAYLIST_SONGS)' 'bounded complete supported playlist song scan'
Assert-NW59Has $browse 'MAX_PLAYLIST_SONGS = 300' 'existing writer capacity for contained-song search'
Assert-NW59Has $activity 'matchContainedSongs = true' 'Library enables playlist contents search'
Assert-NW59Has $activity '"Search playlists or their songs"' 'accurate search placeholder'
Assert-NW59Has $discoveryTests 'hugeFavoritesAndRecentDoNotHidePlaylistOnlyTracks' 'quota regression'
Assert-NW59Has $smartTests 'rediscoverExcludesRecentAndRetainsOnlyLikedSongs' 'smart recent exclusion test'
Assert-NW59Has $browseTests 'songTermsMustMatchOneSongNotAcrossDifferentPlaylistTracks' 'bounded semantic playlist matching'
Assert-NW59Has $smoke 'repeat(2_000)' 'randomized JVM checks recorded'
# NW72: preserve local recovery safety across disconnect/reconnect of the
# same Google account. Drive previews must be fenced by exact revision, not
# just the account email. No credentials ever enter persisted test receipts.
$cloudSession = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/cloud/CloudAccountSessionPolicy.kt'])
$cloudConsent = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupConsentPolicy.kt'])
$cloudSessionTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/v2/cloud/CloudAccountSessionPolicyTest.kt'])
$cloudSmoke = [IO.File]::ReadAllText($files['tools/NW72CloudSessionSmoke.kt'])
Assert-NW59Has $cloudSession 'expected.revision == current.revision' 'same email is insufficient after reconnect'
Assert-NW59Has $cloudSession 'expected.account.equals(current.account, ignoreCase = true)' 'same authorized account'
Assert-NW59Has $cloudSession 'fun mayApply(' 'no restore during concurrent cloud work'
Assert-NW59Has $cloudConsent 'val revision: Long = 0L' 'session revision source'
Assert-NW59Has $activity 'expectedSession = operationConsent' 'capture exact session after Drive verification'
Assert-NW59Has $activity 'CloudAccountSessionPolicy.mayApply(' 'picker and confirmation reject changed session'
Assert-NW59Has $activity 'CloudAccountSessionPolicy.same(' 'worker rechecks account receipt before data write'
Assert-NW59Has $activity 'showCloudMergePreview(plan, expectedSession)' 'merge preview carries exact receipt'
Assert-NW59Has $activity 'showCloudRestorePreview(remote, comparison, expectedSession)' 'restore preview carries exact receipt'
Assert-NW59Has $activity 'restoreCloudAfterConfirmation(preview, report, false, expectedSession)' 'restore confirmed under original account'
Assert-NW59Has $activity 'restoreCloudAfterConfirmation(preview, report, true, expectedSession)' 'history restore session fenced'
Assert-NW59Has $activity 'showCloudOptionalRestorePrompt(preview, expectedSession)' 'optional prefs/lyrics cannot escape session'
Assert-NW59Has $cloudSessionTests 'sameEmailAfterDisconnectReconnectInvalidatesOldPreview' 'regression for same account reconnect'
Assert-NW59Has $cloudSmoke '0L..2_000L' 'reproducible revision stress'
# NW73: a historically verified backup MUST NOT be represented as
# current after consent/privacy choices change; error 8 guidance is staged.
$freshness = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupFreshnessPolicy.kt'])
$cloudState = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupState.kt'])
$cloudPresentation = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/cloud/CloudAccountPresentation.kt'])
$cloudHelp = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/cloud/CloudConnectionHelpPolicy.kt'])
$freshTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/v2/cloud/CloudBackupFreshnessPolicyTest.kt'])
$presentationTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/v2/cloud/CloudAccountPresentationTest.kt'])
$helpTests = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/v2/cloud/CloudConnectionHelpPolicyTest.kt'])
$smoke73 = [IO.File]::ReadAllText($files['tools/NW73CloudStatusSmoke.kt'])
Assert-NW59Has $freshness 'currentConsentRevision != lastVerifiedRevision' 'current consent revision determines freshness'
Assert-NW59Has $freshness '!automaticEnabled || lastSuccessMs <= 0L' 'no unverified backup success'
Assert-NW59Has $cloudState 'lastVerifiedRevision: Long = -1L' 'legacy backup receipt conservatively unknown'
Assert-NW59Has $cloudState 'last_verified_consent_revision' 'persist last verified consent revision'
Assert-NW59Has $cloudState '.putLong(VERIFIED_REVISION_KEY, consent.revision)' 'only verified upload updates receipt'
Assert-NW59Has $cloudState 'CloudBackupFreshnessPolicy.changed(previous, value)' 'unchanged options preserve backup verification'
Assert-NW59Has $freshness 'fun preservesVerifiedSnapshot(key: String): Boolean = key == "wifi_only"' 'network-only settings do not change backed-up bytes'
Assert-NW59Has $cloudState 'val stillVerified = CloudBackupFreshnessPolicy.preservesVerifiedSnapshot(key)' 'only verified snapshot may stay current after Wi-Fi toggle'
Assert-NW59Has $cloudState 'if (stillVerified) editor.putLong(VERIFIED_REVISION_KEY, newRevision)' 'carry verified receipt only for network-only changes'
Assert-NW59Has $cloudState 'Backup settings changed; back up now to verify the current choices.' 'privacy change explained to user'
Assert-NW59Has $cloudPresentation 'BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH' 'not falsely badge stale snapshot'
Assert-NW59Has $cloudPresentation 'CloudBackupFreshnessPolicy.classify(' 'presentation uses current consent status'
# NW75 compiler hotfix: the NW73 fourth BackupStage must have an
# exhaustive compact You-header label too. No generic 'else' masks additions.
Assert-NW59Has $cloudPresentation 'fun badge(stage: BackupStage): String = when (stage)' 'badge exhaustively maps backup state'
Assert-NW59Has $cloudPresentation 'BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH -> "REFRESH NEEDED"' 'stale backup never labelled backed up'
Assert-NW59Has $activity 'CloudAccountPresentation.badge(' 'You screen retains exhaustive backup badge mapper'
Assert-NW59Has $activity 'CloudAccessPolicy.compactBadge(' 'You screen must not call failed or unknown access connected'
Assert-NW59Missing $activity 'Text(when (CloudAccountPresentation.stage(cloud))' 'prevent duplicated nonexhaustive You-stage when'
Assert-NW59Has $presentationTests 'compactYouBadgeHandlesEveryBackupStageWithoutClaimingStaleIsVerified' 'regression test covers each compact badge'
Assert-NW59Has $hub 'Previous verified backup' 'honest earlier snapshot timestamp'
Assert-NW59Has $hub "New privacy or backup settings aren't verified" 'UI explains stale snapshot'
Assert-NW59Has $cloudHelp 'if (stage == CloudAuthorizationFailurePolicy.Stage.REQUEST)' 'request stage error 8 copy'
Assert-NW59Has $cloudHelp 'before a verified account selection' 'request error before chooser'
Assert-NW59Has $cloudHelp 'The account picker appeared' 'picker-stage error distinguished'
Assert-NW59Has $freshTests 'previouslyVerifiedBackupIsNotCurrentAfterConsentChanges' 'freshness regression tests'
Assert-NW59Has $presentationTests 'stageUsesActualVerifiedConsentRevisionRatherThanOnlyTimestamp' 'actual Account stage test'
Assert-NW59Has $helpTests 'requestStageEightNeverClaimsThePickerAppeared' 'stage 8 guidance tests'
Assert-NW59Has $smoke73 '0L..2_000L' 'reproducible receipt-revision stress'
# NW74: recover invalid Library selections and reject Android dialog indices
# from stale snapshots. UI-only: do not mutate persisted library or media.
$studioSafety = [IO.File]::ReadAllText($files['app/src/main/java/com/nightwave/player/v2/ui/StudioUiStateSafetyPolicy.kt'])
$studioSafetyTest = [IO.File]::ReadAllText($files['app/src/test/java/com/nightwave/player/v2/ui/StudioUiStateSafetyPolicyTest.kt'])
$studioSmoke = [IO.File]::ReadAllText($files['tools/NW74UiStateSmoke.kt'])
Assert-NW59Has $studioSafety 'fun resolveCollection(' 'resolve deleted or renamed Library selection'
Assert-NW59Has $studioSafety 'selected?.takeIf' 'never invent unavailable collection'
Assert-NW59Has $studioSafety 'options.getOrNull(index)' 'native dialog indices are bounded'
Assert-NW59Has $studioSafety 'fun createdPlaylistName(' 'find actual newly created playlist'
Assert-NW59Has $studioSafety 'after.lastOrNull { it !in existing }' 'no empty-list last crash'
Assert-NW59Has $activity 'val resolvedPlaylist = StudioUiStateSafetyPolicy.resolveCollection(' 'Library screen uses existing selection'
Assert-NW59Has $activity 'LaunchedEffect(selectedPlaylist, resolvedPlaylist)' 'clear restored stale selection'
Assert-NW59Has $activity 'selectedPlaylist = resolvedPlaylist,' 'no ghost detail screens'
Assert-NW59Has $localCorpus 'val current = if (state.refresh == refresh)' 'manual refresh immediately gates old corpus'
Assert-NW59Has $activity 'StudioUiStateSafetyPolicy.itemAt(names, chosen)' 'copy playlist dialog bounds'
Assert-NW59Has $activity 'StudioUiStateSafetyPolicy.itemAt(names, option)' 'save playlist dialog bounds'
Assert-NW59Has $activity 'StudioUiStateSafetyPolicy.itemAt(playlists, selected)' 'folder playlist chooser bounds'
Assert-NW59Has $activity 'StudioUiStateSafetyPolicy.itemAt(choices, index)' 'folder manager chooser bounds'
Assert-NW59Has $activity 'StudioUiStateSafetyPolicy.itemAt(choices, chosen)' 'folder item chooser bounds'
Assert-NW59Has $activity 'StudioUiStateSafetyPolicy.itemAt(options, which)' 'Drive snapshot chooser bounds'
Assert-NW59Has $activity 'option !in choices.indices' 'invalid chooser cannot trigger create'
Assert-NW59Has $activity 'StudioUiStateSafetyPolicy.createdPlaylistName(' 'safe shared playlist import result'
Assert-NW59Missing $activity 'outcome.updated.playlists.last().first' 'no unchecked last() on mutable library'
Assert-NW59Has $studioSafetyTest 'removedPlaylistOrSmartCollectionGoesBackToLibraryOverview' 'selection unit regression'
Assert-NW59Has $studioSafetyTest 'delayedNativeDialogIndicesNeverThrowOrChooseUnrelatedItems' 'dialog unit regression'
Assert-NW59Has $studioSmoke 'repeat(2500)' 'reproducible bounded selection test'
# NW75: catch the impossible one-playlist 250-track NW71 JUnit test,
# require reproducing core Kotlin policy tests, and preserve signer/build gates.
# JUnit and Android compilation still run later; this is a source-only check.
$finalSmoke = [IO.File]::ReadAllText($files['tools/NW75IntegrationSmoke.kt'])
Assert-NW59Has $discoveryTests 'listOf(playlist.take(200), playlist.drop(200))' 'NW71 250 quota requires two 200-per-playlist sources'
Assert-NW59Missing $discoveryTests 'listOf(playlist), { it }, { true })' 'impossible single playlist quota test'
Assert-NW59Has $discoveryPool 'it.asSequence().take(200)' 'NW71 per-playlist scan remains bounded'
Assert-NW59Has $finalSmoke 'SmartDiscoveryPoolPolicy.gather(' 'NW71 real production pool in final smoke'
Assert-NW59Has $finalSmoke 'StudioUiStateSafetyPolicy.resolveCollection(' 'NW74 restored screen safety in final smoke'
Assert-NW59Has $finalSmoke 'CloudAccountSessionPolicy.mayApply(' 'NW72 cloud account session fence in final smoke'
Assert-NW59Has $finalSmoke 'CloudBackupFreshnessPolicy.Freshness.CURRENT_VERIFIED' 'NW73 actual receipt checking in smoke'
Assert-NW59Has $finalSmoke 'repeat(3_000)' 'deterministic cross-phase randomized regression'
Assert-NW59Has $finalSmoke 'NW75 integrated pure Kotlin source smoke PASS' 'test prints honest isolated result'
Assert-NW59Has $activity 'CloudAccountSessionPolicy.same(' 'cloud restore still verifies account session'
Assert-NW59Has $activity 'StudioUiStateSafetyPolicy.itemAt(options, which)' 'Drive selection still bounded'
# NW76 build-certification guard: source checks validate that the real
# Windows builder invokes an independent current-attempt JUnit XML receipt
# BEFORE promotion of any debug APK. Static contracts are not proof of execution.
$nw76Proof = [IO.File]::ReadAllText($files['tools/nw76-junit-proof.ps1'])
$nw76Builder = [IO.File]::ReadAllText($files['tools/build-windows.ps1'])
Assert-NW59Has $nw76Proof 'function Measure-JunitReceipt(' 'unit test receipt evaluator present'
Assert-NW59Has $nw76Proof "Filter 'TEST-*.xml'" 'actual Gradle JUnit XML required'
Assert-NW59Has $nw76Proof 'DtdProcessing]::Prohibit' 'XML DTD prohibited'
Assert-NW59Has $nw76Proof '$cases -ne $n' 'reject forged/inconsistent testcase count'
Assert-NW59Has $nw76Proof '$executed = $total - $skipped' 'executed test count must exclude skipped cases'
Assert-NW59Has $nw76Proof 'if ($executed -lt $minimum)' 'minimum executed JUnit count not silently skipped'
Assert-NW59Has $nw76Proof '$failed -ne 0 -or $errors -ne 0' 'fail if any JVM tests fail'
Assert-NW59Has $nw76Proof 'function Invoke-SelfTest' 'parser self-test fixture'
Assert-NW59Has $nw76Builder 'if (Test-Path -LiteralPath $junitProofOutput) { Remove-Item' 'remove stale test receipt at start'
Assert-NW59Has $nw76Builder 'NW76 JUnit result certification' 'real proof is a separate gated stage'
Assert-NW59Has $nw76Builder '-MinimumTests 1400' 'enforce current broad JVM regression count'
Assert-NW59Has $nw76Builder 'JUnit receipt: dist\\NightWave-jvm-test-proof.txt' 'saved status points to fresh test receipt'
Assert-NW59Has $nw76Builder 'Copy-Item -LiteralPath $apk -Destination $output -Force' 'original verified APK promotion intact'
$certifyAt = $nw76Builder.IndexOf('    $stage = ''NW76 JUnit result certification''')
$publishAt = $nw76Builder.IndexOf('Copy-Item -LiteralPath $apk -Destination $output -Force')
if ($certifyAt -lt 0 -or $publishAt -le $certifyAt) {
    throw 'NW76 regression: JUnit certification must precede APK promotion.'
}
Write-Host 'NW76 cumulative NW53-NW76 source guard PASS. Gradle/JVM/device QA still mandatory.'
exit 0
