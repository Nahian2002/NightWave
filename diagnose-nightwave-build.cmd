@echo off
setlocal EnableExtensions
title NightWave - Android Build Diagnosis
cd /d "%~dp0"
echo.
echo NightWave - Read-only build diagnosis
echo This scans generated logs; it does not build, install, uninstall
echo or change any installed songs, storage or device settings.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\diagnose-build.ps1"
set "RESULT=%ERRORLEVEL%"
echo.
if "%RESULT%"=="0" (
  echo Read the compact report at dist\NightWave-build-diagnostic.txt
  echo If a Windows build is needed, run build-windows.cmd separately.
) else (
  echo No report could be generated. Check PowerShell and the dist folder.
)
echo.
pause
exit /b %RESULT%
