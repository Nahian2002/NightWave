@echo off
setlocal
for %%I in ("%~dp0..\..") do set "PROJECT_ROOT=%%~fI"
cd /d "%PROJECT_ROOT%" || exit /b 1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\tools\nw77-release-check.ps1" -SelfTest
if errorlevel 1 (
  echo NW77 release checker self-test failed. No public build approved.
  pause
  exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\tools\nw77-release-check.ps1" -ProjectRoot "%PROJECT_ROOT%"
set "RC=%ERRORLEVEL%"
if "%RC%"=="2" (
  echo.
  echo EXPECTED: NW77 has identified current public-release blockers.
  echo Private debug builds still work and your existing music is untouched.
)
echo.
echo Report: dist\NightWave-NW77-public-release-report.txt
pause
exit /b %RC%
