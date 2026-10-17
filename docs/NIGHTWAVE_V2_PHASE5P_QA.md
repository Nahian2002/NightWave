# NightWave 2.0 Alpha 5P — Safe Up Next gestures

**Version:** `2.0.0-alpha5p` (code **32**)  
**Branch:** `feature/nightwave-v2-phase5p-safe-queue-swipes`  
**Status:** Draft source implementation; not yet a verified Android APK.

## New: swipe away upcoming tracks

- Open **Now Playing → Up next**, then swipe **left** across an upcoming public-video song or tap its explicitly labeled **Remove** button.
- The row reveals a restrained destructive-action background. Only future queue entries are removable; the **currently playing and previously played** songs are locked.
- A temporary **Undo** snackbar can restore the removed queue entry in its original position.
- The Remove/Undo UI calls the actual Media3 playback service via explicit, component-targeted Intent actions. It does not rewrite saved playlists, favorites, recent plays, imported audio, app-private files or their storage schema.
- Each Remove request includes the absolute index, exact public watch URL, and a random one-time UUID token. The service refuses stale positions, mismatched URL, current/past tracks, offline playback and shuffle mode. Undo is one-shot, tied to the last accepted removal and guarded by the original playing URL and queue ordering.
- Queued removal is disabled while **Shuffle** is enabled, because visible list order is not reliable as the next playback order. Imported offline audio is also protected from swipe removal; it remains accessible as before.
- Background advancement/Next and other playback queue operations invalidate previous Undo tokens to prevent an older action restoring a track into a different session.
- The Activity schedules two delayed queue snapshot reads after a command because Android's Service Intents are asynchronous. It does not pretend that dispatch alone proves the command succeeded. The existing periodic full reconciliation remains.
- Reduced Motion disables the swipe-row settling animation; every entry remains tappable to play, and the explicit 48dp Remove action remains accessible.
- The Up Next list reserves space for snackbar feedback even with longer queues. Its mini-player and full-player swipe-up/down/left/right behavior is unchanged.

## Validation achieved

- The pure Kotlin queue removal/restoration implementation (matching `PlaybackQueue`) was compiled with the container's Kotlin compiler; **19 isolated behavior assertions passed** for future-only removal, original order restoration, current-song preservation, wrong/stale requests, shuffle guard, changed current song, one-time restoration, history remapping and 100-entry capacity. This does **not** exercise Android Service intent delivery.
- Added eight repository JVM JUnit tests in `PlaybackQueueUpcomingTest.kt` (not run through Gradle here).
- Added two Android Compose UI tests in `NightWaveQueueSwipeInstrumentedTest.kt` for explicit Remove + Undo and swipe-left while protecting currently playing, using a fabricated, in-memory queue (not run here).
- Added a source-only regression gate `tools/verify_v2_phase5p.py` to verify service/bridge/UI wiring, read-only boundaries, Android version consistency and unchanged storage.
- **No Android SDK/Gradle build, Media3 service runtime or phone test was performed** here. Do not merge this as a release candidate without them.

## Windows and phone QA

1. Download full Phase 5P source ZIP and extract to a new folder. Run `build-windows.cmd`; require SUCCESS, code **32**, signer certificate and APK SHA256.
2. Run `check-device-update.cmd` with only your authorized phone connected before any in-place APK update. Never uninstall or clear data to bypass a certificate mismatch.
3. On a disposable emulator, execute the JVM tests and `connectedDebugAndroidTest`, including the new isolated Up Next UI tests and existing launcher/swipe tests.
4. Play an online song from a queue of four or more, open Up Next, and swipe-left on a **future** entry. It should disappear without stopping or restarting the currently playing track. Verify Undo restores it in position.
5. Tap the Remove icon instead of swiping, then Undo. Confirm the same result and TalkBack accessibility description.
6. Quickly switch to Next during Undo, then attempt Undo; it should safely refuse to resurrect an obsolete entry. Repeat with a replaced queue and a changed currently playing song.
7. Enable Shuffle and reopen Up Next: removal must be unavailable. Test offline imported audio: no swipe removal, playback/files preserved.
8. Turn on Reduced Motion and try short/long swipes; short accidental drags must settle back, long left swipes remove once, right swipes do not remove.
9. Confirm Home ↔ Search ↔ Library ↔ You swipes still work but never intercept the full player or Up Next interactions.
10. Reopen the app after removing future entries; the queue remains correctly ordered, saved offline music works, media notification/Bluetooth controls respond, and crash diagnostics show no newly reproduced problem.

**5Q next:** investigate native search failure and improve the fallback shown in the screenshot. **5R:** motion/accessibility refinement and a verified Android build/device stability gate.
