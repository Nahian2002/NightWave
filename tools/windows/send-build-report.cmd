@echo off
setlocal
for %%I in ("%~dp0..\..") do set "PROJECT_ROOT=%%~fI"
cd /d "%PROJECT_ROOT%" || exit /b 1
echo.
echo NightWave - create one privacy-filtered build diagnostic report
echo This reads only existing build status/logs inside this ZIP's dist folder.
echo It does not use ADB, access music, install anything, or send data online.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\tools\diagnose-build.ps1"
set "EXIT=%ERRORLEVEL%"
if not "%EXIT%"=="0" (
  echo Report creation failed. Your original build logs have not been changed.
  pause
  exit /b %EXIT%
)
echo.
echo Please send this ONE text file:
echo   %PROJECT_ROOT%\dist\NightWave-build-diagnostic.txt
echo.
echo Note: It may still include build file names or public dependency URLs.
echo Review it before sharing. Never send your private key or passwords.
echo.
pause
exit /b 0
