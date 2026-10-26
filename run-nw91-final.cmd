@echo off
setlocal
cd /d "%~dp0"
echo.
echo =============================================================
echo  NIGHTWAVE NW91 - VERIFIED-BUILD ATTEMPT (PRIVATE DEBUG ONLY)
echo =============================================================
echo This does NOT install, uninstall, clear, or alter your phone.
echo.
echo NW91 fixes source/JUnit evidence acceptance, not media or app data.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\nw90-windows-gate.ps1"
if errorlevel 1 (
  echo NW91 inherited source and matrix preflight FAILED.
  pause
  exit /b 1
)
call "%~dp0build-windows.cmd"
if errorlevel 1 (
  echo NW91 Gradle/JUnit/APK did NOT certify. Review current dist reports.
  pause
  exit /b 2
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\nw90-windows-gate.ps1" -AfterBuild
if errorlevel 1 (
  echo NW91 receipt/source fingerprint check FAILED. Do NOT install.
  pause
  exit /b 3
)
echo.
echo NW91 locally built debug evidence PASS, NOT on-device certification.
echo Run check-device-update.cmd BEFORE replacing installed NightWave.
echo Preserve the app's existing offline audio and Google account state.
echo Public release remains NO-GO. See docs\NW91_BUILD_RECOVERY_EVIDENCE.md
pause
exit /b 0
