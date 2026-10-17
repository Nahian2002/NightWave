@echo off
setlocal EnableExtensions
title NightWave - Android Crash Capture
echo.
echo NightWave crash diagnostics - read-only Android log collection
echo No app data is cleared. No apps are uninstalled.
echo.
set "ADB="
for %%P in (adb.exe) do set "ADB=%%~$PATH:P"
if not defined ADB if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
  set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
)
if not defined ADB (
  echo ERROR: Android adb.exe not found.
  echo Install Android SDK Platform Tools or use Android Studio's SDK.
  echo If available, run adb logcat -b crash -d on your PC.
  echo.
  pause
  exit /b 1
)
echo Using ADB: %ADB%
echo Connect your Android phone by USB and allow USB debugging.
echo.
"%ADB%" devices
echo.
echo Now open NightWave's premium preview on your phone.
echo If the app closes unexpectedly, return here and press any key.
echo.
pause >nul
set "REPORT=%~dp0NightWave-crash.log"
"%ADB%" logcat -b crash -d -t 600 > "%REPORT%"
if errorlevel 1 (
  echo Failed to capture crash log. Verify USB debugging authorization.
  pause
  exit /b 1
)
echo.
echo Crash buffer saved to: %REPORT%
echo Open the file and look for "FATAL EXCEPTION" and "com.nightwave.player".
echo Review the file for private data before sharing.
echo Note: the crash buffer can contain records from other apps.
echo.
pause
exit /b 0
