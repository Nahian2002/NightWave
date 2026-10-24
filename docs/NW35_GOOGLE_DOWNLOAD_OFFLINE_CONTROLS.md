# NightWave NW35 — Google error 8, Download Controls and Offline Playback

**Status: SOURCE REVISION — Android Gradle and phone tests pending.**  
**Base:** NW34 + separate public-preparation documentation branch.  
**Release status:** NOT CLEARED for public distribution. The YouTube offline-saving experiment still has platform/recording-rights blockers.

## Real user observations (on previous NW34 source)

- Windows Gradle `:app:compileDebugKotlin`, `:app:compileDebugAndroidTestKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug` completed with `BUILD SUCCESSFUL` (58 tasks). Debug APK integrity and signer certificate were checked. No NW35 APK has been built or device-verified.
- Google chooser appears but authorization returns Play services **INTERNAL_ERROR (8)**. User reports Google account never connects.
- Audio can be saved and played offline on user's Android phone, but downloads feel very slow and have inadequate controls.
- User wants cleaner offline audio playback and library navigation.

## Google Drive error 8: what the code can and cannot repair

`CloudAuthorizationDiagnosticPolicy` now provides precise user-facing guidance for status 8: retry, update/enable Play services, check real Android OAuth client package + SHA-1, enable Google Drive API, use `check-google-oauth.cmd`. Internal error 8 is ambiguous, not evidence of a bad Google account. Code still verifies the actual Drive account via API rather than trusting an empty/deprecated legacy chooser result, and does not store tokens or bypass Google's consent.

**Exact locally uploaded debug APK signer, independently read from v2 Android signature:**
- Application ID: `com.nightwave.player`
- Signer SHA-1: `11:E8:90:4D:6E:47:FC:55:16:A4:C4:D4:5D:9C:38:96:35:CA:43:A9`
- Signer SHA-256: `EA:73:7A:70:3B:8F:11:6E:BC:5C:8C:0A:71:7D:DB:F3:A2:A5:80:8A:CC:90:79:A8:F7:9C:D1:B3:CD:86:03:94`
- Certificate subject: `CN=Android Debug,O=Android,C=US`
- These only identify the **uploaded debug APK**; signing identity changes with new build machines/keys and differs from future release/Play App Signing credentials.

**Owner-required Cloud actions:** in <https://console.cloud.google.com/apis/credentials>, select the correct Google Cloud project, configure an **Android** OAuth client with the exact package/SHA-1, enable Google Drive API, and configure Google Auth Platform Audience/Test users if the app is in Testing. An Android app **cannot** register or approve its own OAuth client; the existing source change improves diagnosis, but status 8 may remain until the external configuration/device issue is resolved. Do not give the app a client secret or upload a private signing key.

Official Google references:
- https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes
- https://developers.google.com/android/guides/client-auth

## Downloads: what changed

- New **Pause** action on *new-style searched-track* jobs that have a safely bounded 11-character video ID tag. Pause triggers a clean WorkManager cancellation, removes temporary pending audio, and persists only a private paused-work UUID marker.
- On a paused/cancelled song, **Resume save · Restart transfer** explicitly requests a fresh WorkRequest for the *same search result*, with a consent dialog. **NO byte-range resume**: earlier bytes are deliberately discarded to avoid unsafe append, source-url expiry or format corruption. Other direct/catalog sources still retain their previous Cancel/navigation controls; pausing those jobs is not implemented in this phase.
- **Cancel** is separate from Pause and clears the user's paused marker.
- The Downloads list now estimates **observed KB/s or MB/s**, and if the source advertises a total length, estimated remaining time based on recent WorkInfo progress increments. If no progress or size is known, do not invent a speed/ETA.
- Transfer buffer changed **32 KiB → 128 KiB** to reduce per-chunk overhead on authorized audio endpoints. Total per-call deadline changed **8 → 25 minutes** to allow slower finite transfers. This cannot defeat provider/source throttling or improve a network-limited connection; per-read deadline, HTTPS/security checks, 250 MiB cap, media-track validation and private-library file checks remain.
- No parallel segmented downloads, extractor bypass, DRM workaround, proxying or authentication-token forwarding was introduced.

## Offline Library playback

- Added **Shuffle all** for current search/sort view; all playback remains in the app-private library.
- Added compact **Previous / Play-Pause / Next** shortcuts reusing the existing Media3 playback service, notification and offline queue. They do not initiate YouTube network access.
- Added a guarded `ACTION_TOGGLE_OFFLINE_PLAY_PAUSE` that affects only an already active *offline* Media3 item, never unexpectedly starts public streaming.
- Existing Search, Resume offline session, Play all, lyric import, delete, import, cloud locker, playlists and sorting remain preserved.

## Local acceptance checklist (Windows + existing phone)

1. Extract full NW35 source, run `build-windows.cmd`, require `BUILD SUCCESSFUL` for all three Gradle checks and `assembleDebug`. NW34's build pass does not prove NW35 compiles.
2. Run `check-device-update.cmd`. Never uninstall existing NightWave/clear storage to bypass signing continuity.
3. In a new searched-track save, open Downloads; confirm progress, speed estimate, Pause confirmation and state. Tap Resume: it should start **from zero**, not append partial data.
4. Tap Cancel instead of Pause: should remain cancelled with no Resume marker. Force-stop/restart and confirm a paused search task remains recognizable.
5. Save an authorized audio file, then use Offline Library Play all, Shuffle all, Previous, Play/Pause and Next with Airplane mode enabled. Confirm queue identity and no network required.
6. Test 2+ offline tracks, repeat modes, imported tracks, large font, back navigation and app reopen/resume without losing your existing songs.
7. Try Google connection after registering the exact current APK Android OAuth client; capture **numeric status and safe diagnostic summary** if status 8 persists. Don't share access tokens or private credentials.
8. Test slow/wobbly networks, source 403, unknown Content-Length, 250 MiB cap, storage pressure, WorkManager cancellation and duplicate user actions.
9. Regression-test previous NW34 YouTube search, local file import, Drive data safety, playlists, notification control and offline file deletion.

## Verification caveats

New JVM source tests: `OfflineTransferRatePolicyTest` and `CloudAuthorizationDiagnosticPolicyTest` updated; new Android instrumentation test: `SearchOfflinePauseStateInstrumentedTest`. These tests are **committed, not Android-executed** in this GitHub editing session. The official Windows build and on-phone tests remain necessary. No GitHub Actions triggered.
