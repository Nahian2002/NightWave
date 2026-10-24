# NW34 offline safety source gate. ASCII PowerShell 5.1 compatible.
# This is a fail-closed source preflight, not an Android compiler.
[CmdletBinding()]
param([switch]$SelfTest)
$ErrorActionPreference = 'Stop'

function Has-Token([string]$Source, [string]$Required) {
    return $Source.Contains($Required)
}

if ($SelfTest) {
    if (-not (Has-Token 'canContinue(123L, 4L)' 'canContinue(')) {
        throw 'NW34 positive offline source fixture failed.'
    }
    if (Has-Token 'canStart(123L, 4L)' 'canContinue(') {
        throw 'NW34 negative offline source fixture failed.'
    }
    if (Has-Token 'private link' 'Ready offline') {
        throw 'NW34 false offline readiness fixture failed.'
    }
    Write-Host 'PASS: NW34 offline source checker self-test.'
    exit 0
}

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$checks = @(
    @('app\src\main\java\com\nightwave\player\offline\OfflineSaveStoragePolicy.kt',
        'fun canContinue('),
    @('app\src\main\java\com\nightwave\player\offline\PermittedAudioDownloader.kt',
        'OfflineSaveStoragePolicy.canContinue(freeNow, received)'),
    @('app\src\main\java\com\nightwave\player\offline\PermittedAudioDownloader.kt',
        'inspectAudioOnly(pending)'),
    @('app\src\main\java\com\nightwave\player\offline\SearchOfflineFailurePolicy.kt',
        'device storage is too low'),
    @('app\src\main\java\com\nightwave\player\offline\OfflineDownloadStatusPolicy.kt',
        'workSucceeded && !savedId.isNullOrBlank()'),
    @('app\src\main\java\com\nightwave\player\offline\SearchTrackOfflineIndex.kt',
        'availableIds(requested)'),
    @('app\src\main\java\com\nightwave\player\offline\SearchTrackSaveWorker.kt',
        'SearchTrackOfflineIndex.linkVerified('),
    @('app\src\main\java\com\nightwave\player\offline\SearchTrackSaveWorker.kt',
        'SearchOfflineFailurePolicy.message('),
    @('app\src\main\java\com\nightwave\player\offline\OfflineSaveRetryPolicy.kt',
        'fun retryUrl(tags: Set<String>): String?'),
    @('app\src\main\java\com\nightwave\player\offline\CatalogDownloadsActivity.kt',
        'OfflineDownloadStatusPolicy.isReady('),
    @('app\src\main\java\com\nightwave\player\v2\ui\NightWaveStudioActivity.kt',
        'playSearchedOfflineFirst(video, collection)'),
    @('app\src\main\java\com\nightwave\player\offline\OfflineAudioLibrary.kt',
        'fun availableIds(requested: Set<String>): Set<String>'),
    @('app\src\test\java\com\nightwave\player\offline\OfflineSaveStoragePolicyTest.kt',
        'unknownSizeTransfersMustPreserveSpaceForTheVerifiedCopy'),
    @('app\src\test\java\com\nightwave\player\offline\SearchOfflineFailurePolicyTest.kt',
        'diskExhaustionDuringUnknownLengthTransferIsExplained'),
    @('app\src\androidTest\java\com\nightwave\player\offline\OfflineLibraryHealthInstrumentedTest.kt',
        'batchAvailableIdsRejectsMissingOrChangedFilesWithoutRewritingIndex'),
    @('app\src\main\AndroidManifest.xml',
        'android:usesCleartextTraffic="false"')
)
$failures = 0
foreach ($entry in $checks) {
    $relative = [string]$entry[0]
    $full = Join-Path $root $relative
    if (-not (Test-Path -LiteralPath $full -PathType Leaf)) {
        Write-Host ('FAIL: missing offline source: ' + $relative) -ForegroundColor Red
        $failures++
        continue
    }
    $body = [System.IO.File]::ReadAllText($full, [System.Text.Encoding]::UTF8)
    if (-not (Has-Token $body ([string]$entry[1]))) {
        Write-Host ('FAIL: offline contract missing: ' + $relative) -ForegroundColor Red
        $failures++
    }
}
if ($failures -gt 0) {
    Write-Host ('FAIL: NW34 offline contract preflight: ' + $failures + ' error(s).') -ForegroundColor Red
    exit 1
}
Write-Host 'PASS: NW34 offline source contracts (16 checks).' -ForegroundColor Green
Write-Host 'LIMIT: Android Gradle compilation, actual network transfers and playback still require testing.'
exit 0
