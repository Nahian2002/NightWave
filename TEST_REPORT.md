# NightWave v0.7 — QA & regression status
Date: 2026-10-17

## Confirmed on the user's device before v0.7
- A prior debug APK compiled with the free Windows toolchain.
- Direct YouTube-link streaming, locked-screen background playback and Bluetooth controls were reported working.
- Song-name searches in v0.6 failed: the screenshot showed \`videos: raw=0, streams=0, errors=4\` and \`all: raw=0\`. Filtering or ranking empty lists was not the solution.

## v0.7 changes committed
- Public community-search provider uses documented Invidious \`/api/v1/search?q=...&type=video&sort=relevance\`.
- Multiple maintained public instances from the official list, short HTTPS requests with no redirects, and strict JSON video parsing.
- Existing NewPipe video search is tried first. Empty+error returns switch to the independent provider.
- Search results cache for 10 minutes; 11-character video ID validation; public videos only; premium/paid entries filtered.
- Android JVM tests added: parser fixtures, rejecting invalid JSON, dedupe, query encoding, HTTP 503 failover with local MockWebServer.
- Home and Search now offer six genuine discovery search actions: Night Drive, Lofi, Bangla, Workout, Bollywood, Acoustic.
- Library has visual collection tiles, playlists, queue, listening history, and manual YouTube link save path.
- Search errors present concise retry text and an expandable diagnostics panel. Community-provider usage is stated in UI and README.
- Existing Media3 playback service, playlists and queue remain in place.
- VersionCode 7 / versionName 0.7.0.

## Checks performed in this environment
- Search defect diagnosis from the user's exact screenshot: completed.
- Upstream API documentation/schema and public-instance list checked: completed.
- Static inspection of the core code paths and Kotlin constructs: completed.
- Tests written in repository: YES (but not executed by Android Gradle here).
- Full Android Gradle compilation: NOT RUN HERE (no Android SDK/Gradle dependency download capability).
- Live search across community servers: NOT VERIFIED HERE (runtime has no outbound networking).
- Android UI/animation testing: NOT RUN HERE.

## Acceptance test before treating v0.7 as working
1. Download fresh GitHub repository ZIP, extract, run build-windows.cmd.
2. Ensure \`:app:testDebugUnitTest\` and \`:app:assembleDebug\` both pass. Don't uninstall your working version on failure.
3. Install new \`dist\NightWave-debug.apk\` in place on Samsung.
4. Search "Baby shark", "saiyara", "Bangla music", "Adele Hello" and test Songs/Videos.
5. Tap any real result, lock screen, verify audio continues.
6. Test mood tiles, local playlists, queue, favorites, history, and manual YouTube link saving.
7. On failure open **Connection details**; report provider errors. Logs (optional): \`adb logcat -d -s NightWaveSearch:D\`.

## Risks
- Independent public instances may be blocked, rate-limited or unavailable in a given location.
- NewPipe stream resolution is still required for playback: a returned video may be unplayable if YouTube changes playback extraction.
- Saved playlists must not be erased while installing; use in-place debug APK update signed by the same PC.
- Avoid private search text: a third-party community instance can receive search queries.

**Result: source update complete. v0.7 APK build and actual search/playback regressions PENDING.**
