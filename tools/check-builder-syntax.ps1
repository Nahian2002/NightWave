# Validate the PowerShell builder with Windows PowerShell's own AST parser.
# Runs before Gradle, so parse errors cannot waste an Android build attempt.
[CmdletBinding()]
param()

$scriptFile = Join-Path $PSScriptRoot 'build-windows.ps1'
if (-not (Test-Path -LiteralPath $scriptFile)) {
    Write-Host "NightWave builder missing: $scriptFile" -ForegroundColor Red
    exit 1
}

$tokens = $null
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile(
    $scriptFile, [ref]$tokens, [ref]$parseErrors
) | Out-Null

if ($null -ne $parseErrors -and $parseErrors.Count -gt 0) {
    Write-Host 'NightWave builder PowerShell syntax check FAILED:' -ForegroundColor Red
    foreach ($problem in $parseErrors) {
        Write-Host ('Line {0}, column {1}: {2}' -f
            $problem.Extent.StartLineNumber,
            $problem.Extent.StartColumnNumber,
            $problem.Message) -ForegroundColor Red
    }
    exit 1
}

Write-Host 'NightWave builder PowerShell syntax check passed.' -ForegroundColor Green
exit 0
