# NightWave 5L: privacy-limited, read-only Android crash summary.
# Raw logcat text stays in process memory and is never persisted to disk.
# Never install, clear, uninstall, force-stop, or reset the phone.
[CmdletBinding()]
param([switch]$SelfTest)
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$reportDir = Join-Path $project 'dist'
$report = Join-Path $reportDir 'NightWave-crash-summary.txt'
$limit = 5

function Locate-Adb {
    $candidates = @()
    $found = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($null -ne $found) { $candidates += $found.Source }
    if ($env:ANDROID_HOME) {
        $candidates += (Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe')
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe')
    }
    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA 'NightWaveBuild\android-sdk\platform-tools\adb.exe')
        $candidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe')
    }
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
    }
    throw 'Android Platform Tools adb.exe was not found. Run build-windows.cmd to provision the local SDK.'
}

function Extract-NightWaveFatal([string[]]$RawLines) {
    $blocks = [Collections.Generic.List[string]]::new()
    $current = [Collections.Generic.List[string]]::new()
    foreach ($line in $RawLines) {
        if ($line -match 'FATAL EXCEPTION:') {
            if ($current.Count -gt 0) {
                $blocks.Add(($current -join [Environment]::NewLine))
                $current.Clear()
            }
        }
        if ($current.Count -gt 0 -or $line -match 'FATAL EXCEPTION:') {
            if ($current.Count -lt 120) { $current.Add([string]$line) }
        }
    }
    if ($current.Count -gt 0) { $blocks.Add(($current -join [Environment]::NewLine)) }

    $matching = @($blocks | Where-Object {
        $_ -match 'Process:\s+com\.nightwave\.player(?:\s*,|$)'
    })
    if ($matching.Count -eq 0) {
        return @('No NightWave FATAL EXCEPTION block was found in Android crash-buffer history.',
            'This does NOT rule out ANRs, low-memory kills or an older evicted crash.')
    }

    $safe = [Collections.Generic.List[string]]::new()
    $safe.Add('NightWave FATAL EXCEPTION summaries (newest first, up to five):')
    $number = 0
    $recent = @($matching | Select-Object -Last $limit)
    for ($i = $recent.Count - 1; $i -ge 0; $i--) {
        $block = [string]$recent[$i]
        $number++
        $safe.Add('')
        $safe.Add('Crash ' + $number.ToString() + ':')
        $lines = $block -split '\r?\n'
        $head = $lines | Where-Object { $_ -match 'FATAL EXCEPTION:' } | Select-Object -First 1
        if ($null -ne $head -and $head -match '^\s*(\d\d-\d\d\s+\d\d:\d\d:\d\d(?:\.\d+)?)') {
            $safe.Add('Log time: ' + $Matches[1])
        }
        $exceptionClasses = [Collections.Generic.List[string]]::new()
        foreach ($line in $lines) {
            if ($line -match '((?:[A-Za-z_$][\w$]*\.)+[A-Za-z_$][\w$]*(?:Exception|Error|Throwable))(?:\:|\s|$)') {
                $klass = $Matches[1]
                if (-not $exceptionClasses.Contains($klass) -and $exceptionClasses.Count -lt 3) {
                    $exceptionClasses.Add($klass)
                }
            }
        }
        if ($exceptionClasses.Count -eq 0) { $safe.Add('Exception class: not available') }
        else { $safe.Add('Exception classes: ' + ($exceptionClasses -join ' / ')) }

        $frames = [Collections.Generic.List[string]]::new()
        foreach ($line in $lines) {
            if ($line -match '\bat\s+(com\.nightwave\.player\.[A-Za-z0-9_.$]+)\([^)]+\)') {
                $frame = $Matches[1]
                if ($line -match '\:([0-9]{1,6})\)') { $frame += ':' + $Matches[1] }
                if ($frames.Count -lt 12) { $frames.Add($frame) }
            }
        }
        if ($frames.Count -eq 0) { $safe.Add('NightWave frames: unavailable') }
        else {
            $safe.Add('NightWave frames:')
            foreach ($frame in $frames) { $safe.Add('  ' + $frame) }
        }
    }
    return @($safe)
}

# Local regression test: synthetic Android crash records, no phone and no log files.
# This tests actual PowerShell filtering behavior on the user's Windows PC.
if ($SelfTest) {
    $example = @(
        '10-17 12:00:00.000 777 777 E AndroidRuntime: FATAL EXCEPTION: main',
        '10-17 12:00:00.010 777 777 E AndroidRuntime: Process: com.example.other, PID: 777',
        '10-17 12:00:00.012 777 777 E AndroidRuntime: java.lang.RuntimeException: OTHER_APP_SECRET',
        '10-17 12:01:00.000 888 888 E AndroidRuntime: FATAL EXCEPTION: main',
        '10-17 12:01:00.010 888 888 E AndroidRuntime: Process: com.nightwave.player, PID: 888',
        '10-17 12:01:00.012 888 888 E AndroidRuntime: java.lang.IllegalStateException: USER_PRIVATE_SECRET',
        '10-17 12:01:00.014 888 888 E AndroidRuntime: at com.nightwave.player.ui.MainActivity.onCreate(MainActivity.kt:42)',
        '10-17 12:01:00.015 888 888 E AndroidRuntime: at android.app.ActivityThread.main(ActivityThread.java:1)'
    )
    $filtered = @(Extract-NightWaveFatal -RawLines $example)
    $result = $filtered -join [Environment]::NewLine
    if ($result -notmatch 'java.lang.IllegalStateException' -or
        $result -notmatch 'com.nightwave.player.ui.MainActivity.onCreate:42' -or
        $result -notmatch 'Crash 1:' -or
        $result -match 'OTHER_APP_SECRET|USER_PRIVATE_SECRET|com.example.other|PID:|ActivityThread.main') {
        Write-Host 'FAILED: synthetic NightWave privacy filter checks.' -ForegroundColor Red
        exit 1
    }
    Write-Host 'PASS: NightWave-only crash filtering, redaction and stack summary.' -ForegroundColor Green
    exit 0
}

try {
    New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
    $adb = Locate-Adb
    $deviceLines = @(& $adb devices 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'adb devices failed.' }
    $devices = @($deviceLines | ForEach-Object {
        if ($_ -match '^(\S+)\s+(device|offline|unauthorized)(?:\s|$)') {
            [PSCustomObject]@{ Id = $Matches[1]; State = $Matches[2] }
        }
    })
    if ($devices.Count -ne 1 -or $devices[0].State -ne 'device') {
        throw 'Connect and authorize exactly one Android device. No logs were saved.'
    }
    $serial = $devices[0].Id
    # The crash buffer can include other apps. Hold output in memory only.
    # Only sanitized records matching the NightWave package are written.
    $raw = @(& $adb -s $serial logcat -b crash -d -t 4000 -v threadtime 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'Android crash-buffer read failed.' }
    $safe = @(Extract-NightWaveFatal -RawLines $raw)
    $raw = $null
    $preface = @(
        'NightWave Android crash summary (sanitized; no raw logcat is saved)',
        'Package: com.nightwave.player',
        'Only exception class names, NightWave method frames and crash-buffer timestamps.',
        'No exception messages, other-app entries, PIDs, URLs or system trace content.',
        'This report is diagnostic evidence, not a crash-free certification.',
        ''
    )
    @($preface) + @($safe) | Set-Content -LiteralPath $report -Encoding UTF8
    Write-Host 'NightWave-only crash summary created.' -ForegroundColor Green
    Write-Host ('Report: ' + $report)
    exit 0
} catch {
    New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
    @(
        'NightWave crash summary unavailable.',
        'Reason: ' + $_.Exception.Message,
        'No raw Android logcat was saved. Phone data was not modified.'
    ) | Set-Content -LiteralPath $report -Encoding UTF8
    Write-Host ('NightWave crash capture failed: ' + $_.Exception.Message) -ForegroundColor Yellow
    Write-Host ('Report: ' + $report)
    exit 1
}
