# NightWave Phase 6V6E: cumulative synchronized lyrics editor search gate.
# PowerShell 5.1, read-only. Does not compile Kotlin, modify music, or build APKs.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$problems = New-Object 'System.Collections.Generic.List[string]'

function Source([string]$relative) {
    $file = Join-Path $root ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        $script:problems.Add("Missing file: $relative")
        return ''
    }
    [IO.File]::ReadAllText($file, [Text.Encoding]::UTF8)
}
function Need([string]$source, [string]$token, [string]$reason) {
    if (-not $source.Contains($token)) { $script:problems.Add($reason) }
}
function Tests([string]$relative, [int]$minimum) {
    $found = ([regex]::Matches((Source $relative), '@Test\s+fun\s+')).Count
    if ($found -lt $minimum) {
        $script:problems.Add(('{0}: expected {1} tests, found {2}' -f
            $relative, $minimum, $found))
    }
}

$p = 'app/src/main/java/com/nightwave/player/v2/'
$lrc = Source ($p + 'lyrics/TimedLyrics.kt')
$sync = Source ($p + 'lyrics/LyricsSyncPolicy.kt')
$review = Source ($p + 'lyrics/LyricsImportReview.kt')
$store = Source ($p + 'lyrics/PrivateLyricsStore.kt')
$edit = Source ($p + 'lyrics/LyricsEditorPolicy.kt')
$export = Source ($p + 'lyrics/LyricsExportPolicy.kt')
$nav = Source ($p + 'lyrics/LyricsEditorNavigationPolicy.kt')
$search = Source ($p + 'lyrics/LyricsEditorSearchPolicy.kt')
$ui = Source ($p + 'ui/NightWaveLyricsEditorDialog.kt')
$activity = Source ($p + 'ui/NightWaveStudioActivity.kt')
$builder = Source 'tools/build-windows.ps1'
$ci = Source '.github/workflows/android.yml'

Need $lrc 'MAX_BYTES = 64 * 1024' 'Canonical LRC size cap removed'
Need $sync 'fun activeIndex(' '6V6A lyric timing removed'
Need $review 'TimedLyricsCodec.parse(bytes)' '6V6B strict import review removed'
Need $store 'if (fingerprint(id) != expectedFingerprint) return false' '6V6B replacement race check removed'
Need $edit 'require(parsed.cues == ordered)' '6V6C strict edited cue validation removed'
Need $export 'require(verified.cues == current.cues)' '6V6D strict export verification removed'
Need $nav 'fun afterEdit(' '6V6D stable selection after edit missing'
Need $search 'MAX_QUERY_LENGTH = 80' 'Search input safety limit missing'
Need $search 'fun matches(' 'Local lyric text search missing'
Need $search 'fun nearest(' 'Previous/next match wrapping missing'
Need $search 'Normalizer.normalize(value, Normalizer.Form.NFC)' 'Unicode lyric search normalization missing'
Need $ui 'rememberSaveable(songId)' 'Search and selection not isolated by offline song ID'
Need $ui 'lyrics-search-toggle' 'Collapsible search entry missing'
Need $ui 'lyrics-search-input' 'Search input not connected'
Need $ui 'lyrics-match-previous' 'Previous matching cue button missing'
Need $ui 'lyrics-match-next' 'Next matching cue button missing'
Need $ui 'LyricsEditorSearchPolicy.matches(draft, effectiveQuery)' 'Draft-based live search missing'
Need $ui 'LyricsEditorSearchPolicy.nearest(' 'Search navigation not driven by safe cue policy'
Need $ui 'val searchHit = index in searchMatchSet' 'Search result highlighting missing'
Need $ui 'lyrics-save-edits' 'Explicit private lyric save missing'
Need $ui 'lyrics-export-lrc' 'Separate LRC export missing'
Need $activity 'songId = session.songId' 'Song identity is not passed to editor'
Need $activity 'boundLyrics.saveReviewed(' 'Fingerprint-guarded save missing'
Need $activity 'pending.requestId != lyricsEditorEpoch' 'Export session fencing missing'
Need $builder 'check-phase6v6e-integration.ps1' 'Windows cumulative gate not registered'
Need $builder ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Android compilation and JUnit gates missing'
Need $ci 'python3 tools/verify_v2_phase6v6e.py' 'GitHub lyrics integration audit missing'

Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorSearchPolicyTest.kt' 16
Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsSyncPolicyTest.kt' 11
Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsImportReviewPolicyTest.kt' 14
Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorPolicyTest.kt' 14
Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsExportPolicyTest.kt' 10
Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorNavigationPolicyTest.kt' 10
Tests 'app/src/androidTest/java/com/nightwave/player/v2/ui/LyricsEditorRetainedStateInstrumentedTest.kt' 2
Tests 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveLyricsSearchUiInstrumentedTest.kt' 3

if ($problems.Count -gt 0) {
    Write-Host ('FAIL: 6V6E source integration found {0} issue(s).' -f $problems.Count) -ForegroundColor Red
    $problems | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V6A-E local lyric search, safe import/edit/export and Android lifecycle hooks present.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Kotlin/JVM test execution, Compose, device, SAF, signed APK.' -ForegroundColor Yellow
exit 0
