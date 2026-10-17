# NightWave local APK builder. All downloads are from upstream Android/Gradle/Eclipse vendors.
# Runs without administrator privileges or GitHub Actions/billing.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Say([string]$Message) { Write-Host "[NightWave] $Message" -ForegroundColor Cyan }
function Get-Archive([string]$Url, [string]$Destination) {
    if (Test-Path -LiteralPath $Destination) { return }
    Say "Downloading $Url"
    $part = "$Destination.partial"
    if (Test-Path -LiteralPath $part) { Remove-Item -LiteralPath $part -Force }
    try {
        Invoke-WebRequest -Uri $Url -OutFile $part -MaximumRedirection 10 -UseBasicParsing
        if ((Get-Item -LiteralPath $part).Length -lt 1000000) { throw "Download is unexpectedly small: $Url" }
        Move-Item -LiteralPath $part -Destination $Destination -Force
    } catch {
        Remove-Item -LiteralPath $part -Force -ErrorAction SilentlyContinue
        throw
    }
}
function Set-Java([string]$Exe) {
    $homeDir = Split-Path (Split-Path $Exe -Parent) -Parent
    $env:JAVA_HOME = $homeDir
    $env:PATH = "$(Join-Path $homeDir 'bin');$env:PATH"
    Say "Using Java from $homeDir"
}
function Resolve-Java {
    $possible = @()
    if ($env:JAVA_HOME) { $possible += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
    $possible += @(
        'C:\Program Files\Android\Android Studio\jbr\bin\java.exe',
        'C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe',
        'C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe'
    )
    $onPath = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($onPath) { $possible += $onPath.Source }
    foreach ($exe in $possible) {
        if (-not (Test-Path -LiteralPath $exe)) { continue }
        # Capture stderr without treating java -version's ordinary output as a PowerShell error.
        $previousErrorPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try { $versionText = (& $exe -version 2>&1 | Out-String) } catch { $versionText = '' }
        finally { $ErrorActionPreference = $previousErrorPreference }
        if ($versionText -match 'version "(?:1\.)?(\d+)') {
            if ([int]$Matches[1] -ge 17) { Set-Java $exe; return }
        } elseif ($versionText -match 'openjdk (\d+)') {
            if ([int]$Matches[1] -ge 17) { Set-Java $exe; return }
        }
    }
    $jdkDir = Join-Path $script:cache 'jdk'
    $jdkExe = Get-ChildItem -Path $jdkDir -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '[\\/]bin[\\/]java\.exe$' } | Select-Object -First 1
    if (-not $jdkExe) {
        $jdkArchive = Join-Path $script:cache 'temurin17-jdk.zip'
        Get-Archive 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' $jdkArchive
        New-Item -ItemType Directory -Path $jdkDir -Force | Out-Null
        Expand-Archive -LiteralPath $jdkArchive -DestinationPath $jdkDir -Force
        $jdkExe = Get-ChildItem -Path $jdkDir -Filter java.exe -Recurse |
            Where-Object { $_.FullName -match '[\\/]bin[\\/]java\.exe$' } | Select-Object -First 1
    }
    if (-not $jdkExe) { throw 'Java 17 was not found inside the downloaded archive.' }
    Set-Java $jdkExe.FullName
}

# Build-state files are written even if Java, Gradle or Android SDK setup fails.
# A stale success APK is archived rather than accidentally delivered as new.
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$outDir = Join-Path $project 'dist'
$buildLog = Join-Path $outDir 'NightWave-build-full.log'
$errorReport = Join-Path $outDir 'NightWave-build-errors.txt'
$statusFile = Join-Path $outDir 'NightWave-build-status.txt'
$output = Join-Path $outDir 'NightWave-debug.apk'
$previousApk = Join-Path $outDir 'NightWave-debug-previous.apk'
$stage = 'Preflight'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
if (Test-Path -LiteralPath $errorReport) { Remove-Item -LiteralPath $errorReport -Force }
'STARTED - not a verified build. Do not install an old APK.' |
    Set-Content -LiteralPath $statusFile -Encoding UTF8
try {
    if (-not [Environment]::Is64BitOperatingSystem) { throw '64-bit Windows is required.' }
    $script:cache = Join-Path $env:LOCALAPPDATA 'NightWaveBuild'
    New-Item -ItemType Directory -Path $script:cache -Force | Out-Null
    if (Test-Path -LiteralPath $output) {
        Copy-Item -LiteralPath $output -Destination $previousApk -Force
        Remove-Item -LiteralPath $output -Force
        Say "Previous APK preserved as $previousApk"
    }
    Say 'No GitHub billing or payment method is required.'
    Say 'The first build downloads official build tools and may take time and several GB of disk space.'
    $stage = 'Java preparation'
    Resolve-Java

    $stage = 'Gradle preparation'
    $gradleDir = Join-Path $script:cache 'gradle-8.11.1'
    $gradleBat = Join-Path $gradleDir 'bin\gradle.bat'
    if (-not (Test-Path -LiteralPath $gradleBat)) {
        $zip = Join-Path $script:cache 'gradle-8.11.1-bin.zip'
        Get-Archive 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' $zip
        Say 'Verifying Gradle SHA-256 checksum'
        $checksumPath = Join-Path $script:cache 'gradle-8.11.1-bin.zip.sha256'
        if (-not (Test-Path -LiteralPath $checksumPath)) {
            Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256' -OutFile $checksumPath -UseBasicParsing
        }
        $checksum = (Get-Content -LiteralPath $checksumPath -Raw).Trim().Split(' ')[0]
        $actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash
        if ($actual -ne $checksum) { throw 'Gradle checksum mismatch; stopping.' }
        Expand-Archive -LiteralPath $zip -DestinationPath $script:cache -Force
    }
    if (-not (Test-Path -LiteralPath $gradleBat)) { throw 'Gradle executable missing after installation.' }

    $stage = 'Android SDK preparation'
    $sdk = Join-Path $script:cache 'android-sdk'
    $sdkmanager = Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
    if (-not (Test-Path -LiteralPath $sdkmanager)) {
        $cliZip = Join-Path $script:cache 'android-commandline-tools.zip'
        Get-Archive 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' $cliZip
        $tmp = Join-Path $script:cache 'sdk-cli-unpack'
        New-Item -ItemType Directory -Path $tmp -Force | Out-Null
        Expand-Archive -LiteralPath $cliZip -DestinationPath $tmp -Force
        $from = Join-Path $tmp 'cmdline-tools'
        if (-not (Test-Path -LiteralPath (Join-Path $from 'bin\sdkmanager.bat'))) {
            throw 'Android command-line tools archive has an unexpected layout.'
        }
        New-Item -ItemType Directory -Path (Join-Path $sdk 'cmdline-tools') -Force | Out-Null
        Move-Item -LiteralPath $from -Destination (Join-Path $sdk 'cmdline-tools\latest')
        Remove-Item -LiteralPath $tmp -Recurse -Force
    }
    if (-not (Test-Path -LiteralPath $sdkmanager)) { throw 'Android SDK manager was not installed.' }
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
    Say 'Android SDK licenses must be accepted once. Read the terms and type y when you agree.'
    & $sdkmanager "--sdk_root=$sdk" --licenses
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK licenses were not accepted.' }
    Say 'Installing Android platform 35 and build tools 35.0.0'
    & $sdkmanager "--sdk_root=$sdk" 'platforms;android-35' 'build-tools;35.0.0'
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK package installation failed.' }

    $stage = 'Gradle compile / unit tests'
    Say 'Running Android unit tests and compiling the debug APK'
    # The complete Gradle output is retained; preflight failures also get a report.
    Say "Saving build output to $buildLog"

    Push-Location $project
    try {
        $previousErrorPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            & $gradleBat --no-daemon --console=plain --warning-mode=summary :app:testDebugUnitTest :app:assembleDebug 2>&1 |
                ForEach-Object { $_.ToString() } | Tee-Object -FilePath $buildLog
            $gradleResult = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorPreference
        }

        # A zero exit code without Gradle's terminal success marker is not
        # enough to publish a possibly stale APK from another checkout.
        $gradleSucceeded = $gradleResult -eq 0 -and
            [bool](Select-String -LiteralPath $buildLog -Pattern '^BUILD SUCCESSFUL' -Quiet)
        if (-not $gradleSucceeded) {
            $patterns = @(
                '^e:\s',
                '(^|\s)error:\s',
                'Unresolved reference',
                'Type mismatch',
                'Cannot infer',
                'No value passed for parameter',
                'Overload resolution ambiguity',
                '^> Task .* FAILED',
                '^Execution failed for task',
                '^FAILURE:',
                '^Could not resolve',
                'Compilation error',
                'KSP.*error',
                '\s>\s.*\sFAILED$',
                'java\.lang\.AssertionError',
                '\d+ tests completed, \d+ failed',
                'There were failing tests'
            )
            $matchedLines = @(
                Select-String -LiteralPath $buildLog -Pattern $patterns |
                    Select-Object -First 65 |
                    ForEach-Object { $_.Line.TrimEnd() }
            )
            if ($matchedLines.Count -eq 0) {
                $matchedLines = @(Get-Content -LiteralPath $buildLog -Tail 75)
            }
            $report = @(
                'NightWave build failure - diagnostic summary',
                "Build log: $buildLog",
                'Compiler errors and failed tests appear below (deprecation warnings omitted):',
                ''
            ) + $matchedLines
            $report | Set-Content -LiteralPath $errorReport -Encoding UTF8
            Write-Host ''
            Write-Host '===== FIRST COMPILER ERRORS / FAILURE SUMMARY =====' -ForegroundColor Red
            $matchedLines | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
            Write-Host '===================================================' -ForegroundColor Red
            Write-Host "Send this file for repairs: $errorReport" -ForegroundColor Cyan
            Write-Host "Complete build log:       $buildLog" -ForegroundColor Cyan
            throw 'Gradle returned an Android compilation/test failure. A diagnostic report has been saved.'
        }
    } finally {
        Pop-Location
    }

    $stage = 'APK verification'
    $apk = Join-Path $project 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) {
        throw 'Gradle exited successfully but the debug APK was not found.'
    }
    # Confirm Android actually packaged the correct ID and incremented version,
    # rather than copying a build left by an older checkout.
    $aapt = Join-Path $sdk 'build-tools\35.0.0\aapt.exe'
    if (-not (Test-Path -LiteralPath $aapt)) {
        throw 'Android aapt.exe missing; cannot validate the APK identity.'
    }
    $badging = (& $aapt dump badging $apk 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or
        $badging -notmatch "package: name='com\.nightwave\.player' versionCode='23' versionName='2\.0\.0-alpha5g'") {
        throw 'APK package/version does not match NightWave Alpha 5G, build 23.'
    }
    Copy-Item -LiteralPath $apk -Destination $output -Force
    $hash = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash
    @(
        'SUCCESS - NightWave Android debug APK verified',
        'Package: com.nightwave.player',
        'Version: 2.0.0-alpha5g',
        'Version code: 23',
        "SHA256: $hash",
        "APK: $output"
    ) | Set-Content -LiteralPath $statusFile -Encoding UTF8
    Say 'BUILD SUCCESS'
    Say "APK: $output"
    Write-Host "SHA256: $hash"
    exit 0
} catch {
    $reason = $_.Exception.Message
    # Preflight failures previously had no errors.txt, causing the user to
    # repeatedly upload stale logs from an older build.
    if (-not (Test-Path -LiteralPath $errorReport)) {
        @(
            'NightWave build failure - diagnostic summary',
            "Stage: $stage",
            "Reason: $reason",
            'No Android APK was verified or published by this build.'
        ) | Set-Content -LiteralPath $errorReport -Encoding UTF8
    }
    @(
        'FAILED - do not install dist\NightWave-debug.apk from this attempt.',
        "Stage: $stage",
        "Reason: $reason",
        "Diagnostic: $errorReport",
        "Previous verified copy, if present: $previousApk"
    ) | Set-Content -LiteralPath $statusFile -Encoding UTF8
    Write-Host "[NightWave] BUILD FAILED ($stage): $reason" -ForegroundColor Red
    Write-Host "Diagnostics: $errorReport" -ForegroundColor Yellow
    exit 1
}
