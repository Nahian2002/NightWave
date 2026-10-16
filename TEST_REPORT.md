# NightWave v0.8 — Verification and open issues
Date: 2026-10-17

## User-verified evidence
- An earlier NightWave APK built successfully on Windows.
- Direct YouTube link playback and screen-off listening passed on a Samsung phone.
- Native keyword search (v0.5/v0.6) returned no usable content: NewPipe search reported raw=0, errors=4.
- v0.7's additional public community search providers also failed on the user's device. Further retries alone are not a reliable solution.

## Changes uploaded in v0.8
- Direct YouTube website results shown using an Android WebView when pressing NightWave's main Search arrow.
- The input text is URL-encoded as an official YouTube search query; no API key or Invidious service is needed for this path.
- Main-frame YouTube watch/shorts/live video navigation routes a selected video back to the existing NightWave player.
- DOM click listener captures JavaScript navigation on video links; browser exposes **no** JavascriptInterface bridge.
- **▶ PLAY** manual button selects the current YouTube video if a click is not intercepted.
- **↗ BROWSER** fallback opens the same search in an external app, from which the video can be shared to NightWave.
- Home mood cards now use the same website-based search.
- Existing native NewPipe/Invidious result lists are demoted to clearly labeled experimental mode.
- WebView lifecycle cleanup added, non-YouTube navigation restricted, third-party cookies disabled, file/content URL access disabled.
- Version bumped to 0.8.0, versionCode=8; same applicationId so in-place upgrade is intended.

## Checks done
- Static review of the connected GitHub repository's Android view IDs, search routes and media service.
- Kotlin 1.9/JDK 21 standalone smoke compilation: PASS (14 assertions for query encoding, public video URL selection, and rejection of dangerous/other-host URLs).
- Live YouTube WebView rendering: **NOT TESTED** here.
- Full Android Gradle build: **NOT RUN** here.
- Click-to-player and locked-screen playback regression: **NOT TESTED** on this version.
- Existing queued and local library code: unchanged.

## Acceptance steps on Windows and Android
1. Download a fresh NightWave repository ZIP and extract to a NEW folder. Do not reuse the old v0.7 directory.
2. Run build-windows.cmd. It runs Android JVM tests and :app:assembleDebug; wait for BUILD SUCCESS.
3. Install dist\NightWave-debug.apk over your existing version without clearing app data.
4. Search 'saiyara' from the search bar and press the main arrow. Confirm an embedded YouTube results website appears.
5. Tap a real video thumbnail; if it opens inside the browser, press ▶ PLAY. Verify NightWave audio plays with screen off.
6. If YouTube blocks the embedded WebView, tap ↗ BROWSER. Choose a video and Share → NightWave; report any failure.
7. Test Favorites, Library, queue, Bluetooth controls, and history.
8. If compilation fails, send the first e: Kotlin compiler error or Gradle FAILURE lines; DO NOT uninstall the working app.

## Remaining limitations
- Google's YouTube site may block an embedded WebView or use DOM navigation not intercepted by this prototype.
- Search browsing is not a native Spotify-style song index: it opens YouTube's own actual results, intentionally.
- Stream resolution for a selected video still relies on NewPipe, and not every video works.
- The alternative public API endpoints are not reliable enough to promise automatic keyword result lists.

**Status: v0.8 source uploaded; Windows compile and phone acceptance pending.**
