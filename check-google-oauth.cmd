@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\check-google-oauth.ps1"
echo.
echo The report (if successful) is in dist\NightWave-google-oauth-setup.txt
pause
