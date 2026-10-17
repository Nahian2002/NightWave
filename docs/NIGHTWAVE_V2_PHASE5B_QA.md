# NightWave 2.0 — Phase 5B Search + Playback

**Version**: 2.0.0-alpha5b (code 18)
**Branch**: \`feature/nightwave-v2-phase5b-search-playback\`
**Status**: GitHub source changes complete. Android APK build, on-device testing and YouTube stream validation are **NOT verified here**. The branch must remain a draft.

## Implemented

### Search
- The premium Compose Search tab now tries **real native public video search** using the existing YouTube extractor and optional independent community provider, displaying thumbnail/title/uploader rows in a scrollable result list.
- Songs and Videos modes are selectable. Songs prioritizes music-like matches; Videos is the unranked public-video query mode. The same underlying extractor may be unavailable for both.
- Selecting a native search result sends the selected item **and the result collection** to the existing Media3 queue selection logic (max 100), enabling Next through that collection instead of a single isolated URL.
- **Open YouTube search** remains visible as an explicit fallback when native providers return errors, zero videos or do not work. The previously working WebView search and browser-share flow remain intact in classic Home/Search.
- Search is background-threaded; a newer query cancels the old UI job, and stopping the Activity cancels pending UI work. The underlying third-party HTTP request might finish after cancellation but is never allowed to replace newer result state.
- Native search sends public query text to the extractor and, if needed, community Invidious instances. It does not send user credentials.
- Native results are **not guaranteed playable**. An uploader may remove a video, region-restrict it, require DRM, or YouTube may change extraction. No DRM bypass, premium access bypass or account cookie scraping is implemented.

### Playback, queue and error recovery
- Media3 service can refresh a failed network stream URL **once** for selected transient network/HTTP errors, outside the Player.Listener callback. It never retries indefinitely. This does not guarantee that upstream YouTube stream extraction will succeed.
- A failed stream or extraction records only the public canonical video link and a bounded, safe error label. The premium player shows **Retry**. Explicit Retry resolves a fresh stream URL or retries an imported offline file.
- Stale Media3 media items are cleared when another online song is selected/resolved; the UI can display the intended saved queue entry while the new stream loads, rather than showing the previous song.
- **Previous** restarts the current track when already more than three seconds in. When close to the beginning, it navigates to the previous queued song. Offline files can restart using Previous.
- Existing repeat-one, repeat-all and shuffle behavior is retained, with new queue transition JVM regression tests.
- The playback bridge distinguishes imported offline audio from a public YouTube video: correct title/artist, no old YouTube artwork, avoids favoriting or adding private offline media to the YouTube-only playlist index. The separate Offline library continues to support imported local audio.
- Resuming the saved session after process recreation remains **explicit**, per Phase 5A. Service creation does not initiate network extraction. Headset and Bluetooth first-Play resume remains available.
- The existing NightWave branded premium UI, 60dp navigation, compact Now Playing and private crash diagnostics are preserved.

## Risk and limits
- A successful source review does **not** prove an Android APK compiles. Kotlin/Compose/Media3 integrations require a local Gradle build.
- The YouTube extractor and community search endpoints can fail independently. The embedded website fallback is deliberate; there is no guarantee YouTube will return playable audio on every device.
- This phase does **not** provide YouTube audio downloads, Google account login, Drive backups or other cloud sync.
- Local liked songs/playlists and app-private audio files have not been deleted or migrated. Stable \`main\` stays on v0.8.0.
- Prior crash reports still require the device's Android exception/ANR log; Phase 5B does not claim those reports are conclusively fixed.

## Windows build
1. Download the Phase 5B source ZIP from the branch named above, extract into a new folder.
2. Run \`build-windows.cmd\`. It builds locally and generates \`dist\NightWave-debug.apk\` **only after BUILD SUCCESS**.
3. If compilation fails, upload \`dist\NightWave-build-errors.txt\` and the relevant lines of \`dist\NightWave-build-full.log\`. Do not uninstall/clear the existing app.
4. Optionally run \`python tools/verify_v2_phase5b.py\` to verify structural source wiring. It is **not** a compiler or device test.
5. Install the debug APK as an update over the previous debug-signed install if and only if signing is compatible. Do not uninstall to work around signature errors.

## Phone acceptance checklist
1. Launch classic Home, then open the redesigned NightWave premium preview. Check that the previous visual changes remain.
2. Search "Saiyara", "Baby Shark" and a different artist via the premium Search tab. Check Songs/Videos results, thumbnails and source text. If empty/error, try the visible **Open YouTube search** button. Do not claim universal search availability from a single query.
3. Tap a middle native result; verify the selected track and queue order, not just the first result. Press Next to play the next track; use the queue sheet to jump to another song.
4. Pause/resume, seek, restart a song with Previous after four seconds, and navigate to the previous queue entry when near the start.
5. At the last song, verify repeat OFF stops, repeat ALL wraps and repeat ONE repeats on completion. Shuffle should not replay the same song immediately when other tracks are available.
6. Try an unavailable/deleted public video on a disposable queue. Verify a bounded failure message and **Retry**; verify no infinite retry or app crash.
7. Import a personally owned MP3, play it with Airplane mode enabled, check lock-screen notification, no stale YouTube thumbnail and Previous restarts the file. Confirm offline files are not written to the YouTube favorites by accident.
8. Play for 15 minutes with screen locked and Bluetooth connected; check background audio and media keys. Reopen and confirm saved queue/bookmark are intact and playback does not start automatically on app launch.
9. Test navigation, screen ratio, and no app crash warnings. If an Android crash recurs, use \`capture-nightwave-crash.cmd\` or classic Home → Diagnostics to share a reviewed report.

## Testing authored vs executed
- Three new JVM queue transition tests in PlaybackQueueTest (10 total): result-list navigation, repeat-one/manual skip, shuffle without immediate duplication.
- Two new Android bookmark/error tests (9 total): failure scoping and rejecting private signed audio links.
- Existing Phase 5A, offline file, profile corruption, Room shadow and web-route tests remain present.
- **None of these new tests have been executed in this turn.** Full Gradle compilation, Android instrumentation and phone testing are pending.

## Release gate
Keep draft PR open until the user reports **BUILD SUCCESS**, test results, and—if errors recur—actual crash traces. Do not merge this work to \`main\` or replace the last working APK merely because source assertions passed.
