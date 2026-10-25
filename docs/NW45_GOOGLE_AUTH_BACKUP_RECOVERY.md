# NW45 — Google Drive Authorization Recovery and Backup Safety

**Parent:** `feature/nightwave-nw44-smart-search-v2`  
**Branch:** `feature/nightwave-nw45-google-drive-recovery`  
**Status:** CODE IMPLEMENTED. Google's authorization outcome and full Android compilation **NOT VERIFIED**. Device testing is deferred to the user's later combined test.

## Why Google still fails

The phone recordings showed the Google account chooser opening, followed by an **internal error 8**. Official Google Play services documentation defines status **8** as an internal error which *may* be temporary; status **10** is the more specific developer misconfiguration indicator. Neither code establishes a particular failure without logs and checking the actual installed APK.

The current NightWave code already uses Google's `AuthorizationClient` with the narrow, non-sensitive `https://www.googleapis.com/auth/drive.appdata` scope. Google Drive's `about.get` explicitly supports this scope; NightWave can use it to confirm the account associated with a granted Drive token. **This implementation does not replace a valid Google flow with deprecated GoogleSignIn or request broad Drive/Gmail/email permissions.**

Google Cloud configuration remains a separate external gate:
- Confirm an **Android OAuth client** exists in the intended Google Cloud project with exact package `com.nightwave.player` and the **SHA-1 of the APK actually installed on the phone**.
- Enable the Google Drive API in the **same** project; verify OAuth branding/audience and test-user settings where appropriate.
- Use NightWave **You > Account & backup > Copy Google setup report**, which includes installed APK SHA-1, Play services status, and last numeric error/stage; compare against Google Cloud Clients.
- Test account authorization on-device. **An account chooser alone does not confirm access.** Credentials, tokens or passwords must never be pasted into reports.

These are checks, not a claim that SHA mismatch is the cause of error 8 or that code can configure the user's Google Cloud project.

## Implemented in source

1. **Concise, staged auth errors**: `CloudAuthorizationFailurePolicy` distinguishes request failure before the chooser, account picker/consent failure, and post-grant Drive verification failure. Error 8 suggests a **single manual retry**, then Play services / project setup; error 10 explains configuration, 7 connectivity and 16 cancellation. Drive verification HTTP 401/403 is reported separately from OAuth codes. No silent infinite permission retries.
2. **Credential-free diagnostics**: `CloudAuthorizationFailureStore` holds only an allowlisted stage and bounded numeric status in private preferences. It never writes an email, access token, OAuth request Intent, signing private material or exception body. Corrupt optional prefs fail closed. `InstalledApkOAuthIdentity.report()` now includes last public error code/stage alongside actual APK signer SHA-1 and Play services status; successful connection or user cancellation clears obsolete failure metadata.
3. **Activity integration**: error callbacks update stages consistently without changing user account/library values. When Google Play services reports a user-resolvable device issue, the Activity offers Google's official resolution dialog. Selected Drive account is still verified with an HTTPS `about.get` call before local account association. Failure leaves local guest music intact.
4. **Account UI**: dynamic help shows short reason and relevant stage when available; otherwise a neutral setup introduction. The existing safe `Copy Google setup report` and `Open Google OAuth settings` actions remain.
5. **Atomically verified manual backup**: the final manual backup success now calls `CloudBackupState.successIfCurrent(consent, ...)`, which checks the exact current account, user-selected included data, Wi-Fi choice and preference revision under a shared lock. A disconnect/reconnect or privacy change can no longer be marked as an up-to-date verified backup by a late callback.
6. **Bounded worker behavior**: background automatic backup does **not** reopen Google consent. Non-recoverable Play-services errors/cancellations require interactive attention instead of pointless automatic retries; transient errors are bounded by WorkManager retry count. Existing Drive HTTP 401/403 and retry classifications remain.

## Scope intentionally unchanged

No app ID, signer, Google Cloud project/client registration, access scopes, persistent tokens, audio library writer, downloads, playback service, NW43 queue changes, NW44 search ranking, import/export schema, Drive snapshot deletion, or automatic library merging were modified.

Drive backup remains **opt-in** and backs up library metadata/selected optional preferences and lyrics **not offline audio files**. Restores continue to require explicit preview/confirmation. Local guest music remains accessible without Google.

## Tests and verification

- **Five new pure JVM tests**: distinguish codes 8/10/7/16, Drive HTTP 401/403 vs Google OAuth codes, stage-aware messages, avoid credentials and tolerate unknown stages.
- **Two Android tests** with isolated, test-suffixed SharedPreferences: only error code/stage are recorded and exposed in the copy report; malformed codes ignored.
- **Four Android first-backup tests** using isolated preferences: exact-consent first backup, outdated privacy revision, disconnect/reconnect revision and unchanged snapshot reporting.
- `tools/verify_nw45_drive.py` and its negative self-test enforce exact token-free scope, stage report, UI wiring, and atomic manual verification. Android CI branch wired.
- **Policy smoke:** compiled the equivalent pure Kotlin stage/error policy locally with `kotlinc` and executed basic error/stage checks. This does **not** compile Android code or run connected Google tests.
- **Not yet run:** Gradle build, JUnit tests, Android instrumentation, Play services on phone, Drive account grant, upload/download round-trip, consent sandbox tests on device. GitHub Actions has sometimes failed before runner allocation, with no useful Kotlin compiler result.

## Actual acceptance gate (when the user tests the combined APK)

1. Run `build-windows.cmd` and require **LOCAL BUILD SUCCESS**, fresh debug APK and all unit/Android test-source compilation. Run `check-device-update.cmd` before installing: **do not uninstall, clear data, or force a different signature**.
2. On phone, open **You > Account & backup** and copy the credential-free setup report. Compare exact package/SHA-1 with Google Cloud. Confirm Drive API and OAuth Audience for that project.
3. Try Connect Google Drive; record **which stage** the failure occurs at and numeric code if failure remains. Do not share email, tokens, or private account screens.
4. Once Google authorizes, confirm NightWave shows connected but *not backed up* until an actual verified upload.
5. Create a disposable test playlist, back up metadata, preview snapshots and compare/merge a *test* library on another test environment. Do **not** destructively replace the real library to test restore.
6. Change include-history/lyrics/preferences while a test backup runs: it must never claim the old backup matches the new choices. Disconnect and reconnect tests must leave old cloud snapshots untouched.
7. Disable network / simulate timeout / cancel account chooser. Ensure bounded retries, specific error explanations, and the local library intact.
8. Confirm search, NW43 manual Play Next, old offline tracks and background playback still work.

## Official references

- Android AuthorizationClient: https://developer.android.com/identity/authorization
- Google Play services error codes (8 = internal, 10 = developer): https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes
- Android OAuth client package and SHA-1 setup: https://developer.android.com/identity/authorization
- Drive app-specific folder and narrow scope: https://developers.google.com/workspace/drive/api/guides/appdata
- Drive about.get authorized appdata scope: https://developers.google.com/workspace/drive/api/reference/rest/v3/about/get

**Next scheduled batch:** NW46 immersive Now Playing redesign. The Google issue remains a release blocker until the signed app demonstrates a successful authorization and safe Drive backup on a real device.
