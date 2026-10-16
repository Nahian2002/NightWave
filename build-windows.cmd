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
  echo BUILD FAILED. Review the error above. No APK was produced.
) else (
  echo BUILD SUCCESS. See dist\NightWave-debug.apk
)
echo.
pause
exit /b %BUILD_EXIT%
