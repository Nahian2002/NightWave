# NightWave 6V6F: fail-closed build diagnostics and lyrics source gate.
# Read-only PowerShell 5.1, no Gradle compilation or device operations.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$problems = New-Object 'System.Collections.Generic.List[string]'
function Source([string]$relative) {
    $path = Join-Path $root ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $script:problems.Add("Missing file: $relative")
        return ''
    }
    return [IO.File]::ReadAllText($path, [Text.Encoding]::UTF8)
}
function Need([string]$source, [string]$token, [string]$description) {
    if (-not $source.Contains($token)) { $script:problems.Add($description) }
}
function TestCount([string]$relative, [int]$minimum) {
    $number = ([regex]::Matches((Source $relative), '@Test\s+fun\s+')).Count
    if ($number -lt $minimum) {
        $script:problems.Add(('{0}: expected >= {1} tests, found {2}' -f
            $relative, $minimum, $number))
    }
}
$p = 'app/src/main/java/com/nightwave/player/v2/'
$lrc = Source ($p + 'lyrics/TimedLyrics.kt')
$store = Source ($p + 'lyrics/PrivateLyricsStore.kt')
$editor = Source ($p + 'lyrics/LyricsEditorPolicy.kt')
$search = Source ($p + 'lyrics/LyricsEditorSearchPolicy.kt')
$export = Source ($p + 'lyrics/LyricsExportPolicy.kt')
$activity = Source ($p + 'ui/NightWaveStudioActivity.kt')
$dialog = Source ($p + 'ui/NightWaveLyricsEditorDialog.kt')
$triage = Source 'tools/triage_github_actions.py'
$smoke = Source 'tools/smoke-lyrics-kotlin.ps1'
$builder = Source 'tools/build-windows.ps1'
$workflow = Source '.github/workflows/android.yml'

Need $lrc 'MAX_BYTES = 64 * 1024' 'Strict imported lyrics size cap is missing'
Need $store 'if (fingerprint(id) != expectedFingerprint) return false' 'Concurrent lyric save is unguarded'
Need $editor 'require(parsed.cues == ordered)' 'Edited lyric parser round-trip protection missing'
Need $search 'MAX_QUERY_LENGTH = 80' 'Search size limit missing'
Need $search 'fun nearest(' 'Safe wrapped lyric navigation missing'
Need $export 'require(verified.cues == current.cues)' 'LRC export could silently alter lyric cues'
Need $dialog 'lyrics-save-edits' 'Editor explicit Save missing'
Need $dialog 'lyrics-export-lrc' 'User-selected LRC export missing'
Need $activity 'boundLyrics.saveReviewed(' 'Private LRC save not fingerprint checked'
Need $activity 'pending.requestId != lyricsEditorEpoch' 'Old export requests not fenced'
Need $activity 'activeEditor?.requestId == pending.requestId' 'Export writer must recheck the live editor session'
Need $triage 'RUNNER_UNASSIGNED' 'Runner allocation triage is missing'
Need $triage 'NOT_ATTEMPTED' 'Runner-less CI failure must not masquerade as Kotlin failure'
Need $triage 'def self_test()' 'Runner triage synthetic regression missing'
Need $smoke 'Get-Command ' 'Optional kotlinc smoke entry is missing'
Need $smoke 'LyricsEditorSearchPolicy.kt' 'Search Kotlin source missing in smoke check'
Need $smoke 'LyricsImportReview.kt' 'Import Kotlin source missing in smoke check'
Need $smoke 'NOT VERIFIED: Android Compose/Gradle' 'Compiler smoke is wrongly claiming Android certification'
Need $builder 'check-phase6v6f-integration.ps1' '6V6F Windows gate is not registered'
Need $builder ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Actual Gradle verification missing'
Need $builder 'verify-apk-signature.ps1' 'APK signature verification missing'
Need $workflow 'python3 tools/triage_github_actions.py --self-test' 'CI triage self-test missing'
Need $workflow 'python3 tools/verify_v2_phase6v6f.py --self-test' 'CI integration gate missing'

TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorSearchPolicyTest.kt' 16
TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorPolicyTest.kt' 14
TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsSyncPolicyTest.kt' 11
TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsImportReviewPolicyTest.kt' 14
TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsExportPolicyTest.kt' 10
TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorNavigationPolicyTest.kt' 10
TestCount 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveLyricsSearchUiInstrumentedTest.kt' 3
TestCount 'app/src/androidTest/java/com/nightwave/player/v2/ui/LyricsEditorRetainedStateInstrumentedTest.kt' 2

if ($problems.Count -gt 0) {
    Write-Host ("FAIL: 6V6F integration gate: {0} issue(s)" -f $problems.Count) -ForegroundColor Red
    $problems | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: lyrics A-F source invariants and honest CI diagnostics wired.' -ForegroundColor Green
Write-Host 'NOT VERIFIED: real Android compiler/tests, emulator, Drive or signed APK.' -ForegroundColor Yellow
exit 0
