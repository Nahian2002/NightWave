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
    $finish = $Body.IndexOf('private fun MoodTile(', [Math]::Max(0, $start))
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
    'app/src/main/java/com/nightwave/player/playback/PlaybackService.kt',
    'app/src/main/java/com/nightwave/player/playback/OfflineDecodeRecoveryPolicy.kt',
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
Assert-NW59Has $downloads 'OfflineAudioLibrary(applicationContext).availableIds(ids)' 'NW62 background verification of actual saved files'
Assert-NW59Has $downloads 'Resume restarts from zero' 'honest no-byte-resume label'
Assert-NW59Missing $downloads 'LayoutParams(-1, dp(44))' 'undersized transfer actions'
Assert-NW59Has $filter 'Filter.ATTENTION -> "Issues"' 'compact offline filter'
Assert-NW59Has $filter 'Filter.ATTENTION -> "Needs attention"' 'accessible offline filter'
Assert-NW59Has $palette '0xFF815154' 'readable Pearl accent'
Assert-NW59Has $palette 'else 0xFFF9F7F2' 'readable Pearl action text'
Assert-NW59Has $activity 'openSearch = { navigateToTab(StudioTab.SEARCH) }' 'Home Search action preserved'
Assert-NW59Has $player 'seekToForTrack' 'safe lyric track-bound seek'
# NW60 recovers once per local ID instead of silently ignoring decoder errors.
$playback = [IO.File]::ReadAllText($files[$main + 'playback/PlaybackService.kt'])
$decode = [IO.File]::ReadAllText($files[$main + 'playback/OfflineDecodeRecoveryPolicy.kt'])
Assert-NW59Has $playback 'OfflineDecodeRecoveryPolicy.belongsToSelection(' 'decoder error belongs to active selection'
Assert-NW59Has $playback 'offlineDecodeFailedIds.add(selectedId!!)' 'bounded failed ID receipt'
Assert-NW59Has $playback 'generation != atErrorGeneration' 'deferred recovery generation fence'
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
Assert-NW59Has $downloads 'OfflineAudioLibrary(applicationContext).availableIds(ids)' 'background file inspection'
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
Assert-NW59Has $downloads 'DownloadHistoryPagePolicy.offset(historyPage, filteredJobs.size)' 'all filtered history remains reachable'
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
Write-Host 'NW65 cumulative NW53-NW65 source guard PASS. Gradle/JVM/device QA still mandatory.'
exit 0
