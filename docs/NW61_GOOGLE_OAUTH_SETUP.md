# NW61 — Reproducible Google Drive OAuth diagnostics

**Parent:** `feature/nightwave-nw60-offline-playback-recovery`  
**Branch:** `feature/nightwave-nw61-google-oauth-diagnostics`  
**Status:** Diagnostic source changes committed. **Google error 8 is NOT verified fixed.**

## The confirmed source-level diagnostic problem

The existing `check-google-oauth.cmd` / `tools/check-google-oauth.ps1` script assumed Android Build Tools `35.0.0` and picked a preexisting `dist/NightWave-debug.apk` (or raw Gradle APK) even after a later build failed. Such a report could show a certificate from the **wrong APK**, misleading Google Cloud OAuth configuration and leaving the status-8 investigation unresolved. The build already writes a fingerprint and explicit success/failure into `dist/NightWave-build-status.txt`.

## Changes in NW61

- A successful `build-windows.cmd` result and an exact `SHA256` hash match with the candidate APK are now **required** before reading its package name and certificate. Manual `-ApkPath` overrides cannot bypass this.
- SDK Build Tools are discovered from NightWave's local SDK, Android Studio SDK and `ANDROID_SDK_ROOT` / `ANDROID_HOME`. The checker picks the newest complete installed tools (both `apksigner.bat` and `aapt.exe`), without requiring a particular version.
- `apksigner verify --print-certs` and `aapt dump badging` must both pass, with the expected `com.nightwave.player` package and exactly one signing certificate. A certificate mismatch, missing APK, failed build or missing tools yields **INCONCLUSIVE**.
- If verification fails, the script overwrites any old report with an **INCONCLUSIVE** notice instead of leaving a stale trusted-looking signer fingerprint in place.
- PowerShell self-test, Windows launcher, cumulative NW59 source gate, builder syntax check and Python/CI regression tests check these requirements. No Google credentials, OAuth access tokens, email, app private data, music files or Android-device data are read or changed.
- The app's `drive.appdata` scope, Google native authorization API, Drive endpoint, audio service and saved library remain unchanged.

## Exact user workflow after a successful NW61 build

1. Extract the **NW61 complete ZIP** into a fresh Windows folder; run `build-windows.cmd`. It must finish with **SUCCESS** and a freshly verified APK.
2. Double-click `check-google-oauth.cmd` in the same extracted folder. It runs the self-test and checks **that exact APK**. On success, see `dist/NightWave-google-oauth-setup.txt`.
3. In your own Google Cloud project under Google Auth Platform > Clients, find/create an **Android** OAuth client. Its package must equal `com.nightwave.player` and its signing SHA-1 must match the report **character-for-character**. Ensure the Google Drive API is enabled in that project and your account is a test user if OAuth's audience is in Testing.
4. Run `check-device-update.cmd` before installing to confirm that the new APK signer matches the installed app. Do **not uninstall NightWave or clear offline music** to fix OAuth.
5. Open NightWave > You > Google connection on the phone, attempt once, and use **Copy Google setup report** if it still fails. That safe on-device report provides the **installed** APK signer, Play Services status and last failure stage/code without account secrets. Compare the *installed* signer to the Windows candidate and Google Cloud client.
6. If Google still returns code 8, do **not** claim success or begin arbitrary retries. It may be Play Services/internal or Android OAuth client configuration. Continue only after the verified report is compared to the external Google Cloud setup.

## Why this is not a guaranteed code-only OAuth fix

Google's native `AuthorizationClient` can fail before or after an external account chooser. Google Drive's `about.get?fields=user(emailAddress)` accepts `drive.appdata`, so adding broader scopes is not appropriate merely to address status 8.

Official references:
- https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient
- https://developers.google.com/workspace/drive/api/reference/rest/v3/about/get
- https://developers.google.com/workspace/drive/api/guides/appdata

**Still unverified:** a full NW61 Gradle build, actual Google Cloud OAuth registration, successful phone authorization and Google Drive backup readback, true offline playback, and final visual design acceptance. **PUBLIC DISTRIBUTION: NO-GO**.
