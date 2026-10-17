@echo off
setlocal EnableExtensions
title NightWave - Private Android Crash Summary
cd /d "%~dp0"
echo.
echo NIGHTWAVE CRASH SUMMARY - READ ONLY
echo.
echo This reads a limited Android crash buffer and saves ONLY:
echo   NightWave exception class names, NightWave method frames, timestamps.
echo.
echo It never saves a raw all-app log and never resets or changes your phone.
echo Make sure exactly one phone is connected and USB debugging is approved.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\capture-nightwave-crash.ps1"
set "CAPTURE_RESULT=%ERRORLEVEL%"
echo.
if "%CAPTURE_RESULT%"=="0" (
    echo Sanitized report: dist\NightWave-crash-summary.txt
    echo Review the report before sharing it.
) else (
    echo Capture did not succeed. Check dist\NightWave-crash-summary.txt
    echo Verify USB authorization and that Android Platform Tools are installed.
)
echo.
echo When no recent NightWave fatal exception is found, NightWave's
echo in-app You - App health report can show Android ANR / system exit reasons.
echo.
pause
exit /b %CAPTURE_RESULT%
