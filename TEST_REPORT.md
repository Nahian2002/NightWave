# NightWave v0.6 — verification and regression report
Date: 2026-10-17

## Verified user observations
- Previous NightWave Android APK built successfully with the provided Windows \`build-windows.cmd\`.
- User confirmed screen-off playback and Bluetooth play/pause on Samsung S24 Ultra.
- User reported the v0.5 song-name search displays **No matches found** for \`Baby shark\`. This is a real observed defect, not a misspelled query.

## Changes committed
- Normal public YouTube video search is tried first for both Songs and Videos.
- Only after empty/unparseable results are the ALL and MUSIC_SONGS filters tried.
- Relative \`/watch?v=...\` and \`/shorts/...\` result URLs are normalized safely; other domains are rejected.
- Music matches are scored and sorted without deleting otherwise-usable results.
- Search diagnostic logs count returned elements, stream elements, parse success and errors.
- Search errors include Retry and Try Videos actions. No false "song does not exist" claim.
- New original Midnight Sessions visual identity with violet/aqua/navy palette, Home hero, music cards, full-height Now Playing, mini-player artwork and animations.
- Foreground Media3 playback service, YouTube stream resolver, saved library and queue code preserved.
- Version bumped to 0.6.0, versionCode 6.

## Source checks performed
- Current GitHub main inspected for required files, drawables and ID references.
- 34 unique view IDs; no missing ActivityMainBinding references.
- All additionally referenced drawable names found or confirmed present in the repo.
- Activity and gateway braces balanced in static source inspection.
- Search fallback order and diagnostics paths reviewed.
- Existing media-session foreground player and automatic next-song logic still present.
- Kotlin smoke-test attempt in this isolated container timed out; **not counted as passed**.

## Not verified / open risks
- Android build of v0.6: **NOT RUN HERE** (no configured Android SDK / dependency network in this environment).
- Live \`Baby shark\` search: **NOT VERIFIED**.
- UI transitions, full-screen dialog, device sizes and window insets: **NOT DEVICE TESTED**.
- Screen-off playlist auto-next after this revision: **NOT DEVICE TESTED**.
- YouTube's upstream search implementation can change; code-level fallback cannot promise results from every query.

## Test on Windows + S24 Ultra
1. Download a fresh main-branch repository ZIP, extract to a clean folder, run \`build-windows.cmd\`.
2. If compilation fails, leave the old APK installed; report first Gradle \`e:\`/FAILURE section.
3. Install newly generated \`dist\NightWave-debug.apk\` without uninstalling.
4. Search \`Baby shark\`, \`Adele Hello\`, \`lofi hip hop\` in Songs, then in Videos. Expect usable results or an explanatory error with retry options.
5. Open 3 songs, test playback, screen-off continuity, queue progression, shuffle, favorites and playlists.
6. Test all 3 navigation tabs, rotating device, screen locked, Bluetooth controls and Now Playing seek.
7. If search still fails, capture app error and \`adb logcat -d -s NightWaveSearch:D\` if available. Do not upload private tokens or device-wide unfiltered logs.

Build/release is **pending local compilation and user validation**.
