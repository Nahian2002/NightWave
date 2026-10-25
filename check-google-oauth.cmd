@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\check-google-oauth.ps1" -SelfTest
if errorlevel 1 goto :failed
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\check-google-oauth.ps1"
if errorlevel 1 goto :failed
echo.
echo Google OAuth report: dist\NightWave-google-oauth-setup.txt
echo Use the PACKAGE and SHA-1 in YOUR Google Cloud Android OAuth client.
goto :done
:failed
echo.
echo Google OAuth report INCONCLUSIVE. See the console message.
echo Never use an APK from a failed or older build to register Google OAuth.
:done
pause
