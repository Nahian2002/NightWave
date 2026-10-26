# NW76 read-only JUnit receipt. Called ONLY after the full Gradle command succeeds.
# Never examines an installed app, APK signer, user audio or Google credentials.
param(
    [switch]$SelfTest,
    [string]$ResultsDirectory = '',
    [string]$OutputFile = '',
    [ValidateRange(1, 1000000)][int]$MinimumTests = 1000
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Read-IntAttribute([System.Xml.XmlElement]$suite, [string]$name) {
    $raw = $suite.GetAttribute($name)
    $value = 0
    if ([string]::IsNullOrWhiteSpace($raw) -or
        -not [int]::TryParse($raw, [ref]$value) -or $value -lt 0) {
        throw "JUnit suite has invalid/missing $name count."
    }
    return $value
}

function Measure-JunitReceipt([string]$folder, [int]$minimum) {
    if (-not (Test-Path -LiteralPath $folder -PathType Container)) {
        throw 'JUnit results folder is missing; cannot certify Gradle tests.'
    }
    $files = @(Get-ChildItem -LiteralPath $folder -Filter 'TEST-*.xml' -File -Recurse)
    if ($files.Count -eq 0) { throw 'No TEST-*.xml reports; unit tests not verified.' }
    $total = 0
    $failed = 0
    $errors = 0
    $skipped = 0
    foreach ($file in $files) {
        $settings = New-Object System.Xml.XmlReaderSettings
        $settings.DtdProcessing = [System.Xml.DtdProcessing]::Prohibit
        $settings.XmlResolver = $null
        $reader = [System.Xml.XmlReader]::Create($file.FullName, $settings)
        try {
            $doc = New-Object System.Xml.XmlDocument
            $doc.XmlResolver = $null
            $doc.Load($reader)
        } finally {
            $reader.Dispose()
        }
        $suite = $doc.DocumentElement
        if ($null -eq $suite -or $suite.LocalName -ne 'testsuite') {
            throw 'Unexpected JUnit XML root. Cannot infer test success.'
        }
        $n = Read-IntAttribute $suite 'tests'
        $f = Read-IntAttribute $suite 'failures'
        $e = Read-IntAttribute $suite 'errors'
        $k = Read-IntAttribute $suite 'skipped'
        if ($f + $e + $k -gt $n) {
            throw 'Inconsistent JUnit counters (failure/error/skip > tests).'
        }
        $cases = $suite.GetElementsByTagName('testcase').Count
        if ($cases -ne $n) {
            throw 'JUnit testsuite count does not match the recorded testcases.'
        }
        $total += $n
        $failed += $f
        $errors += $e
        $skipped += $k
    }
    if ($total -lt $minimum) {
        throw "Only $total JVM tests reported; expected at least $minimum. Do not publish."
    }
    if ($failed -ne 0 -or $errors -ne 0) {
        throw "JVM tests failed: $failed failures, $errors errors."
    }
    $passed = $total - $skipped
    return [pscustomobject]@{
        Suites = $files.Count
        Total = $total
        Passed = $passed
        Failed = $failed
        Errors = $errors
        Skipped = $skipped
    }
}

function Invoke-SelfTest {
    $fixture = Join-Path ([IO.Path]::GetTempPath()) ('nw76-junit-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $fixture -Force | Out-Null
    try {
        $a = @'
<testsuite name="a" tests="2" failures="0" errors="0" skipped="0">
  <testcase classname="Smoke" name="one"/>
  <testcase classname="Smoke" name="two"/>
</testsuite>
'@
        $b = @'
<testsuite name="b" tests="2" failures="0" errors="0" skipped="1">
  <testcase classname="Smoke" name="three"/>
  <testcase classname="Smoke" name="four"><skipped/></testcase>
</testsuite>
'@
        $first = Join-Path $fixture 'TEST-a.xml'
        $second = Join-Path $fixture 'TEST-b.xml'
        [IO.File]::WriteAllText($first, $a)
        [IO.File]::WriteAllText($second, $b)
        $receipt = Measure-JunitReceipt $fixture 3
        if ($receipt.Total -ne 4 -or $receipt.Passed -ne 3 -or
            $receipt.Skipped -ne 1 -or $receipt.Suites -ne 2) {
            throw 'JUnit receipt positive fixture failed.'
        }

        $rejected = $false
        try { $null = Measure-JunitReceipt $fixture 5 } catch { $rejected = $true }
        if (-not $rejected) { throw 'JUnit undercount was not rejected.' }

        [IO.File]::WriteAllText($first, $a.Replace('failures="0"', 'failures="1"'))
        $rejected = $false
        try { $null = Measure-JunitReceipt $fixture 3 } catch { $rejected = $true }
        if (-not $rejected) { throw 'JUnit failure was not rejected.' }

        [IO.File]::WriteAllText($first, $a.Replace('tests="2"', 'tests="3"'))
        $rejected = $false
        try { $null = Measure-JunitReceipt $fixture 3 } catch { $rejected = $true }
        if (-not $rejected) { throw 'JUnit forged count was not rejected.' }

        [IO.File]::WriteAllText($first, '<!DOCTYPE testsuite [<!ENTITY ext SYSTEM "file:///secrets">]><testsuite/>')
        $rejected = $false
        try { $null = Measure-JunitReceipt $fixture 3 } catch { $rejected = $true }
        if (-not $rejected) { throw 'JUnit XML DTD was not rejected.' }

        Remove-Item -LiteralPath $first, $second -Force
        $rejected = $false
        try { $null = Measure-JunitReceipt $fixture 1 } catch { $rejected = $true }
        if (-not $rejected) { throw 'Missing JUnit XML was not rejected.' }
    } finally {
        if (Test-Path -LiteralPath $fixture) {
            Remove-Item -LiteralPath $fixture -Recurse -Force
        }
    }
    Write-Host 'NW76 JUnit proof self-test PASS (valid, skipped, missing, forged, failing and hostile XML).' -ForegroundColor Green
}

try {
    if ($SelfTest) {
        Invoke-SelfTest
        exit 0
    }
    if ([string]::IsNullOrWhiteSpace($ResultsDirectory) -or
        [string]::IsNullOrWhiteSpace($OutputFile)) {
        throw 'ResultsDirectory and OutputFile are required. Never infer a pass.'
    }
    $receipt = Measure-JunitReceipt $ResultsDirectory $MinimumTests
    $lines = @(
        'PASS - NightWave NW76 current-attempt JVM test receipt',
        "Test suites: $($receipt.Suites)",
        "Total JVM tests: $($receipt.Total)",
        "Passed: $($receipt.Passed)",
        "Failures: $($receipt.Failed)",
        "Errors: $($receipt.Errors)",
        "Skipped: $($receipt.Skipped)",
        "Minimum accepted: $MinimumTests",
        'Source: Gradle TEST-*.xml reports generated by this build attempt.',
        'Limit: This does not certify Android runtime, Play licensing or device playback.'
    )
    $lines | Set-Content -LiteralPath $OutputFile -Encoding UTF8
    $lines | ForEach-Object { Write-Host $_ }
    exit 0
} catch {
    Write-Host ('NW76 JUnit proof FAILED: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
