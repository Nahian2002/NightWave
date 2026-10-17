@echo off
setlocal
cd /d "%~dp0"
echo.
echo NightWave - zero-billing local Android APK build

echo This build runs on your Windows PC, NOT on GitHub's paid runners.
echo.
echo Checking PowerShell build script syntax...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\check-builder-syntax.ps1"
if errorlevel 1 (
  echo.
  echo BUILD CANCELLED. The PowerShell build script has a syntax error.
  echo No Gradle tasks were run. Please download the newest repository ZIP.
  echo.
  pause
  exit /b 1
)
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build-windows.ps1"
set "BUILD_EXIT=%ERRORLEVEL%"
echo.
if not "%BUILD_EXIT%"=="0" (
  echo BUILD FAILED. No new APK was verified.
  echo An earlier successful APK, if present, was preserved as:
  echo   dist\NightWave-debug-previous.apk
  if exist "%~dp0dist\NightWave-build-status.txt" (
    echo Build status: %~dp0dist\NightWave-build-status.txt
  )
  if exist "%~dp0dist\NightWave-build-errors.txt" (
    echo.
    echo Error report: %~dp0dist\NightWave-build-errors.txt
    echo Full log:     %~dp0dist\NightWave-build-full.log
    echo Please send NightWave-build-errors.txt so the failing Kotlin lines can be fixed.
  ) else (
    echo The build stopped before Gradle could create an error report.
    echo Please share the earliest red error above.
  )
) else (
  echo BUILD SUCCESS. Verified build 26: dist\NightWave-debug.apk
  echo See dist\NightWave-build-status.txt for version and SHA256.
)
echo.
pause
exit /b %BUILD_EXIT%
