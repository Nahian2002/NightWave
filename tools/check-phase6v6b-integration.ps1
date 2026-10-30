# NightWave Phase 6V6B: safe, local LRC import-review source audit.
# Read-only PowerShell 5.1: not a Gradle compile, runtime/phone or Google test.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$issues = New-Object 'System.Collections.Generic.List[string]'
function Source([string]$rel) {
    $path = Join-Path $root ($rel -replace '/', '\')
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $script:issues.Add("Missing file: $rel")
        return ''
    }
    return [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
}
function Need([string]$body, [string]$piece, [string]$why) {
    if (-not $body.Contains($piece)) { $script:issues.Add($why) }
}
function TestCount([string]$rel, [int]$min) {
    $body = Source $rel
    $number = ([regex]::Matches($body, '@Test\s+fun\s+')).Count
    if ($number -lt $min) { $script:issues.Add("$rel has $number tests; expected $min") }
}
$p = 'app/src/main/java/com/nightwave/player/v2/'
$analyzer = Source ($p + 'lyrics/LyricsImportReview.kt')
$codec = Source ($p + 'lyrics/TimedLyrics.kt')
$store = Source ($p + 'lyrics/PrivateLyricsStore.kt')
$activity = Source ($p + 'ui/NightWaveStudioActivity.kt')
$screen = Source ($p + 'ui/StudioPlayerScreens.kt')
$build = Source 'tools/build-windows.ps1'
$ci = Source '.github/workflows/android.yml'

Need $analyzer 'TimedLyricsCodec.parse(bytes)' 'Imported LRC must use strict existing parser'
Need $analyzer 'fun compare(' 'Advisory title match missing'
Need $analyzer 'fun failureHint(' 'Safe actionable errors missing'
Need $analyzer 'ignoredTimestampRows' 'Malformed timestamp diagnostics missing'
Need $analyzer 'repeatedCues' 'Duplicate lyric diagnostics missing'
Need $analyzer 'durationWarning' 'Duration discrepancy warning missing'
Need $codec 'const val MAX_BYTES = 64 * 1024' 'Input-size bound missing'
Need $codec 'const val MAX_LINES = 1200' 'Cue-count bound missing'
Need $store 'fun fingerprint(id: String): String?' 'Existing lyric fingerprint missing'
Need $store 'MessageDigest.getInstance("SHA-256")' 'Expected content fingerprint not cryptographic'
Need $store 'fun saveReviewed(id: String, bytes: ByteArray, expectedFingerprint: String?): Boolean' 'Reviewed replacement method missing'
Need $store 'if (fingerprint(id) != expectedFingerprint) return false' 'Stale lyric preview must fail closed'
Need $activity 'LyricsImportReviewPolicy.inspect(' 'Import diagnostics not wired'
Need $activity 'boundLyrics.fingerprint(id)' 'Existing lyric fingerprint not captured'
Need $activity 'showLyricsImportReview(' 'Preview dialog not wired'
Need $activity 'boundLyrics.saveReviewed(' 'Confirmed commit guard missing'
Need $activity 'requestEpoch != lyricsImportEpoch' 'Stale file picker callback fence missing'
Need $activity 'setPositiveButton(if (replacing) "Replace LRC" else "Import LRC")' 'Explicit replacement confirmation missing'
Need $activity 'LyricsImportReviewPolicy.failureHint(error)' 'Safe import errors not displayed'
Need $screen 'R.string.nw141_how_to_import_synchronized_lyrics_511ec' 'LRC guide missing'
Need $screen 'R.string.nw141_lyrics_owned' 'Song identity warning missing'
Need $screen 'R.string.nw141_open_full_screen_synchronized_lyrics_dd54c' 'Previous synced lyrics UI missing'
Need $build 'check-phase6v6b-integration.ps1' '6V6B build gate not wired'
Need $build ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Real Gradle checks missing'
Need $ci 'python3 tools/verify_v2_phase6v6b.py' 'GitHub LRC gate missing'
TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsImportReviewPolicyTest.kt' 12
TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/TimedLyricsCodecTest.kt' 10
TestCount 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsSyncPolicyTest.kt' 11

if ($issues.Count -gt 0) {
    Write-Host "FAIL: NightWave 6V6B import source gate found $($issues.Count) gap(s)" -ForegroundColor Red
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V6B LRC review, duplicate/timestamp diagnostics, explicit safe replace and tests wired.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Android Kotlin compilation, unit tests, UI rendering and signer.' -ForegroundColor Yellow
exit 0
