# NW53 — Account connection recovery UX

**Parent:** `feature/nightwave-nw52-final-integration-release-gates`  
**Branch:** `feature/nightwave-nw53-google-recovery-ux`  
**Status:** Source implemented. **Google authorization error 8 is still unverified on a phone.**

## What changed

- The optional Google Drive account dialog now closes *before* launching a Google-owned permission/account sheet. The same applies when changing an account, and when explicitly retrying a previous status-8 request. This avoids stacking the app's modal below Google UI.
- An error-specific, credential-free help section clearly distinguishes Google's internal status 8 (which is not proof of an invalid Android certificate), configuration status 10, cancellation, network errors and HTTP failures during Drive verification.
- **Retry Google connection** is available only for potentially transient statuses; it is user-initiated, never automatic. Repeated status 8 triggers instructions to compare installed signing SHA-1/package name, Google OAuth Android client, Drive API activation and test-user enrollment.
- Existing **Copy Google setup report** still includes only installed package, public signing fingerprint, Google Play services code, failure stage and code. No access token, Google account email, Intent payload, exception text or Drive content is copied.
- Never mark the account connected until `NightWaveDriveClient.authorizedEmailAddress()` succeeds with the requested `drive.appdata` scope.
- The rest of the local music library, saved audio, consent flags and backup archive formats are unchanged. The Google feature remains optional.

## Evidence and outstanding issue

- The newest user video recorded Google **internal error (8) after account selection**. Google defines 8 as an internal Play-services error, typically retryable, and 10 as a developer-configuration error. These status codes alone do **not** identify root cause.
- A real connection requires a working OAuth client registered for **the exact certificate SHA-1 of the APK installed on that phone**, the matching package name, Google Drive API and OAuth consent/test-user settings. None can be modified or proven merely by changing this repository's Kotlin.
- `CloudConnectionHelpPolicyTest.kt` covers retry eligibility, HTTP-vs-OAuth statuses, cancellation, absent credential strings and stable read-only diagnostics.
- `tools/verify_nw53_oauth_ui.py --self-test` adds source-guard mutations for stale chooser routing, unsafe error messages, and cancelled library protection.
- Source guards do not substitute for a Gradle build or a real Google chooser/Drive readback test.

## Acceptance later

1. Install signer-compatible APK (never uninstall or clear existing local music).
2. On You > Account & backup, verify that Connect opens native Google UI *without a NightWave modal behind it*.
3. On error 8, reopen You, check the safe staged message and copy the installed APK Google setup report.
4. Retry explicitly once. If it continues, verify the actual APK SHA-1/package with the Google Cloud Android OAuth client, test user and Drive API.
5. Only when the account connects, perform backup, check verified receipt, preview a restore and disconnect without losing offline audio.

**Next:** NW54 Search clarity and result density, followed by NW55 Downloads/Offline design cohesion.
