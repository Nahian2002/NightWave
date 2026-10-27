# NightWave 6V6A lyrics source gate. Read-only PowerShell 5.1.
# Not an Android compiler, live Google lyrics lookup or device test.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$issues = New-Object 'System.Collections.Generic.List[string]'
function FileText([string]$rel) {
    $p = Join-Path $root ($rel -replace '/', '\')
    if (-not (Test-Path -LiteralPath $p -PathType Leaf)) {
        $script:issues.Add("Missing file: $rel")
        return ''
    }
    return [IO.File]::ReadAllText($p, [Text.Encoding]::UTF8)
}
function Need([string]$src, [string]$token, [string]$description) {
    if (-not $src.Contains($token)) { $script:issues.Add($description) }
}
function TestMethods([string]$path, [int]$minimum) {
    $src = FileText $path
    $count = ([regex]::Matches($src, '@Test\s+fun\s+')).Count
    if ($count -lt $minimum) {
        $script:issues.Add("$path has $count tests; expected at least $minimum")
    }
}
$p = 'app/src/main/java/com/nightwave/player/v2/'
$sync = FileText ($p + 'lyrics/LyricsSyncPolicy.kt')
$offset = FileText ($p + 'lyrics/PrivateLyricsOffsetStore.kt')
$code = FileText ($p + 'lyrics/TimedLyrics.kt')
$store = FileText ($p + 'lyrics/PrivateLyricsStore.kt')
$ui = FileText ($p + 'ui/StudioPlayerScreens.kt')
$activity = FileText ($p + 'ui/NightWaveStudioActivity.kt')
$build = FileText 'tools/build-windows.ps1'
$workflow = FileText '.github/workflows/android.yml'

Need $sync 'MAX_OFFSET_MS = 10_000' 'Lyrics offset limit missing'
Need $sync 'STEP_MS = 250' 'Lyrics offset step missing'
Need $sync 'fun seekPosition(' 'Timestamp seek conversion missing'
Need $sync 'fun activeIndex(' 'Line-follow timing missing'
Need $code 'const val MAX_BYTES = 64 * 1024' 'Imported LRC size protection missing'
Need $code 'const val MAX_LINES = 1200' 'Imported LRC line limit missing'
Need $store 'TimedLyricsCodec.parse(bytes)' 'Imported LRC parser validation missing'
Need $offset 'nightwave_private_lrc_offset_v1' 'Lyric offsets not isolated from existing LRC'
Need $offset 'offline.find(id) != null' 'Lyric offset not tied to imported track'
Need $offset 'LyricsSyncPolicy.validOffset(offsetMs)' 'Invalid offsets could be persisted'
Need $ui 'var lyricsImmersive by remember(state.videoUrl)' 'Full-screen per-track lyrics mode missing'
Need $ui 'rememberLazyListState()' 'Lyric line scroll state missing'
Need $ui 'LyricsSyncPolicy.activeIndex(' 'Active line does not include offset'
Need $ui 'LyricsSyncPolicy.seekPosition(' 'Tap-to-seek does not include offset'
Need $ui 'onLyricsOffsetChange(' 'Lyric timing controls missing'
# NW89: bounded lyric scroll animation policy handles reduced motion AND large seeks.
Need $ui 'LyricsReadingPolicy.shouldAnimateScroll(' 'Reduced-motion and long-seek lyric scroll policy missing'
Need $ui 'lyricListState.firstVisibleItemIndex, target, reduceMotion' 'Lyrics scroll policy must receive reducedMotion'
Need $ui 'else lyricListState.scrollToItem(target)' 'No non-animated lyric scroll fallback'
Need $activity 'lyricsOffsetStore.read(id)' 'Saved lyric offset not loaded'
Need $activity 'lyricsOffsetStore.save(id, desiredOffset)' 'Adjusted lyric offset not saved'
Need $activity 'playback.state.videoUrl == current' 'Old track callback fence missing'
Need $build 'check-phase6v6a-integration.ps1' '6V6A Windows build gate missing'
Need $build ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Actual Gradle tasks not configured'
Need $workflow 'python3 tools/verify_v2_phase6v6a.py' 'GitHub lyrics audit missing'
TestMethods 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsSyncPolicyTest.kt' 10
TestMethods 'app/src/test/java/com/nightwave/player/v2/lyrics/TimedLyricsCodecTest.kt' 10

if ($issues.Count -gt 0) {
    Write-Host "FAIL: 6V6A source checks found $($issues.Count) issue(s)" -ForegroundColor Red
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V6A private imported-LRC timing, full-screen UI and regression files wired.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Kotlin, Gradle JVM tests, Android UI, signed APK, and phone behavior.' -ForegroundColor Yellow
exit 0
