# NightWave 6V7A read-only premium chrome source gate (PowerShell 5.1).
# This cannot compile Kotlin or certify a device release.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$problems = New-Object 'System.Collections.Generic.List[string]'
function Read-Source([string]$relative) {
    $path = Join-Path $root ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $script:problems.Add("Missing: $relative")
        return ''
    }
    return [IO.File]::ReadAllText($path, [Text.Encoding]::UTF8)
}
function Need([string]$body, [string]$term, [string]$reason) {
    if (-not $body.Contains($term)) { $script:problems.Add($reason) }
}
function TestCount([string]$path, [int]$minimum) {
    $total = ([regex]::Matches((Read-Source $path), '@Test\s+fun\s+')).Count
    if ($total -lt $minimum) {
        $script:problems.Add(('{0}: expected {1}, found {2} test cases' -f
            $path, $minimum, $total))
    }
}
$prefix = 'app/src/main/java/com/nightwave/player/v2/ui/'
$policy = Read-Source ($prefix + 'NightWaveChromePolicy.kt')
$chrome = Read-Source ($prefix + 'NightWavePremiumChrome.kt')
$motion = Read-Source ($prefix + 'NightWaveMotionPolicy.kt')
$theme = Read-Source ($prefix + 'NightWaveTheme.kt')
$activity = Read-Source ($prefix + 'NightWaveStudioActivity.kt')
$mini = Read-Source ($prefix + 'StudioPlayerScreens.kt')
$build = Read-Source 'tools/build-windows.ps1'
$ci = Read-Source '.github/workflows/android.yml'

Need $policy 'MINI_NEXT_MIN_WIDTH_DP = 365f' '320dp mini-player next button regression guard missing'
Need $policy 'NAV_ITEM_COUNT = 4' 'Navigation count contract missing'
Need $policy 'NAV_MIN_TOUCH_DP = 56' 'Navigation touch target contract missing'
Need $chrome 'fun NightWavePremiumNavigation(' 'Premium navigation not implemented'
Need $chrome 'fun NightWaveHomeHero(' 'Premium Home header not implemented'
Need $chrome 'Role.Tab' 'Navigation accessibility role missing'
Need $chrome 'selected = active' 'Selected navigation state missing'
Need $chrome '"nightwave-tab-" + tags[index]' 'Original UI automation tags not preserved'
# NW42 replaces one hardcoded tween with a centrally bounded motion policy
# and an instant snap fallback; keep both paths verified.
Need $chrome 'NightWaveMotionPolicy.duration(' 'Motion duration not wired to navigation'
Need $chrome 'NightWaveMotionPolicy.tabScale(active, reducedMotion)' 'Navigation selection motion not state-driven'
Need $chrome 'if (reducedMotion) snap()' 'Reduced-motion navigation spring bypass missing'
Need $motion 'if (reduceMotion) 0 else requested.coerceIn(0, MAX_MS)' 'Reduced-motion duration must be zero'
Need $motion 'if (reduceMotion) 1f' 'Reduced-motion icon/artwork transforms must be neutral'
Need $theme 'LocalNightWaveReduceMotion provides reduceMotion' 'App-wide reduced-motion preference not provided'
Need (Read-Source ($prefix + 'NightWaveAdaptiveChromePolicy.kt')) 'if (reducedMotion) 0 else regularMs.coerceIn(0, 600)' 'Reduced-motion policy must return zero duration'
Need $activity 'NightWavePremiumNavigation(' 'Root navigation not wired'
Need $activity 'NightWaveHomeHero(' 'Home header not wired'
Need $activity 'navigateToTab(StudioTab.entries[' 'Pager tab navigation bypassed'
Need $activity 'userScrollEnabled = !expanded && !showQueue' 'Background swipes may activate under overlay'
Need $activity 'onHide = { compactHidden = true }' 'Mini-player hide state not preserved'
Need $mini 'NightWaveChromePolicy.showMiniNext(' 'Mini-player responsive controls not wired'
Need $mini 'PlayerGestureRules.decideVerticalSwipe(' 'Mini-player swipe gesture behavior removed'
Need $mini 'actions::playPause' 'Mini-player playback control removed'
Need $mini 'IconButton(onClick = onHide' 'Hide must not pause playback'
Need $mini 'PlayerProgressRules.fraction(' 'Existing player progress lost'
Need $build 'check-phase6v7a-integration.ps1' 'Windows 6V7A source check not registered'
Need $build ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'Real Android Gradle tasks missing'
Need $ci 'python3 tools/verify_v2_phase6v7a.py --self-test' 'GitHub source gate missing'

TestCount 'app/src/test/java/com/nightwave/player/v2/ui/NightWaveChromePolicyTest.kt' 9
TestCount 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWavePremiumChromeInstrumentedTest.kt' 3
TestCount 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveSwipeTabsInstrumentedTest.kt' 2
TestCount 'app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveMiniDismissInstrumentedTest.kt' 2
if ($problems.Count -ne 0) {
    Write-Host ('FAIL: Phase 6V7A source preflight: {0} issue(s)' -f $problems.Count) -ForegroundColor Red
    $problems | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: 6V7A premium Home, tabs, compact player and regression declarations wired.' -ForegroundColor Green
Write-Host 'NOT VERIFIED: Kotlin/Android compilation, runtime or APK update.' -ForegroundColor Yellow
exit 0
