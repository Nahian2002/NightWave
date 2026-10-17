# NightWave Phase 6K — Safe Service-Owned Sleep Timer

**Draft branch:** `feature/nightwave-v2-phase6k-service-sleep-timer` stacked on user-tested 6J. This phase deliberately excludes speculative equalizer/crossfade/gapless changes. Existing Media3 playback, users' imported offline files, queues, lyrics, profile, listening-memory consent, Drive backup format, APK package/version and signing are unchanged.

## What was implemented

- **Now Playing > Sleep timer**: visible button with four explicit choices (15, 30, 45, 60 minutes) and a Cancel action if active. The display updates approximately every five seconds while the Activity is open.
- The **Media3-owning PlaybackService** executes the timer using `SystemClock.elapsedRealtime()`, not the Activity lifecycle, so normal home/app switching does not discard a pending timer while the service remains alive.
- The service keeps a **generation-fenced, single Handler callback**. Changing or cancelling a timer removes the prior callback; invalid minute values do not replace a valid timer. Expiry pauses both public-video and imported-local playback, clears `playWhenReady`, flushes the existing playback bookmark, and invalidates any pending public stream lookup that could otherwise autoplay later.
- A guard prevents an already-posted `STATE_ENDED` callback from starting the next song after timer expiry. The guard clears when playback genuinely begins again, allowing manual resume.
- A tiny, private **ephemeral status preference** holds only a monotonic deadline so the visible Activity can render remaining time without becoming the scheduling owner. On service creation or destruction it is cleared; there is no music ID, history, network upload, cloud schema or additional permission.
- **Graceful scope:** timer duration is not persisted across **service destruction, process death or device reboot**. It does not schedule an exact alarm or guarantee second-accurate expiry if Android suspends the process. This avoids new alarm permissions and false promises.

## Non-destructive acceptance / risk review

1. Build full branch: run `build-windows.cmd` on the complete 6K ZIP, verify actual `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`; do not mistake source gates for an Android APK test. If it fails run `send-build-report.cmd` and review `dist/NightWave-build-diagnostic.txt`.
2. Verify Now Playing has **Sleep timer**, choose each preset; status should reflect remaining time, tick down with screen on, and permit Cancel. Setting 30m after 15m replaces rather than stacks the old timer.
3. Test public and imported offline tracks while music continues after leaving the Activity/switching apps. At expiry, audio pauses **without deleting queue, liked music, bookmarks, lyrics or imported files**. Press Play manually and verify the song may resume.
4. Test near a natural track boundary and during a slow public stream lookup; after timer expiry there must be **no automatic next track or delayed stream autoplay**.
5. With timer active, force-close the service/process and reopen NightWave: old timer must not be resurrected as if still armed. No silent playback should start on service recreation.
6. Test invalid intent values, repeated cancellation, repeated timers, rapid navigation, UI rotation, TalkBack, light/AMOLED, large fonts, portrait and landscape. Touch targets and previously implemented mini-player X/swipe gestures must remain correct.
7. Verify the previous 6J metadata display, creator shelf, playlists, opt-in/erase listening memory, queue, music-source switching, offline LRC lyrics, cloud backup/merge and notifications remain unaffected.
8. **Device upgrade safety**: check package and APK signing via the read-only `check-device-update.cmd` before installing on the existing phone. Never uninstall or clear music data to bypass signature mismatch.

## Verification truth

- Pure policy tests: eight new `SleepTimerPolicyTest` JUnit methods authored for allowed presets, monotonic clock semantics, invalid values, overflow and remaining-time display.
- Source regression gate: `tools/verify_v2_phase6k.py` checks service ownership, race protection, callback cancellation, unchanged Media3 owner, UI wiring and application identity. Staged after inherited gates.
- **Full Gradle/Android compilation and physical-device timer accuracy must still be tested** on this branch. The user's completed testing of Phase 6J does not establish 6K timer reliability.
- Equalizer, crossfade, gapless, external audio DSP, alarm-exact scheduling and Spotify-like recommendations remain out of scope. Implement them in a later phase only if device and architecture evidence justify the complexity.
