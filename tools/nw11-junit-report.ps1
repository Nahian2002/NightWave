# NW-11: read-only, privacy-reduced JVM test failure report.
# The Gradle test XML is local compiler output; never collect files, tokens,
# private account contents, raw stack traces, or user music.
[CmdletBinding()]
param(
    [string]$ResultsDirectory = '',
    [string]$OutputFile = '',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $ResultsDirectory) {
    $ResultsDirectory = Join-Path $root 'app\build\test-results\testDebugUnitTest'
}
if (-not $OutputFile) {
    $OutputFile = Join-Path $root 'dist\NightWave-jvm-failure-summary.txt'
}

function Clean-Field([string]$value, [int]$limit = 220) {
    if ($null -eq $value) { return '' }
    $v = $value -replace '[\r\n\t]+', ' '
    $v = $v -replace '(?i)([A-Z]:\\Users\\)[^\\\s]+\\', '$1<user>\'
    $v = $v -replace '(?i)(/home/|/Users/)[^/\s]+/', '$1<user>/'
    $v = $v -replace '(?i)(Authorization:\s*(?:Bearer|Basic)\s+)\S+', '$1<redacted>'
    $v = $v -replace '(?i)([?&](?:token|access_token|refresh_token|api_key|key|client_secret|sig|signature|code)=)[^\s&#]+', '$1<redacted>'
    $v = $v -replace '(?i)((?:client_secret|refresh_token|access_token|id_token)\s*[:=]\s*)[^\s&#]+', '${1}<redacted>'
    $v = $v.Trim()
    if ($v.Length -gt $limit) { return ($v.Substring(0, $limit) + '…') }
    return $v
}

function Collect-TestFailures([System.Xml.XmlReader]$reader) {
    # ReadOuterXml followed by Read() skips the next sibling test case.
    # Parse the bounded, DTD-prohibited XML document once instead, then
    # iterate exactly the failing nodes. The reader caps input at 8 MB.
    $doc = New-Object System.Xml.XmlDocument
    $doc.XmlResolver = $null
    $doc.Load($reader)
    $found = [Collections.Generic.List[string]]::new()
    $failures = $doc.SelectNodes('//testcase[failure or error]')
    foreach ($node in $failures) {
        if ($found.Count -ge 25) { break }
        $failure = $node.SelectSingleNode('./failure|./error')
        if ($null -eq $failure) { continue }
        $className = Clean-Field ([string]$node.GetAttribute('classname')) 100
        $name = Clean-Field ([string]$node.GetAttribute('name')) 100
        $messageAttribute = $failure.Attributes['message']
        $message = if ($null -ne $messageAttribute) {
            Clean-Field ([string]$messageAttribute.Value) 220
        } else { '' }
        if (-not $message) {
            # No full stack trace; only a short diagnostic first line.
            $message = Clean-Field ([string]($failure.InnerText -split '\r?\n' | Select-Object -First 1)) 220
        }
        $found.Add("$className.$name — $message")
    }
    return @($found.ToArray())
}

function Read-JUnit([string]$path) {
    $settings = New-Object System.Xml.XmlReaderSettings
    $settings.DtdProcessing = [System.Xml.DtdProcessing]::Prohibit
    $settings.XmlResolver = $null
    $settings.MaxCharactersInDocument = 8MB
    $reader = [System.Xml.XmlReader]::Create($path, $settings)
    try { return @(Collect-TestFailures $reader) }
    finally { $reader.Dispose() }
}

try {
    if ($SelfTest) {
        $fake = '<testsuite><testcase classname="First" name="firstFailure"><failure message="first"/></testcase><testcase classname="Second" name="adjacentFailure"><error message="second"/></testcase><testcase classname="OfflinePlaybackQueueTest" name="jumpHandlesBoundsWithoutDamagingSelection"><failure message="expected b but was c"/></testcase><testcase classname="Other" name="passing"/><testcase classname="Final" name="withoutMessage"><failure>assertion failed</failure></testcase></testsuite>'
        $settings = New-Object System.Xml.XmlReaderSettings
        $settings.DtdProcessing = [System.Xml.DtdProcessing]::Prohibit
        $reader = [System.Xml.XmlReader]::Create((New-Object System.IO.StringReader($fake)), $settings)
        try { $found = @(Collect-TestFailures $reader) } finally { $reader.Dispose() }
        if ($found.Count -ne 4 -or
            $found[0] -notmatch 'firstFailure' -or
            $found[1] -notmatch 'adjacentFailure' -or
            $found[2] -notmatch 'jumpHandlesBoundsWithoutDamagingSelection' -or
            $found[2] -notmatch 'expected b but was c' -or
            $found[3] -notmatch 'withoutMessage') {
            throw 'JUnit failure extraction self-test failed.'
        }
        if ((Clean-Field 'Authorization: Bearer secret123') -match 'secret123' -or
            (Clean-Field 'C:\Users\private123\Downloads') -match 'private123') {
            throw 'JUnit redaction self-test failed.'
        }
        Write-Host 'PASS: NW11 safe JVM failure extraction and redaction self-test'
        exit 0
    }
    $lines = [Collections.Generic.List[string]]::new()
    $lines.Add('NightWave NW-11 JVM failure summary (diagnostic only, not an APK)')
    $lines.Add('The report cannot certify Gradle success or release readiness.')
    $files = if (Test-Path -LiteralPath $ResultsDirectory -PathType Container) {
        @(Get-ChildItem -LiteralPath $ResultsDirectory -Filter 'TEST-*.xml' -File |
            Sort-Object Name | Select-Object -First 300)
    } else { @() }
    $totalFailures = 0
    foreach ($file in $files) {
        try {
            $failures = @(Read-JUnit $file.FullName)
            foreach ($line in $failures) {
                if ($totalFailures -ge 50) { break }
                $lines.Add($line)
                $totalFailures++
            }
        } catch {
            $lines.Add('Unreadable JUnit XML: one test suite (details withheld).')
        }
        if ($totalFailures -ge 50) { break }
    }
    if ($files.Count -eq 0) {
        $lines.Add('NO TEST XML: JVM tests did not run or results were not retained.')
    } elseif ($totalFailures -eq 0) {
        $lines.Add('NO FAILURES FOUND in sampled XML. This is not proof all tests ran.')
    }
    $outputDir = Split-Path -Parent $OutputFile
    if ($outputDir -and -not (Test-Path -LiteralPath $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }
    $lines.ToArray() | Set-Content -LiteralPath $OutputFile -Encoding UTF8
    Write-Host "NW11 JUnit diagnostic saved. Sampled suites: $($files.Count); findings: $totalFailures"
    exit 0
} catch {
    Write-Host ('NW11 JUnit diagnostic failed (no APK affected): ' + $_.Exception.Message)
    exit 1
}
