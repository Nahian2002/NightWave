# NightWave Phase 5T — Official-first discovery (source-only)

## Why this phase exists
The phone screenshot shows an ordinary keyword, "shape of you", ending in a public-provider unavailable card. The native quick results currently use NewPipeExtractor with fallback to independently hosted community endpoints. Neither is a dependable primary discovery interface. A previously implemented official YouTube in-app search WebView already exists and has a separate external-browser fallback.

## Source changes
- "Find music on YouTube" and the keyboard Search action open official YouTube search inside NightWave; they no longer wait on NewPipe/Invidious.
- Typing immediately searches liked, recent and playlist metadata locally, without any network request.
- "Try quick in-app results" explicitly offers legacy native results. If it fails, the smaller notice offers quick retry and an official YouTube route; the failure is not presented as a complete search outage.
- Home discovery shortcuts prefill the Search tab without silently contacting a provider; Search-tab shortcuts use the primary route.

- External shared plain-text searches also prefill Search instead of automatically triggering an unreliable provider; the user can choose the official route.
- Songs/Videos chips are labeled under OPTIONAL QUICK RESULTS so they are not mistaken for filters controlling Google's official search.
- Pasted valid YouTube video URLs still follow the same validated NightWave playback path; bad URLs remain rejected without contacting providers.
- No changes to Media3, app-private offline files, playlist storage, profile storage, signing or manifest permissions.
- Existing package identity/version code 35 / alpha5s is retained for this UNVERIFIED source branch pending a verified Android build and separately reviewed version bump.

## Required tests
1. Build on Windows with `build-windows.cmd`; require Gradle JVM unit tests, APK assembly and `:app:compileDebugAndroidTestKotlin` to succeed.
2. Open Search, enter "shape of you", tap "Find music on YouTube": official results should appear in the in-app browser without any public-provider error card. Check keyboard Search and two shortcut queries.
3. Test the in-app YouTube browser controls: back/close, open a valid video, external browser fallback on WebView network failure, and Share → NightWave on device. Search results do **not** guarantee that a third-party extracted audio stream will play.
4. Save a public song and verify local matching without network access. Check two searches where earlier native public results must not be displayed as results for the new query.
5. "Try quick in-app results" with a blocked provider: a compact quick-results status should appear and leave the primary search accessible. Retry should not erase saved tracks.
6. Paste a valid YouTube video URL (direct validated playback) and a malformed/unsupported URL (explanation, no provider calls).
7. Reopen the app; verify Home, four-tab pager gestures, queue removal/Undo, offline imported music, current song resume, playlists, favorites, and no startup crash.
8. Before ANY APK install-over-existing app, run `check-device-update.cmd` and compare signing fingerprints. Do not uninstall or clear existing app data.

## Current verification
GitHub source readback and isolated source assertions are meaningful but **not** a Compose compile, a real build, a successful Google/YouTube search request, or a successful playback test. The Phase 5S CI runner previously failed without usable steps/logs. Keep the Phase 5T PR draft; preserve stable main.

## Next phase 5U — opt-in Google backup
Use Google's Android AuthorizationClient and only `drive.appdata` permission, requested after explicit user opt-in. Keep a bounded/versioned archive with checksum and metadata. Preserve old backups until the next new snapshot is fully written and validated; never restore automatically over existing local data. Enable daily WorkManager scheduling only after a successful initial backup; use connected network + optional unmetered/Wi-Fi preference, retry/backoff, and clearly display last-success/last-error times. Listening history and recent searches require explicit separate opt-in. Back up song metadata, playlists, profile/settings; never Google credentials, actual audio files, playback stream URLs, or private imported-audio paths. Cloud recovery on another device requires user-confirmed preview/merge/replace choices and checks for device-local content. Drive app data may be deleted from the Drive app management UI; retain manual export as a separate recovery method.

Google Cloud OAuth setup, app signing fingerprints and a Google-enabled device are required before a working Google account connection can be verified. **Phase 5T does not claim Google sync has been implemented.**
