# Fail-fast Kotlin multiline LRC fixture check on Windows PowerShell 5.1.
# "one" + "two".toByteArray() is String; group before conversion.
# Read-only; no APK, library, files, credentials or Android device modified.
[CmdletBinding()]
param([switch]$SelfTest)
$ErrorActionPreference = 'Stop'
$pattern = '(?m)(?:"(?:\\.|[^"\\])*"\s*\+\s*)+"(?:\\.|[^"\\])*"\s*\.toByteArray\s*\('
function Count-BrokenFixture([string]$content) {
    return ([regex]::Matches($content, $pattern)).Count
}
if ($SelfTest) {
    $broken = 'parse("one" + "two".toByteArray())'
    $correct = 'parse(("one" + "two").toByteArray())'
    $multiline = 'parse("one" +' + [Environment]::NewLine + ' "two".toByteArray())'
    if ((Count-BrokenFixture $broken) -ne 1 -or
        (Count-BrokenFixture $multiline) -ne 1 -or
        (Count-BrokenFixture $correct) -ne 0 -or
        (Count-BrokenFixture 'parse("single".toByteArray())') -ne 0) {
        Write-Host 'FAIL: Kotlin ByteArray fixture checker self-test failed.' -ForegroundColor Red
        exit 1
    }
    Write-Host 'PASS: synthetic Kotlin fixture precedence check.' -ForegroundColor Green
    exit 0
}
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$roots = @('app\src\test', 'app\src\androidTest')
$issues = New-Object 'System.Collections.Generic.List[string]'
foreach ($relative in $roots) {
    $folder = Join-Path $project $relative
    if (-not (Test-Path -LiteralPath $folder -PathType Container)) {
        $issues.Add("Missing Kotlin test source directory: $relative")
        continue
    }
    foreach ($file in @(Get-ChildItem -LiteralPath $folder -Filter '*.kt' -File -Recurse)) {
        $source = [IO.File]::ReadAllText($file.FullName, [Text.Encoding]::UTF8)
        foreach ($match in [regex]::Matches($source, $pattern)) {
            $line = ($source.Substring(0, $match.Index).Split([char]10)).Count
            $issues.Add(('{0}:{1}: group concatenated Strings before .toByteArray()' -f
                $file.FullName.Substring($project.Length + 1), $line))
        }
    }
}
if ($issues.Count -gt 0) {
    $issues | ForEach-Object { Write-Host "FAIL: $_" -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: Kotlin test fixture byte conversion is grouped correctly.' -ForegroundColor Green
Write-Host 'NOT VERIFIED: real Gradle or Android test compilation.' -ForegroundColor Yellow
exit 0
