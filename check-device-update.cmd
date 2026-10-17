@echo off
setlocal
cd /d "%~dp0"
echo.
echo NightWave - read-only Android update compatibility check
echo.
echo This does NOT install, uninstall, reset, or modify your phone.
echo Connect and authorize exactly one Android phone using USB debugging.
echo A freshly verified NightWave debug APK must already exist in dist.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\check-device-update.ps1"
set "VERIFY_EXIT=%ERRORLEVEL%"
echo.
if not "%VERIFY_EXIT%"=="0" (
  echo INCONCLUSIVE - do NOT uninstall NightWave or assume the update is safe.
) else (
  echo Device update compatibility preflight PASSED.
  echo This is NOT a playback or crash-free certification.
)
echo Report: dist\NightWave-device-update-check.txt
echo.
pause
exit /b %VERIFY_EXIT%
