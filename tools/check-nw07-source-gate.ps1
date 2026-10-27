# NW-07: fail-closed, read-only Windows source gate for integrated NightWave.
# Uses built-in PowerShell only. Never signs or installs APKs or touches user data.
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'

function Require([bool]$ok, [string]$reason) {
    if (-not $ok) { throw $reason }
}
function Check-Marker([string]$body, [string]$marker, [string]$label) {
    Require ($body.Contains($marker)) ("Missing integrated contract: " + $label)
}
function Gate-File([string]$root, [string]$rel) {
    # Fixed paths only; never read system or user music.
    $full = Join-Path $root ($rel.Replace('/', [IO.Path]::DirectorySeparatorChar))
    Require (Test-Path -LiteralPath $full -PathType Leaf) ("Incomplete NW-07 ZIP: " + $rel)
    return [IO.File]::ReadAllText($full)
}
function Check-Imports([string]$body, [string]$label) {
    $imports = @($body -split "\r?\n" | Where-Object { $_ -match '^import\s+' })
    # Sort-Object -Unique uses case-insensitive comparisons by default.
    # Kotlin identifiers are case-sensitive, so class and property imports
    # differing only by capitalization are both valid.
    $seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($import in $imports) {
        $exact = $import.Trim()
        if (-not $seen.Add($exact)) {
            throw ("Duplicate Kotlin import: " + $label + ": " + $exact)
        }
    }
}

try {
    if ($SelfTest) {
        Check-Marker 'hello world' 'hello' 'selftest-positive'
        Check-Imports ("import kotlin.String" + [Environment]::NewLine +
            "import kotlin.Int") 'selftest-unique'
        # This exact pair exists in NightWaveSeekTimeline.kt. One is a
        # semantics class and the other is a distinct semantics property.
        Check-Imports ("import androidx.compose.ui.semantics.ProgressBarRangeInfo" +
            [Environment]::NewLine +
            "import androidx.compose.ui.semantics.progressBarRangeInfo") 'selftest-case-distinct'
        $negative = $false
        try { Check-Marker 'not here' 'missing' 'selftest-negative' }
        catch { $negative = $true }
        Require $negative 'Negative marker self-test unexpectedly passed.'
        $duplicateRejected = $false
        try { Check-Imports "import kotlin.String`nimport kotlin.String" 'selftest-duplicate' }
        catch { $duplicateRejected = $true }
        Require $duplicateRejected 'Duplicate import self-test unexpectedly passed.'
        $caseTrueDuplicateRejected = $false
        try {
            Check-Imports ("import androidx.compose.ui.semantics.ProgressBarRangeInfo" +
                [Environment]::NewLine +
                "import androidx.compose.ui.semantics.progressBarRangeInfo" +
                [Environment]::NewLine +
                "import androidx.compose.ui.semantics.ProgressBarRangeInfo") 'selftest-case-repeat'
        } catch { $caseTrueDuplicateRejected = $true }
        Require $caseTrueDuplicateRejected 'Exact duplicate after case-distinct imports was not rejected.'
        Write-Host 'PASS: NW07 source gate case-sensitive import positive/negative self-tests'
        exit 0
    }
    if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
        $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
    }
    Require (Test-Path -LiteralPath $ProjectRoot -PathType Container) 'Source root is missing.'
    $ui = Gate-File $ProjectRoot 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt'
    $bridge = Gate-File $ProjectRoot 'app/src/main/java/com/nightwave/player/v2/playback/StudioPlaybackBridge.kt'
    $service = Gate-File $ProjectRoot 'app/src/main/java/com/nightwave/player/playback/PlaybackService.kt'
    $player = Gate-File $ProjectRoot 'app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt'
    $studio = Gate-File $ProjectRoot 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveSoundStudio.kt'
    $hub = Gate-File $ProjectRoot 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveAccountHub.kt'
    $timeline = Gate-File $ProjectRoot 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekTimeline.kt'
    $gateway = Gate-File $ProjectRoot 'app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt'
    $gradle = Gate-File $ProjectRoot 'app/build.gradle.kts'
    $manifest = Gate-File $ProjectRoot 'app/src/main/AndroidManifest.xml'

    Check-Marker $bridge 'ShuffleModeReceiptStore(context).matches(' 'NW01 shuffle receipt'
    Check-Marker $service 'ShuffleModePolicy.resolve(' 'NW01 absolute shuffle request'
    Check-Marker $studio 'NightWaveSoundStudioDialog' 'NW02 sound studio'
    Check-Marker $service 'candidate.hasControl()' 'NW02 real audio-effect control'
    Check-Marker $hub 'CloudAccountPresentation.stage(cloud)' 'NW03 guest backup status'
    Check-Marker $ui 'NightWaveAccountHubDialog(' 'NW03 account hub'
    Check-Marker $timeline 'trackIdentity: String' 'NW04/NW05 scrub identity'
    Check-Marker $bridge 'PlaybackSeekIdentityPolicy.target(' 'NW05 Media3 song identity'
    Check-Marker $player 'NightWaveSeekTimeline(' 'NW04 seek UI'
    Check-Marker $studio 'SoundStudioPolicy.safeManualRange(' 'NW05 safe EQ limits'
    Check-Marker $service 'PublicQueueSelectionPolicy.selectedIndex(' 'NW05 queue identity'
    Check-Marker $ui 'LocalSearchSuggestions.fromMatches(query, savedTracks, suggestionMatches, limit = 6)' 'NW06 same-query local search suggestions'
    Check-Marker $ui 'if (savedTracks.size <= 5_000) matches' 'NW122 shared suggestions within existing source limit'
    Check-Marker $ui 'else SavedTrackSearch.match(query, savedTracks.take(5_000))' 'NW122 larger-library suggestion boundary'
    Check-Marker $ui 'SearchResultPresentationPolicy.matches(' 'NW06 search filter state'
    Check-Marker $gateway 'CommunityVideoSearch()' 'NW06 independent search fallback'

    Check-Marker $gradle 'minSdk = 26' 'Android minimum SDK'
    Check-Marker $gradle 'applicationId = "com.nightwave.player"' 'Android package identity'
    foreach ($name in @(
        'NW01', 'NW02', 'NW03', 'NW04', 'NW05', 'NW06'
    )) {
        # Verify integration and test files exist; no test is considered PASS
        # until Gradle actually executes.
        switch ($name) {
            'NW01' { $test = 'app/src/test/java/com/nightwave/player/playback/ShuffleModePolicyTest.kt' }
            'NW02' { $test = 'app/src/test/java/com/nightwave/player/playback/SoundStudioPolicyTest.kt' }
            'NW03' { $test = 'app/src/test/java/com/nightwave/player/v2/cloud/CloudAccountPresentationTest.kt' }
            'NW04' { $test = 'app/src/test/java/com/nightwave/player/v2/ui/NightWaveSeekGesturePolicyTest.kt' }
            'NW05' { $test = 'app/src/test/java/com/nightwave/player/v2/playback/PlaybackSeekIdentityPolicyTest.kt' }
            'NW06' { $test = 'app/src/test/java/com/nightwave/player/net/SearchResultPresentationPolicyTest.kt' }
        }
        $body = Gate-File $ProjectRoot $test
        Check-Marker $body '@Test' ($name + ' regression test source')
    }
    Check-Imports $ui 'StudioActivity'
    Check-Imports $bridge 'PlaybackBridge'
    Check-Imports $service 'PlaybackService'
    Check-Imports $player 'PlayerUI'
    Check-Imports $studio 'SoundStudio'
    Check-Imports $hub 'AccountHub'
    Check-Imports $timeline 'SeekTimeline'
    Write-Host 'PASS: NW07 complete source tree and NW01-NW06 integration preflight'
    Write-Host 'NOT VERIFIED: Kotlin compilation, JVM tests, APK signer and phone runtime'
    exit 0
} catch {
    Write-Host ('FAIL: NW07 source preflight: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
