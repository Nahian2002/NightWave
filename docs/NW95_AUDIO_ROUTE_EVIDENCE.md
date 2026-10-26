# NW95 — Headset / Audio-Focus Transport Reliability

**26 October 2026** · cumulative NW94 base `5facd671517ec45cf6713edfa611034e9856c314` · branch `feature/nightwave-nw95-headset-media-focus-reliability`.

## Confirmed defect

NightWave previously intercepted `KEYCODE_MEDIA_PLAY`, `KEYCODE_MEDIA_PLAY_PAUSE` and `KEYCODE_HEADSETHOOK` identically whenever the public queue was selected but no Media3 `MediaItem` was loaded. Such a state is normal during audio URL extraction. Pressing a headset's Play/Pause button while the pending request intended to play always called `resumeSavedQueue()` and hence `player.play()`. The user could not use the button to pause the pending request. The late resolution callback might then start unwanted playback after an intended pause.

## Change

- New pure-Kotlin `AudioRouteTransportPolicy` explicitly distinguishes **PLAY**, **PAUSE** and **TOGGLE** actions.
- When a canonical public song is selected but no media item has loaded:
  - **TOGGLE with playWhenReady=true** -> `player.pause()`, preventing automatic play on later resolver success.
  - **TOGGLE with playWhenReady=false** -> explicit `resumeSavedQueue()`.
  - **PLAY** -> resume saved queue (idempotent for pending requests).
  - **PAUSE with playWhenReady=true** -> pause pending request; PAUSE if already paused delegates.
- Existing loaded-media and offline-media Play/Pause control delegates to Media3. No independent `AudioManager` focus request, noisy receiver, device permission or background auto-resume mechanism is added.
- Repeated KeyEvent ACTION_DOWN events from long-press are not treated as repeated Next/Previous/Play/Pause by NightWave; platform handles unconsumed repeats.

## Local tests and source checks

- **12/12 pure Kotlin/JVM policy smoke scenarios actually compiled and executed** in a disposable directory with local `kotlinc` and `java`. Scenarios include pending pause, pending resume, explicit Play and Pause, loaded-media delegation, offline isolation and missing selection.
- Six JUnit unit tests added to the Android module; **execution pending** while hosted GitHub Actions runner cannot execute even the empty canary.
- New source-contract checker verifies production media-button wiring and that `setHandleAudioBecomingNoisy(true)` and Media3-managed focus remain in place.
- New NW95 GitHub workflow triggers and smoke command. **Full Android compilation, JUnit suite, Android instrumentation and Samsung/Bluetooth/headphone tests are NOT VERIFIED.**

## Preservation / limitations

No changes to Android package, signing, OAuth, persisted song IDs/queue/preferences, private offline files, cloud staging, media-source selection, equalizer or serverless architecture.

Audio focus and physical Bluetooth route switching remain handled by Media3 and Android, not tested by this standalone policy. In particular, a headset disconnect while a URL is *still resolving* is not fully simulated by these tests. No claim of flawless auto-routing is justified until wired/Bluetooth tests.

Public release remains **NO-GO** pending current Windows/Android build evidence, a signer-continuity check, production OAuth and device QA, content-source rights assessment, and Android target-API compliance.

For owner testing, download a fresh cumulative NW95 ZIP, run `run-nw91-final.cmd` for the Windows build/JUnit evidence, then `check-device-update.cmd` before installing any APK. Do not uninstall or clear music.
