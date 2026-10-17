# NightWave 2.0 Phase 2 — Playback UI and QA

Status: **source implementation complete on feature branch; Android compilation, live streaming, screen-size screenshots, and phone tests PENDING**.

Base: \`feature/nightwave-v2-phase1-foundation\`. Target pull request: this branch → Phase 1, NOT main.

## Scope implemented

- A dedicated **StudioPlaybackBridge** owns a Media3 \`MediaController\` bound to the existing foreground \`MediaSessionService\`.
- Android's existing playback service remains the only player. It handles NEW \`ACTION_PLAY_QUEUE\` atomically: save the playlist, keep the chosen index, resolve the video on the worker thread, and play using the service.
- A pure **QueueSelectionRules** validator rejects malformed links, removes duplicates, caps to 100 tracks and preserves the specifically selected item even when it is beyond the cap.
- Native Compose floating mini-player with real artwork, current song metadata, play/pause, next and playback progress.
- Immersive full-screen Now Playing: artwork, seek slider, elapsed/duration, shuffle, repeat off/all/one, previous/next, like and save, plus a real Up Next bottom sheet and track jumping.
- Home, Search, Library and Profile remain responsive Material3; app content is centered with an 840dp tablet width cap, full player art is capped at 430dp, and safe-window insets are observed. Reduced motion disables the vertical expand animation.
- Library supports actual local playlists, create/rename/delete, favorites, recent history and per-track actions (like, add to queue, add to playlist, play).
- Search opens the existing YouTube website browser **within the Compose session**. A selected video URL is passed to the Media3 service. The web-based search mechanism remains dependent on live upstream behavior.
- Legacy launcher/activity, original saved library, and public-link player have **not been removed**. Google sign-in / Drive backup are not yet implemented.

## Verification completed

- GitHub inspection confirmed source files and manifest references.
- Source checks: no duplicate imports, Kotlin brace counts balanced, the service continues to use \`Player.STATE_ENDED\` and \`MediaSessionService\`, in-place update applicationId maintained.
- Five new unit tests uploaded for 100-item cap, selected index, source order, invalid URLs and deduplication.
- Static regression verifier \`tools/verify_v2_phase2.py\` added (must be run locally).
- **No Android/Compose compilation happened here**. Do not claim that the new UI runs until Windows Gradle passes.

## Windows build gate

1. Download the branch ZIP via GitHub. Extract into a **new directory**.
2. Do **not** uninstall the old NightWave app.
3. Run \`build-windows.cmd\`. Its tasks are \`:app:testDebugUnitTest\` and \`:app:assembleDebug\`; it does not use GitHub-hosted runners or billing.
4. For optional extra validation (when Python 3 is installed), run \`python tools/verify_v2_phase1.py\` and \`python tools/verify_v2_phase2.py\`.
5. Only after \`BUILD SUCCESS\`, install \`dist/NightWave-debug.apk\` over the existing debug APK from the same signing environment. VersionCode=9, versionName=2.0.0-alpha2.
6. Open NightWave Home and tap **Explore the new premium interface**.

## Real Android acceptance tests

- Home/Search/Library/Profile open; themes, avatar and guest name persist after restarting.
- Screen sizes: test 360dp, 412dp, 600dp and 840dp layouts if available; check clipped titles, app bars, album-art ratio, text resizing, nav bars and insets.
- Search "saiyara": YouTube search dialog opens. Selecting a public video dismisses it and eventually shows actual title/artwork in the v2 mini-player. If the site doesn't load inside WebView, do not mark search as fixed.
- A saved playlist of three public videos should play with screen locked and move to the next track. Verify Bluetooth play/pause/next and notification controls.
- Tap mini-player → full-screen Now Playing → seek, play/pause, next/previous, queue sheet, select queue entry, repeat and shuffle. The audio must **not** restart when merely opening or closing UI.
- Save favorite, create/rename/delete playlist, add a song from the track menu and reopen the app. No duplicate songs should appear.
- Rotate phone, background app, resume, kill UI Activity while music plays. Playback service should persist without an extra ExoPlayer.
- Test reduced motion, large font, TalkBack, airplane mode, invalid YouTube URL, and phone call interruptions.
- If a Gradle Kotlin error occurs, report first \`e:\` diagnostic; if a runtime crash occurs, report filtered \`adb logcat\` data (omit credentials and account identifiers).

## Known risks

- The old YouTube/newpipe search/extractor paths have failed on the user's phone before. A new player interface **does not fix upstream search**.
- Not tested on a physical Android device. API changes, permission behavior, Compose version mismatches and layout issues can only be resolved after compilation and test evidence.
- Media3 is controlled via async session connection; reconnection and failures need on-device stress testing.
- Phase 2 is a draft and must not be merged into main until Phase 1 and Phase 2 compile and pass acceptance.
- Profile/guest settings remain local only, Google Drive backup requires explicit OAuth project setup later.
