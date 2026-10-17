@echo off
setlocal
cd /d "%~dp0"
echo.
echo NightWave - zero-billing local Android APK build

echo This build runs on your Windows PC, NOT on GitHub's paid runners.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build-windows.ps1"
set "BUILD_EXIT=%ERRORLEVEL%"
echo.
if not "%BUILD_EXIT%"=="0" (
  echo BUILD FAILED. No APK was produced.
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
  echo BUILD SUCCESS. See dist\NightWave-debug.apk
)
echo.
pause
exit /b %BUILD_EXIT%
