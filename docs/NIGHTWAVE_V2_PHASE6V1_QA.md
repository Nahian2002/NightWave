# NightWave Phase 6V1 — Google Drive Receipt Validation and Build Stabilization

Stacked on 6V. This phase is deliberately focused on verification and data integrity, not new UI or music syncing.

## Implementation
- Extracted Google Drive upload final-response verification into the small, deterministic LockerDriveReceiptPolicy. The production uploadCompletion function now passes the Drive JSON response to the policy after requiring HTTP 200 or 201.
- The response is accepted as a new cloud upload only when Drive file ID, folder ID, original private song ID, sanitized filename, MIME, exact byte count, stored app-specific SHA-256 and Google-computed MD5 match the original imported audio. Missing fields or mismatches fail closed and cannot report 100 percent uploaded.
- Added 18 pure JUnit test fixtures that use synthetic JSON and a synthetic offline track, covering normal completion and missing/mismatched IDs, folder, content hashes, filename, type and size. No actual songs, user accounts, OAuth tokens or Drive calls are involved.
- Updated the earlier 6R1, 6R3B and 6V source assertions to verify the new centralized receipt policy rather than expecting duplicate validation code inside the HTTP client. Added a dedicated Python 6V1 regression script wired into the Android GitHub workflow.
- Existing upload retries, download resume checkpoints, optional LRC backup, playback, app permissions and installed app identity are unchanged.

## Actual local verification
In the current isolated development container, the exact pure Kotlin upload-resume policy body was compiled with a local Kotlin compiler using small offline stubs and passed 10 runtime assertions. The new isolated receipt-policy Kotlin source also passed syntax/type compilation with minimal local JSON/Audio stubs, plus 8 runtime assertions using synthetic JSON objects (one valid result and seven deliberately mismatched completion receipts).

These are deliberately **narrow** checks; they do not execute Android Gradle, the repository's full JVM test suite, the real org.json library, Google OAuth, Drive network requests or APK signer verification. The 18 JUnit cases are authored but not executed by Gradle.

## Remaining build blocker
The GitHub Actions job still finishes before runner allocation (zero steps). It is not possible to claim successful Android compilation or working cloud transfer from source checks. No new APK was generated, no phone installation was touched, no Spotify-style music features were added.

## Safe build next step
When a Windows build is available, use the latest branch's existing build-windows.cmd. The 6P/6V source preflights run before Gradle and the actual Gradle command compiles the app, runs unit tests and compiles Android test sources. The same builder checks Android version/package and APK signing continuity. Do not install a differently signed APK over the phone's existing NightWave app, and do not delete local playlists, songs or lyrics to force an update.
