# NightWave Phase 6V6D: read-only local export and editor lifecycle preflight.
# PowerShell 5.1 source gate. NOT an Android compile or APK certification.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$errors = New-Object 'System.Collections.Generic.List[string]'
function Src([string]$rel) {
    $file = Join-Path $root ($rel -replace '/', '\')
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        $script:errors.Add("Missing file: $rel")
        return ''
    }
    return [IO.File]::ReadAllText($file, [Text.Encoding]::UTF8)
}
function Need([string]$source, [string]$token, [string]$reason) {
    if (-not $source.Contains($token)) { $script:errors.Add($reason) }
}
function Tests([string]$path, [int]$minimum) {
    $n = ([regex]::Matches((Src $path), '@Test\s+fun\s+')).Count
    if ($n -lt $minimum) {
        $script:errors.Add(('{0}: {1} tests found, {2} required' -f $path, $n, $minimum))
    }
}
$p = 'app/src/main/java/com/nightwave/player/v2/'
$export = Src ($p + 'lyrics/LyricsExportPolicy.kt')
$nav = Src ($p + 'lyrics/LyricsEditorNavigationPolicy.kt')
$ui = Src ($p + 'ui/NightWaveLyricsEditorDialog.kt')
$activity = Src ($p + 'ui/NightWaveStudioActivity.kt')
$gradle = Src 'app/build.gradle.kts'
$build = Src 'tools/build-windows.ps1'
$ci = Src '.github/workflows/android.yml'
Need $export 'LyricsEditorPolicy.serialize(current)' 'Export must serialize through strict LRC policy'
Need $export 'require(verified.cues == current.cues)' 'Export parser round-trip guard missing'
Need $export 'fun suggestedFileName(' 'Sanitized SAF filename missing'
Need $nav 'fun followOrKeep(' 'Offset-aware current-line navigation missing'
Need $nav 'fun afterEdit(' 'Reordered line navigation missing'
Need $ui 'onExport: () -> Unit' 'Export action not declared in Compose'
Need $ui 'onClick = onExport' 'Export button not connected'
Need $ui 'Export unsaved draft .lrc' 'Unsaved export label missing'
Need $ui 'Modifier.heightIn(min = 48.dp)' 'Accessible touch targets missing'
Need $ui 'stateDescription =' 'Screen-reader row selection missing'
Need $ui 'LyricsEditorNavigationPolicy.afterEdit(' 'Editor focus after reorder missing'
Need $activity 'internal class LyricsEditorRetainedState : ViewModel()' 'Rotation-retained state missing'
Need $activity 'ViewModelProvider(this)[LyricsEditorRetainedState::class.java]' 'Retained state not wired'
Need $activity 'private val exportLyricsFile = registerForActivityResult(' 'SAF export contract missing'
Need $activity 'ActivityResultContracts.CreateDocument("text/plain")' 'User-chosen export file missing'
Need $activity 'pending.requestId != lyricsEditorEpoch' 'Stale export callback fencing missing'
Need $activity 'playback.state.videoUrl != "offline:" + pending.songId' 'Wrong song export fencing missing'
Need $activity 'onExport = { exportLyricsEditorDraft() }' 'Lyrics editor export callback not connected'
Need $activity 'lyricsStore.saveReviewed(' 'Existing safe LRC save lost'
Need $activity 'current.isNotBlank()' 'Temporary blank song rotation handling missing'
Need $gradle 'lifecycle-viewmodel-ktx:2.8.7' 'Android ViewModel dependency missing'
Need $build 'check-phase6v6d-integration.ps1' '6V6D Windows preflight missing'
Need $build ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Real Gradle gate missing'
Need $ci 'python3 tools/verify_v2_phase6v6d.py' 'CI 6V6D check missing'
Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsExportPolicyTest.kt' 10
Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorNavigationPolicyTest.kt' 10
Tests 'app/src/androidTest/java/com/nightwave/player/v2/ui/LyricsEditorRetainedStateInstrumentedTest.kt' 2
Tests 'app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorPolicyTest.kt' 14

if ($errors.Count -gt 0) {
    Write-Host ("FAIL: 6V6D integration preflight: {0} issue(s)." -f $errors.Count) -ForegroundColor Red
    $errors | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V6D SAF export, accessible LRC editor, ViewModel rotation state and test declarations wired.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Kotlin/Gradle, executed JVM tests, Android UI and APK signer.' -ForegroundColor Yellow
exit 0
