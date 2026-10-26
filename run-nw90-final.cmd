@echo off
setlocal
cd /d "%~dp0"
echo.
echo =============================================================
echo  NIGHTWAVE NW90 - CUMULATIVE WINDOWS QA / PRIVATE DEBUG BUILD
echo =============================================================
echo This will NOT install, uninstall, clear, or modify your phone.
echo No public release or YouTube offline authorization is implied.
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\nw90-windows-gate.ps1"
if errorlevel 1 (
  echo NW90 matrix/source integrity failed. No Gradle build attempted.
  echo Review dist\NightWave-NW90-gate.txt
  pause
  exit /b 1
)

echo.
echo Running the existing full Windows debug build and JVM certification...
call "%~dp0build-windows.cmd"
if errorlevel 1 (
  echo NW90 current-branch Android build DID NOT pass.
  echo Run send-build-report.cmd and share fresh redacted diagnostic only.
  pause
  exit /b 2
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\nw90-windows-gate.ps1" -AfterBuild
if errorlevel 1 (
  echo NW90 post-build integrity FAILED. Do NOT install any APK.
  pause
  exit /b 3
)

echo.
echo DEBUG BUILD/JVM SOURCE CHECK COMPLETE, but not phone certification.
echo Before updating your installed app, run check-device-update.cmd
echo to compare installed package, version and signing identity.
echo Then follow docs\NW90_EXPANDED_139_CASE_MATRIX.csv on a test device.
echo Google backup/restore, audio devices, Play compliance and NW77 rights
echo remain separate acceptance gates. Public distribution is still NO-GO.
echo.
pause
exit /b 0
