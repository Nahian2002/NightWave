# NightWave 6V5B: local transfer journal + checkpoint readiness integration gate.
# PowerShell 5.1, read-only, no OAuth, music files, ADB, or APK access.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$issues = New-Object 'System.Collections.Generic.List[string]'
function ReadSource([string]$relative) {
    $path = Join-Path $root ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $script:issues.Add("Missing: $relative")
        return ''
    }
    return [System.IO.File]::ReadAllText($path, [Text.Encoding]::UTF8)
}
function Require([string]$body, [string]$part, [string]$explanation) {
    if (-not $body.Contains($part)) { $script:issues.Add("Missing $explanation") }
}
function TestCount([string]$source, [int]$minimum, [string]$label) {
    $amount = ([regex]::Matches($source, '@Test\s+fun\s+')).Count
    if ($amount -lt $minimum) { $script:issues.Add("$label : $amount tests; expected $minimum") }
}
$b = 'app/src/main/java/com/nightwave/player/v2/locker/'
$j = ReadSource ($b + 'LockerTransferHistoryStore.kt')
$p = ReadSource ($b + 'LockerTransferHistoryPolicy.kt')
$c = ReadSource ($b + 'LockerDownloadCheckpointStore.kt')
$r = ReadSource ($b + 'LockerCheckpointReadiness.kt')
$ui = ReadSource ($b + 'NightWaveLockerActivity.kt')
$testJ = ReadSource 'app/src/test/java/com/nightwave/player/v2/locker/LockerTransferHistoryPolicyTest.kt'
$testR = ReadSource 'app/src/test/java/com/nightwave/player/v2/locker/LockerCheckpointReadinessTest.kt'
$testA = ReadSource 'app/src/androidTest/java/com/nightwave/player/v2/locker/LockerTransferHistoryStoreInstrumentedTest.kt'
$build = ReadSource 'tools/build-windows.ps1'
$ci = ReadSource '.github/workflows/android.yml'

Require $j 'AtomicFile(file)' 'private atomic journal'
Require $j 'fun read(): LockerTransferHistorySnapshot' 'non-destructive journal read'
Require $j 'fun clear()' 'explicit local journal clear'
Require $j 'require(!previous.unreadable)' 'corrupt journal fail-closed'
Require $j 'LockerTransferHistoryPolicy.capped(' 'bounded history'
Require $p 'MAX_ENTRIES = 30' 'history entry cap'
Require $p 'Unconfirmed' 'unknown crash state is not success'
Require $r 'HASH_CHECK_PENDING' 'full bytes still require final hash'
Require $r 'MISSING_DATA' 'lost staged bytes detected'
Require $c 'fun inspect(connectedAccount: String): LockerCheckpointPreview' 'read-only readiness inspection'
Require $c 'Files.isSymbolicLink(' 'unsafe staged-file rejection'
Require $ui 'history.start(LockerTransferKind.UPLOAD' 'upload journal attempt'
Require $ui 'history.start(LockerTransferKind.DOWNLOAD' 'download journal attempt'
Require $ui 'history.finish(journalId, resultState)' 'final/uncertain history recording'
Require $ui 'checkpoints.inspect(state.account())' 'resume precondition'
Require $ui 'if (!readiness.canResume || saved == null)' 'unsafe resume prevention'
Require $ui 'history.clear()' 'explicit history-only clear'
Require $ui 'renderTransferHistory()' 'history rendered in vault UI'
Require $build 'check-phase6v5b-integration.ps1' 'Windows build gate'
Require $build ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'real Gradle gate'
Require $ci 'python3 tools/verify_v2_phase6v5b.py' 'GitHub integration source gate'
TestCount $testJ 8 'Transfer policy'
TestCount $testR 10 'Checkpoint readiness'
TestCount $testA 5 'Isolated Android history persistence'

if ($issues.Count -gt 0) {
    Write-Host ("FAIL: 6V5B integration gate found {0} error(s)" -f $issues.Count) -ForegroundColor Red
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V5B transfer journal, read-only recovery checks and regression suites present.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Kotlin/Gradle, JVM tests, Android test compile, OAuth and APK/device behavior.' -ForegroundColor Yellow
exit 0
