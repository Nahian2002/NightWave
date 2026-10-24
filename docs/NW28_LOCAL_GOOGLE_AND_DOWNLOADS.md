# NightWave NW28 local OAuth and authorized-audio build

This is a **source revision**, not a certified Android APK. Android Gradle
compilation, WorkManager runtime behavior and real Google OAuth consent
must pass on the user's Windows computer / Android device. GitHub Actions
are not required and must not be triggered.

## Google Drive authorization
- Existing NightWave backup uses only `drive.appdata`, not broad Drive access.
- Google's account chooser can return RESULT_CANCELED, a null result, or an
  ApiException. These are not proof that the user deliberately cancelled.
- NightWave now uses `CloudAuthorizationDiagnosticPolicy` to show safe
  numeric codes, including status 10 (Google OAuth developer configuration).
  No token, account address, exception payload or pendingIntent is logged.
- For the **exact locally built APK**, run `check-google-oauth.cmd`. This
  checks `apksigner` and `aapt` and prints its package and certificate SHA-1.
- Configure an Android OAuth client in YOUR Google Cloud project for
  `com.nightwave.player` and the reported SHA-1. Enable Google Drive API.
  If the external OAuth app is in Testing mode, add the Google account used
  on the phone as a test user. Google may require OAuth consent verification
  before publishing to general users.
- The helper is read-only. It cannot register OAuth credentials for you.
  Local debug signer changes may require another Android OAuth client.
- Test account chooser, consent, Drive account resolution, backup and recovery
  preview separately. Local music and metadata must never be deleted to
  fix a Google connection failure.

## Authorized direct-audio download
- The user explicitly confirms the right to save each direct HTTPS audio file.
- The existing direct-audio download and free-audio catalog remain available.
- NEW: **Save in background** queues a validated source with WorkManager.
  It continues after closing the Activity and can resume after a process
  restart, restarting an interrupted partial transfer from byte zero.
- Direct jobs and CC0/public-domain catalog jobs share the Downloads queue,
  including progress, cancel, failure status and Offline Library links.
- The downloader reuses existing audio signature, Android MediaExtractor,
  byte-limit, private-address DNS, redirect, duplicate-detection, SHA256 copy,
  and atomic library-index protections.
- No conversion is performed: source MP3, M4A, FLAC, AAC, WAV, OGG, Opus or
  WebM audio is saved in its validated original format, with a 250 MiB maximum
  individual source size. Background downloads have no guaranteed delivery
  time; Android battery/network constraints apply.
- This feature does NOT download, demux, transcode or bypass protected YouTube
  streams. A YouTube search result is not a licensed download source and
  Commons can return a **different** recording. The app must never falsely
  identify one recording as another.

## Windows / device acceptance test
1. Extract the complete source archive into a clean folder.
2. Run `build-windows.cmd`. Require source preflight, all Gradle unit tests,
   assembleDebug, Android test Kotlin compilation and APK signature validation.
3. Run `check-google-oauth.cmd`; compare the signer SHA-1 with the Google Cloud
   Android OAuth client, not with an unrelated keystore.
4. Run `check-device-update.cmd` with one phone. Never uninstall/reset app
   data to force a signing mismatch.
5. Test Google chooser, error reporting and a verified Drive snapshot.
6. Queue one **permitted** HTTPS direct audio track in the background,
   leave the screen, verify notification/progress/cancel and offline playback.
   Also test an invalid format, duplicate, revoked URL, network interruption,
   and library contents after app restart.
7. Test YouTube streaming separately; it remains an online player feature.

No phone or APK build was executed as part of the GitHub source edits.
