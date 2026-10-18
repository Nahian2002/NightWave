# NightWave Phase 6V6C: in-app LRC editor source wiring gate.
# PowerShell 5.1, read-only: does not run Android, overwrite files or sign APKs.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$errors = New-Object 'System.Collections.Generic.List[string]'
function Source([string]$relative) {
    $file = Join-Path $root ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        $script:errors.Add("Missing: $relative")
        return ''
    }
    [IO.File]::ReadAllText($file, [Text.Encoding]::UTF8)
}
function Require([string]$source, [string]$token, [string]$context) {
    if (-not $source.Contains($token)) { $script:errors.Add($context) }
}
function TestMethods([string]$path, [int]$minimum) {
    $source = Source $path
    $count = ([regex]::Matches($source, '@Test\s+fun\s+')).Count
    if ($count -lt $minimum) {
        $script:errors.Add("$path: only $count tests, expected $minimum")
    }
}
$p = 'app/src/main/java/com/nightwave/player/v2/'
$policy = Source ($p + 'lyrics/LyricsEditorPolicy.kt')
$store = Source ($p + 'lyrics/PrivateLyricsStore.kt')
$dialog = Source ($p + 'ui/NightWaveLyricsEditorDialog.kt')
$activity = Source ($p + 'ui/NightWaveStudioActivity.kt')
$screen = Source ($p + 'ui/StudioPlayerScreens.kt')
$build = Source 'tools/build-windows.ps1'
$ci = Source '.github/workflows/android.yml'

Require $policy 'data class LyricsEditorDraft(' 'Unsaved LRC draft model missing'
Require $policy 'MAX_UNDO = 15' 'Undo history cap missing'
Require $policy 'fun updateText(' 'LRC line text editing missing'
Require $policy 'fun updateTime(' 'LRC timecode editing missing'
Require $policy 'fun nudge(' '250ms timestamp corrections missing'
Require $policy 'fun add(' 'Add line feature missing'
Require $policy 'fun delete(' 'Remove line feature missing'
Require $policy 'TimedLyricsCodec.parse(bytes)' 'Strict LRC parser round-trip missing'
Require $policy 'require(parsed.cues == ordered)' 'Parser round-trip equality missing'
Require $policy 'require(bytes.size in 1..TimedLyricsCodec.MAX_BYTES)' 'Edited LRC size bound missing'
Require $store 'if (fingerprint(id) != expectedFingerprint) return false' 'Lost-update guard missing'
Require $dialog 'fun NightWaveLyricsEditorDialog(' 'Editing dialog missing'
Require $dialog 'LazyColumn(' 'Scrollable lyric rows missing'
Require $dialog 'LyricsEditorPolicy.updateText(' 'Editor text action not wired'
Require $dialog 'LyricsEditorPolicy.nudge(' 'Editor nudge action not wired'
Require $dialog 'LyricsEditorPolicy.delete(' 'Editor remove action not wired'
Require $dialog 'Discard unsaved lyric edits?' 'Explicit discard warning missing'
Require $dialog 'lyrics-save-edits' 'Explicit Save action missing'
Require $activity 'private data class LyricsEditorSession(' 'Per-song editing session missing'
Require $activity 'LyricsEditorPolicy.begin(imported)' 'Import snapshot not captured'
Require $activity 'lyricsStore.saveReviewed(' 'Verified reviewed save not wired'
Require $activity 'editor.expectedFingerprint' 'Concurrent LRC protection missing'
Require $activity 'editor.requestId == lyricsEditorEpoch' 'Old save generation not guarded'
Require $activity 'fun undoLyricsEditorDraft()' 'Undo not implemented'
Require $activity 'fun discardLyricsEditorDraft()' 'No discard path'
Require $screen 'onEditLyrics: () -> Unit = {}' 'Now Playing editor link missing'
Require $screen 'Edit lyric lines & timestamps' 'Editor entry button missing'
Require $build 'check-phase6v6c-integration.ps1' '6V6C Windows source gate not connected'
Require $build ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Actual Android build/test tasks missing'
Require $ci 'python3 tools/verify_v2_phase6v6c.py' 'GitHub source check not connected'

TestMethods 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorPolicyTest.kt' 14
TestMethods 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsImportReviewPolicyTest.kt' 14
TestMethods 'app/src/test/java/com/nightwave/player/v2/lyrics/TimedLyricsCodecTest.kt' 10

if ($errors.Count -gt 0) {
    Write-Host ("FAIL: 6V6C source preflight detected {0} issue(s)" -f $errors.Count) -ForegroundColor Red
    $errors | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V6C local lyrics editor, reviewed save, navigation and undo regression hooks present.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Gradle/JVM tests, Compose UI compilation, emulator and APK signing.' -ForegroundColor Yellow
exit 0
