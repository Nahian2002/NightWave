# NightWave 6V4D local source and regression-discovery gate.
# Windows PowerShell 5.1, no Python/Java/SDK/network/admin/phone needed.
# A PASS is NOT a Kotlin compilation, emulator test or signed-APK approval.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$errors = New-Object 'System.Collections.Generic.List[string]'

function Source([string]$relative) {
    $p = Join-Path $root ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $p -PathType Leaf)) {
        $script:errors.Add("Missing: $relative")
        return ''
    }
    return [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)
}
function Require([string]$relative, [string]$marker, [string]$reason) {
    $text = Source $relative
    if (-not $text.Contains($marker)) {
        $script:errors.Add("$relative : $reason")
    }
}
function RequireTests([string]$relative, [int]$minimum) {
    $text = Source $relative
    $count = ([regex]::Matches($text, '@Test\s+fun\s+')).Count
    if ($count -lt $minimum) {
        $script:errors.Add("$relative : $count tests, expected at least $minimum")
    }
}

$queueStore = 'app/src/main/java/com/nightwave/player/playback/QueueStateStore.kt'
$offlineLib = 'app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt'
$offlineUi = 'app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt'
$service = 'app/src/main/java/com/nightwave/player/playback/PlaybackService.kt'
$bridge = 'app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt'

Require $queueStore 'QueueRestorePolicy.selectedIndex(originalUrls,' 'selected song must survive corrupt queue rows'
Require $queueStore 'put("selected_url",' 'persist selected public ID'
Require $queueStore '.commit()' 'durable selection write'
Require 'app/src/main/java/com/nightwave/player/playback/QueueRestorePolicy.kt' 'isCanonicalPublicWatchUrl' 'reject signed media URLs'
Require $offlineLib 'fun healthReport(): OfflineLibraryHealthReport' 'read-only offline audit missing'
Require $offlineLib 'inspectFile(song).first != OfflineFileHealth.AVAILABLE' 'unsafe delete must fail closed'
Require $offlineLib 'Files.isSymbolicLink(' 'offline path inspection'
# NW90 performs the health audit off the main thread. Preserve that safety
# improvement instead of requiring the retired synchronous Activity call.
Require $offlineUi 'worker.execute {' 'offline audit must run away from UI thread'
Require $offlineUi 'val result = runCatching { library.healthReport() }' 'read-only background health report'
Require $offlineUi 'runOnUiThread {' 'background health result must return to UI thread'
Require $offlineUi 'cachedReport = result.getOrNull()' 'successful health result must be cached'
Require $offlineUi 'val rawReport = cachedReport ?: run {' 'UI must consume cache rather than synchronously scanning disk'
Require $offlineUi 'val report = rawReport.copy(entries = rawReport.entries.map' 'audio-check failures must overlay the cached snapshot without a disk scan'
Require $offlineUi 'entry.copy(health = OfflineFileHealth.UNREADABLE)' 'failed audio checks must not remain available'
Require $offlineUi 'requestHealthCheck()' 'no cached report must schedule background refresh'
Require $offlineUi 'if (!report.indexReadable)' 'unreadable health result must fail closed'
Require $offlineUi 'report.attentionCount' 'unavailable file count shown'
Require 'app/src/main/java/com/nightwave/player/offline/OfflineLibraryHealth.kt' 'SIZE_CHANGED' 'changed file size classification'
Require $service 'PlaybackRecoveryPolicy.mayAutoRetry(' 'user-intent check before retry'
Require $service 'PlaybackRecoveryPolicy.acceptsPostedRetry(' 'stale posted retry fence'
Require $service 'PlaybackRecoveryPolicy.acceptsResolvedTrack(entry.url, video.url)' 'extractor cannot switch songs'
Require $bridge 'MediaReconnectPolicy.mayAttempt(' 'bounded foreground connection'
Require $bridge 'controller !== player' 'stale controller event rejection'
Require 'app/src/main/java/com/nightwave/player/playback/PlaybackRecoveryPolicy.kt' 'timerPaused' 'sleep-timer fence'
Require 'app/src/main/java/com/nightwave/player/v2/playback/MediaReconnectPolicy.kt' 'oneShotAvailable' 'reconnect budget'
Require 'app/src/main/java/com/nightwave/player/v2/locker/NightWaveMusicDriveClient.kt' 'request(builder.build().toString()).get().build()' 'previous Kotlin compile fix'
Require 'app/src/test/java/com/nightwave/player/v2/cloud/CloudRecoveryPolicyTest.kt' 'reportCountsEveryMissingPlaylistWithoutModifyingInputs' 'previous failing JUnit test fixture'
Require 'tools/build-windows.ps1' ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'real Gradle gate missing'
Require 'tools/build-windows.ps1' 'verify-apk-signature.ps1' 'APK signature check missing'

RequireTests 'app/src/test/java/com/nightwave/player/playback/QueueRestorePolicyTest.kt' 6
RequireTests 'app/src/test/java/com/nightwave/player/offline/OfflineLibraryHealthPolicyTest.kt' 6
RequireTests 'app/src/test/java/com/nightwave/player/playback/PlaybackRecoveryPolicyTest.kt' 9
RequireTests 'app/src/test/java/com/nightwave/player/v2/playback/MediaReconnectPolicyTest.kt' 4
RequireTests 'app/src/androidTest/java/com/nightwave/player/playback/QueueStateBookmarkInstrumentedTest.kt' 15
RequireTests 'app/src/androidTest/java/com/nightwave/player/offline/OfflineLibraryHealthInstrumentedTest.kt' 3

if ($errors.Count -gt 0) {
    Write-Host "FAIL: Phase 6V4D integration preflight found $($errors.Count) problem(s)." -ForegroundColor Red
    $errors | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: Phase 6V4A/B/C source hooks and required regression tests are present.' -ForegroundColor Green
Write-Host 'NOT YET VERIFIED: Kotlin, JVM test execution, Android test compilation, signer and device tests.' -ForegroundColor Yellow
exit 0
