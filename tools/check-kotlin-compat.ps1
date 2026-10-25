# NightWave fast local Kotlin source preflight.
# Standard Windows PowerShell 5.1; no Python, Gradle, SDK or network needed.
# This is a guard against known bad imports, NOT a Kotlin compiler.
[CmdletBinding()]
param([switch]$SelfTest)
$ErrorActionPreference = 'Stop'

function Get-ExactDuplicateImports([string[]]$Names) {
    # Group-Object and -contains use case-insensitive comparison by default.
    # Kotlin identifiers ARE case-sensitive: ProgressBarRangeInfo (class)
    # and progressBarRangeInfo (semantics property) are both needed.
    $seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $duplicates = [System.Collections.Generic.List[string]]::new()
    foreach ($name in $Names) {
        if (-not $seen.Add($name)) {
            $duplicates.Add($name)
        }
    }
    return @($duplicates.ToArray())
}

function Test-NightWaveGreeting([string]$Body) {
    # ASCII-only PowerShell 5.1 safeguard for the actual NW21 Home Kotlin
    # string syntax error. Gradle remains the authoritative Kotlin checker.
    return ($Body.Contains('"Welcome, " + firstName + "') -and -not $Body.Contains('"Welcome,  + firstName + "'))
}

function Test-ComposeAnimatedDelegate([string]$Body) {
    # Compose animate*AsState returns State<T>. Delegation with "by" must
    # have runtime.getValue (or the runtime wildcard) imported in that file.
    # This is a syntax-pattern guard, not a type checker.
    $delegated = [regex]::IsMatch($Body,
        '(?m)^\s*(?:val|var)\s+\w+\s+by\s+animate\w*AsState\s*\(')
    if (-not $delegated) { return $true }
    return ($Body.Contains('import androidx.compose.runtime.getValue') -or
        $Body.Contains('import androidx.compose.runtime.*'))
}

function Test-QualifiedFilterEnum([string]$Body, [string]$PolicyType) {
    # A singleton value's p.Filter is not a Kotlin nested type path.
    # Always reference PolicyType.Filter, regardless of a local val p.
    return (-not $Body.Contains('p.Filter.') -and
        $Body.Contains(($PolicyType + '.Filter.')))
}

if ($SelfTest) {
    if (-not (Test-NightWaveGreeting '"Welcome, " + firstName + "good"')) {
        throw 'Valid Home greeting concatenation was rejected.'
    }
    if (Test-NightWaveGreeting '"Welcome,  + firstName + "bad"') {
        throw 'Broken NW21 Home greeting Kotlin literal was not rejected.'
    }
    $validCompose = @(
        'androidx.compose.ui.semantics.ProgressBarRangeInfo',
        'androidx.compose.ui.semantics.progressBarRangeInfo'
    )
    $validDuplicates = @(Get-ExactDuplicateImports $validCompose)
    if ($validDuplicates.Count -ne 0) {
        throw 'Kotlin case-sensitive import regression: valid Compose symbols were conflated.'
    }
    $repeatedNames = @('a.B', 'a.b', 'a.B')
    $actuallyRepeated = @(Get-ExactDuplicateImports $repeatedNames)
    if ($actuallyRepeated.Count -ne 1 -or $actuallyRepeated[0] -cne 'a.B') {
        throw 'Kotlin exact duplicate import was not detected.'
    }
    $badAnimation = 'import androidx.compose.animation.animateColorAsState' + [Environment]::NewLine +
        'val background by animateColorAsState(targetValue = Color.White)'
    if (Test-ComposeAnimatedDelegate $badAnimation) {
        throw 'Missing Compose runtime.getValue import was not detected.'
    }
    if (-not (Test-ComposeAnimatedDelegate (
        'import androidx.compose.runtime.getValue' + [Environment]::NewLine + $badAnimation))) {
        throw 'Correctly imported animated delegate was rejected.'
    }
    if (-not (Test-ComposeAnimatedDelegate 'val background = animateColorAsState(targetValue = Color.White).value')) {
        throw 'Non-delegated Compose State incorrectly requires getValue.'
    }
    if (-not (Test-QualifiedFilterEnum 'DownloadCenterViewPolicy.Filter.READY' 'DownloadCenterViewPolicy')) {
        throw 'Properly qualified nested policy Filter enum was rejected.'
    }
    if (Test-QualifiedFilterEnum 'p.Filter.READY' 'DownloadCenterViewPolicy') {
        throw 'Invalid Kotlin p.Filter nested enum reference was accepted.'
    }
    Write-Host 'PASS: Filter enum references use the declaring Kotlin object type.'
    Write-Host 'PASS: Compose animation delegates require runtime.getValue when using by.'
    Write-Host 'PASS: Kotlin import checker distinguishes case and detects genuine duplicates.'
    exit 0
}
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$sourceRoot = Join-Path $root 'app\src'
if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) {
    throw 'NightWave app/src is missing; use the complete branch ZIP.'
}
$files = @(Get-ChildItem -LiteralPath $sourceRoot -Filter '*.kt' -Recurse -File)
if ($files.Count -lt 1) { throw 'No Kotlin source files were found.' }
$invalid = @(
    'androidx.compose.ui.test.assertExists',
    'androidx.compose.ui.test.assertDoesNotExist',
    'androidx.compose.foundation.layout.weight'
)
$issues = New-Object System.Collections.Generic.List[string]
foreach ($file in $files) {
    $imports = @()
    foreach ($line in [System.IO.File]::ReadAllLines($file.FullName)) {
        if (-not $line.StartsWith('import ')) { continue }
        $name = $line.Substring(7).Trim().Split(' ')[0]
        $imports += $name
        if ($invalid -ccontains $name) {
            $issues.Add(('Invalid Compose import in {0}: {1}' -f
                $file.FullName.Substring($root.Length + 1), $name))
        }
    }
    # Compose sizing methods are top-level extension functions. A reference
    # compiles only when its foundation.layout import is in the same file.
    # Check every Kotlin file, not just the three positions from the 6H1 log.
    $source = [System.IO.File]::ReadAllText($file.FullName)
    if (-not (Test-ComposeAnimatedDelegate $source)) {
        $issues.Add(('Missing androidx.compose.runtime.getValue for animated State delegate in {0}' -f
            $file.FullName.Substring($root.Length + 1)))
    }
    if ($source -match '\.\s*fillMaxSize\s*\(' -and
        -not ($imports -ccontains 'androidx.compose.foundation.layout.fillMaxSize') -and
        -not ($imports -ccontains 'androidx.compose.foundation.layout.*')) {
        $issues.Add(('Missing androidx.compose.foundation.layout.fillMaxSize import in {0}' -f
            $file.FullName.Substring($root.Length + 1)))
    }
    $duplicates = @(Get-ExactDuplicateImports $imports)
    foreach ($duplicate in $duplicates) {
        $issues.Add(('Duplicate Kotlin import in {0}: {1}' -f
            $file.FullName.Substring($root.Length + 1), $duplicate))
    }
}
$homeChromePath = Join-Path $sourceRoot 'main\java\com\nightwave\player\v2\ui\NightWavePremiumChrome.kt'
if (-not (Test-Path -LiteralPath $homeChromePath -PathType Leaf)) {
    $issues.Add('Missing NightWave Home chrome; full source ZIP required.')
} elseif (-not (Test-NightWaveGreeting ([System.IO.File]::ReadAllText($homeChromePath)))) {
    $issues.Add('NightWave Home greeting has a broken Kotlin literal; restore correct firstName concatenation.')
}

# Known Kotlin compiler errors from the 2026-10-19 Windows build.
# Keep the preflight narrow: this does not replace API/type checking by Gradle.
$playbackPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\playback\PlaybackService.kt'
$previewPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\net\YouTubeGateway.kt'
$smartPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\v2\library\SmartLibraryPolicy.kt'
if (Test-Path -LiteralPath $playbackPath -PathType Leaf) {
    $playbackSource = [System.IO.File]::ReadAllText($playbackPath)
    if ($playbackSource -match 'AcceptedResultBuilder\s*\(\s*session\s*,\s*controller\s*\)') {
        $issues.Add('Media3 1.6.1 AcceptedResultBuilder requires one MediaSession argument, not session + controller.')
    }
    if ($playbackSource.Contains('MediaSession.ConnectionResult.AcceptedResultBuilder') -and
        -not $playbackSource.Contains('MediaSession.ConnectionResult.AcceptedResultBuilder(session)')) {
        $issues.Add('Missing verified Media3 1.6.1 AcceptedResultBuilder(session) invocation.')
    }
}
if ((Test-Path -LiteralPath $smartPath -PathType Leaf) -and
    (Test-Path -LiteralPath $previewPath -PathType Leaf)) {
    $smartSource = [System.IO.File]::ReadAllText($smartPath)
    $previewSource = [System.IO.File]::ReadAllText($previewPath)
    if (-not $previewSource.Contains('val durationSeconds: Long')) {
        $issues.Add('VideoPreview durationSeconds model contract changed; review Smart Library duration handling.')
    }
    if ($smartSource -match '\b(?:it|song)\.duration\s*(?:>=|>|<=|<|==)') {
        $issues.Add('SmartLibraryPolicy uses VideoPreview.duration, which does not exist; use durationSeconds.')
    }
    if (-not $smartSource.Contains('it.durationSeconds >= 240L')) {
        $issues.Add('Smart Library long-listen durationSeconds boundary check is missing.')
    }
}

# NW52 2026-10-25 Gradle compileDebugUnitTestKotlin errors:
# Nested enums must be accessed via their declaring object type. In Kotlin,
# 'val p = DownloadCenterViewPolicy; p.Filter.READY' does NOT resolve.
foreach ($tuple in @(
    @('DownloadCenterViewPolicyTest.kt', 'DownloadCenterViewPolicy'),
    @('OfflineLibraryViewPolicyTest.kt', 'OfflineLibraryViewPolicy')
)) {
    $testPath = Join-Path $sourceRoot ('test/java/com/nightwave/player/offline/' + $tuple[0])
    if (-not (Test-Path -LiteralPath $testPath -PathType Leaf)) {
        $issues.Add(('Missing NW48 Kotlin unit test: ' + $tuple[0]))
        continue
    }
    $testSource = [System.IO.File]::ReadAllText($testPath)
    if (-not (Test-QualifiedFilterEnum $testSource $tuple[1])) {
        $issues.Add(('Invalid enum qualification in ' + $tuple[0] +
            ': replace p.Filter with ' + $tuple[1] + '.Filter'))
    }
}

# 2026-10-19 Gradle unit-test Kotlin type regressions.
# LocalDailyMixEngine.make's third input is List<VideoPreview>, NOT
# List<List<VideoPreview>>. Guard the two formerly broken call sites.
$dailyMixTestPath = Join-Path $sourceRoot 'test\java\com\nightwave\player\v2\recommendation\LocalDailyMixEngineTest.kt'
$dailyMixPolicyPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\v2\recommendation\LocalDailyMixEngine.kt'
if ((Test-Path -LiteralPath $dailyMixTestPath -PathType Leaf) -and
    (Test-Path -LiteralPath $dailyMixPolicyPath -PathType Leaf)) {
    $dailyMixTest = [System.IO.File]::ReadAllText($dailyMixTestPath)
    $dailyMixPolicy = [System.IO.File]::ReadAllText($dailyMixPolicyPath)
    if (-not $dailyMixPolicy.Contains('playlistTracks: List<VideoPreview>')) {
        $issues.Add('Daily Mix playlistTracks parameter type changed; review its tests.')
    }
    if ($dailyMixTest.Contains('listOf(invalid), listOf(valid, valid), 333L')) {
        $issues.Add('Daily Mix unit test passes List<List<VideoPreview>>: flatten repeated valid songs.')
    }
    if ($dailyMixTest.Contains('LocalDailyMixEngine.make(favorites, recent, listOf(favorites), 500L)')) {
        $issues.Add('Daily Mix unit test passes List<MutableList<VideoPreview>> instead of the flat favorites list.')
    }
    if (-not $dailyMixTest.Contains('listOf(invalid), valid + valid, 333L') -or
        -not $dailyMixTest.Contains('LocalDailyMixEngine.make(favorites, recent, favorites, 500L)')) {
        $issues.Add('Daily Mix duplicate-source and immutability test call contracts are missing.')
    }
}

# Compiler regressions confirmed by the 2026-10-24 LOCAL Gradle log.
# Reject malformed two-backslash Kotlin character literals and invalid OkHttp
# Dns interface construction before the next full 40+ task Gradle build.
$offlineAudioSourcePath = Join-Path $sourceRoot 'main\java\com\nightwave\player\offline\OfflineAudioLibrary.kt'
if (Test-Path -LiteralPath $offlineAudioSourcePath -PathType Leaf) {
    $offlineAudioSource = [System.IO.File]::ReadAllText($offlineAudioSourcePath)
    if (-not $offlineAudioSource.Contains("replace('\n', ' ')") -or
        -not $offlineAudioSource.Contains("replace('\r', ' ')")) {
        $issues.Add('OfflineAudioLibrary must replace newline and carriage-return using valid Kotlin char literals.')
    }
} else {
    $issues.Add('OfflineAudioLibrary.kt is missing.')
}
$permittedDownloaderPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\offline\PermittedAudioDownloader.kt'
if (Test-Path -LiteralPath $permittedDownloaderPath -PathType Leaf) {
    $permittedDownloaderSource = [System.IO.File]::ReadAllText($permittedDownloaderPath)
    if ($permittedDownloaderSource.Contains('.dns(Dns {') -or
        -not $permittedDownloaderSource.Contains('.dns(object : Dns {') -or
        -not $permittedDownloaderSource.Contains('override fun lookup(hostname: String): List<InetAddress>') -or
        -not $permittedDownloaderSource.Contains('resolved.all(::isPublicAddress)')) {
        $issues.Add('PermittedAudioDownloader must implement OkHttp Dns.lookup and preserve unsafe-address filtering.')
    }
} else {
    $issues.Add('PermittedAudioDownloader.kt is missing.')
}

# NW43: Search is a selectable single-song source, not an implicit 20-item
# queue. Full playlist/mix Play commands must remain separate from this guard.
$studioPath = Join-Path $sourceRoot 'main\java\com\nightwave\player\v2\ui\NightWaveStudioActivity.kt'
$queuePath = Join-Path $sourceRoot 'main\java\com\nightwave\player\playback\PlaybackQueue.kt'
$servicePath = Join-Path $sourceRoot 'main\java\com\nightwave\player\playback\PlaybackService.kt'
if ((Test-Path -LiteralPath $studioPath -PathType Leaf) -and
    (Test-Path -LiteralPath $queuePath -PathType Leaf) -and
    (Test-Path -LiteralPath $servicePath -PathType Leaf)) {
    $studio = [System.IO.File]::ReadAllText($studioPath)
    $queueSource = [System.IO.File]::ReadAllText($queuePath)
    $service = [System.IO.File]::ReadAllText($servicePath)
    $start = $studio.IndexOf('private fun StudioSearch(')
    $finish = $studio.IndexOf('private fun StudioLibrary(')
    if ($start -lt 0 -or $finish -le $start) {
        $issues.Add('NW43 Search and Library function boundaries missing.')
    } else {
        $searchContent = $studio.Substring($start, $finish - $start)
        foreach ($bad in @('onPlay(song, state.videos)', 'onPlay(song, savedMatches)',
            'onPlay(song, savedTracks)', 'onPlay(song, selectedCreatorGroup.tracks)')) {
            if ($searchContent.Contains($bad)) {
                $issues.Add(('NW43 Search must not autoqueue search results: ' + $bad))
            }
        }
        $savedPaths = $searchContent.Split(@('onPlay(song, listOf(song))'),
            [System.StringSplitOptions]::None).Length - 1
        $singlePublic = $searchContent.Contains('onPlay(selected, listOf(selected))')
        $publicPathCount = $searchContent.Split(@('playSinglePublic(song)'),
            [System.StringSplitOptions]::None).Length - 1
        if ($savedPaths -ne 3 -or -not $singlePublic -or $publicPathCount -ne 2) {
            $issues.Add('NW44: all grouped Search taps must remain single-song.')
        }
    }
    if (-not $queueSource.Contains('fun insertNext(entry: QueueEntry): Boolean') -or
        -not $service.Contains('ACTION_ENQUEUE, ACTION_PLAY_NEXT ->')) {
        $issues.Add('NW43 explicit Play Next service/queue wiring missing.')
    }
} else {
    $issues.Add('NW43 queue, service or Studio source missing.')
}

$gradlePath = Join-Path $root 'app\build.gradle.kts'
if (-not (Test-Path -LiteralPath $gradlePath)) {
    $issues.Add('Missing app/build.gradle.kts.')
} else {
    $gradle = [System.IO.File]::ReadAllText($gradlePath)
    foreach ($marker in @(
        'applicationId = "com.nightwave.player"',
        'versionCode = 35',
        'versionName = "2.0.0-alpha5s"',
        'id("org.jetbrains.kotlin.plugin.compose")',
        'androidTestImplementation("androidx.compose.ui:ui-test-junit4")'
    )) {
        if (-not $gradle.Contains($marker)) { $issues.Add("Build identity/dependency marker missing: $marker") }
    }
}
if ($issues.Count -gt 0) {
    Write-Host 'NightWave preflight found source/build contract errors:' -ForegroundColor Red
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    throw ('Kotlin preflight failed with {0} error(s).' -f $issues.Count)
}
Write-Host ('PASS: Kotlin source/import preflight ({0} files); no outdated Compose test imports.' -f $files.Count) -ForegroundColor Green
Write-Host 'Android Gradle compilation and instrumentation sources still require real build verification.'
exit 0
