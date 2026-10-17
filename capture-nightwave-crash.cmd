@echo off
setlocal EnableExtensions
title NightWave - Crash and Background Error Capture
echo.
echo NIGHTWAVE CRASH CAPTURE - no app reinstall or data reset required
echo This script reads Android diagnostic logs. It does NOT clear app data.
echo It can capture recent crashes even if NightWave will not open.
echo.

set "ADB="
for %%P in (adb.exe) do set "ADB=%%~$PATH:P"
if not defined ADB if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
)
if not defined ADB (
    echo ERROR: adb.exe not found in PATH or the default Android SDK folder.
    echo Install or locate Android SDK Platform Tools and retry.
    pause
    exit /b 1
)

echo Connect your phone by USB. Turn on USB debugging and approve its prompt.
"%ADB%" devices
echo.
echo Capturing recent crashes NOW. You do NOT need to reproduce a crash.
echo.
set "CRASH=%~dp0NightWave-crash.log"
set "SYSTEM=%~dp0NightWave-system-log.log"
"%ADB%" logcat -b crash -d -v threadtime > "%CRASH%"
if errorlevel 1 (
    echo Could not read crash logs. Check USB debugging authorization.
    pause
    exit /b 1
)
"%ADB%" logcat -b main -b system -d -t 3000 -v threadtime > "%SYSTEM%"
if errorlevel 1 (
    echo Main/system log collection failed. Crash log was still saved.
)
echo.
echo Crash log: %CRASH%
echo Supplemental log: %SYSTEM%
echo.
echo FIRST: open NightWave-crash.log and search for:
echo   "FATAL EXCEPTION"  or  "Process: com.nightwave.player"
echo Include that block and its "Caused by:" stack when sending.
echo.
echo If the crash log has no NightWave exception, share the relevant
echo NightWave/AndroidRuntime/ANR lines from NightWave-system-log.log.
echo.
echo IMPORTANT: These logs may contain personal information and data from
echo other apps. Review/redact sensitive content before sharing.
echo.
echo If no crash is recorded yet, reproduce the issue and run this script
echo again. DO NOT uninstall, clear data, or select reset.
echo.
pause
exit /b 0
