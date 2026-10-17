# NightWave Phase 6D — Playback Intelligence & Reliability

**Branch:** `feature/nightwave-v2-phase6d-playback-resolve-recovery` built on 6C2. **Draft source only; Android compilation, JVM/Compose tests, emulator and phone timing tests are deferred.** Stable `main` and installed app/music data unchanged.

## What Phase 6D changes

### Cancel stale music lookups and bound extractor waiting
- Replaces a **single-thread serial stream extractor** with at most **two concurrent extraction workers and one queued request**, using a bounded `ThreadPoolExecutor`. When the user selects another track, the service cancels the previous cancellable Future, discards queued superseded requests, and increments a generation token. Only the still-selected public URL at the current generation may populate Media3.
- A monotonic **35-second extraction-only watchdog** expires a stuck lookup. It cancels/invalidates the job, clears the progress indicator and stores a local actionable error: **Audio lookup took too long. Tap Retry or choose another song.** A saturated worker pool fails visibly with a Retry message rather than queuing indefinitely.
- External extractor and YouTube stream providers are third-party dependencies. **This does NOT guarantee song playback in 35 seconds**: it stops waiting for an extraction that took too long. The later HTTP stream buffering/Media3 read timeout may still vary. Cancellation may not immediately interrupt an uncooperative third-party blocking call; strict active-worker and waiting-queue bounds limit resource growth.
- No signed audio stream URLs are persisted or recycled across restarts; fresh extraction still occurs for genuinely new public streams, explicit retry, and expiration recovery. One previous auto-recovery attempt is retained for transient Media3 network errors; no unlimited automatic retry.

### Protect the selected queue and user commands
- A resume command reuses a loaded Media3 item **only when its exact media ID matches the current saved public queue entry and the player has no error**. Otherwise it re-resolves the correct selected song and its **own** saved bookmark. It cannot accidentally replay a loaded offline/older video merely because a MediaItem exists.
- Manual **Retry** also targets the current selected queue track; it doesn't silently return if the player still has an older item, and never seeks to another track's current position.
- Rapid repeated Play/queue commands for the same resolved or pending URL remain idempotent; repeated Retry during a pending extraction cannot spawn another identical extractor task.
- A Play/Pause request while resolution is still underway updates Media3's pending `playWhenReady` intent. **If the user pauses during extraction, successful resolution leaves the new item paused**, rather than starting against that decision. A later Play resumes the pending intent without restarting the extractor.
- **Repeat One** restarts the already-loaded same item from zero and prepares locally without re-resolving a new signed URL each time. If the loaded source has expired, the inherited bounded onPlayerError stream refresh remains the fallback. Manual Next uses the normal queue logic.
- Ended or idle loaded audio that matches the expected track is prepared before Play; no mandatory extractor round-trip for an already loaded, healthy item.

### Report lookup progress and failure truthfully
- `QueueStateStore` persists a short-lived **public URL-only** `resolving_url` and an elapsedRealtime timestamp. A 35s monotonic TTL prevents permanent loading after a crash or reboot; the marker clears on completion, cancellation, timeout, service restart and switching to local offline music. This is an internal UI status, not a signed audio stream URL or listening analytics event.
- The MediaController bridge shows `buffering` if Media3 itself buffers **or** the selected public track has a currently valid pending extraction. It reads the service's actionable failure once the lookup ends.
- When the MediaItem is null but a saved public track is displayed, reconcile about every 2.25s instead of 6s, without reading preferences on every playhead tick. Ordinary active audio keeps the previous low-cost reconciliation schedule.

### Unchanged
- Media3 remains the only player; background playback, notification transport, queue/Undo, offline imported audio, 5Y safe Home resume, 6A2 mini-player X and swipe, 6B private LRC lyrics, 6C/6C2 opted-in memory and trends, Google Drive backup and 5Z manual merge stay.
- No new permission, app ID/version/signing, library migration, cloud export schema, logged sensitive URL, audio file deletion, or new background Android service.
- The queue still limits public tracks to 100, keeps title/metadata and bookmarks locally, and does not autoplay on service startup.

## QA to run later, not yet executed

1. Run the new `tools/verify_v2_phase6d.py` and inherited static gates, and compile `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` via Windows builder. The source gate is **not** proof of Kotlin compilation.
2. Execute `PlaybackResolutionPolicyTest` (five pure cases), updated `PlayerSnapshotPolicyTest`, and emulator-only `QueueLookupIndicatorInstrumentedTest` (three cases). Do not run the latter on a primary phone; it intentionally skips physical devices before modifying transient test prefs.
3. Simulate a 50s public extraction stall: the user should see pending state, then a Retry-able failure after ~35s, with no hang/indefinite spinner. Clicking Retry starts a new extraction; a double tap does not generate duplicate concurrent resolutions. Measure actual startup time separately from extractor timeout.
4. Begin A while slow, rapidly select B then C. Only C may load/play; stale extractor callbacks must not replace C, corrupt the queue, change the bookmark, or cause unbounded worker growth. A and B may continue blocking in uncooperative extractor code, but only two requests run simultaneously and a queue slot is bounded.
5. Begin a delayed song and tap Pause: the resolved song stays paused. Tap Play during a pending extraction: pending intent resumes without duplicate lookup. Test same-track repeated taps and disabled airplane mode/network.
6. Test public queue saved bookmark, restoring after cold activity reopen and process death (without autoplay); ensure the selected song matches the restored Media3 ID. Switch from local offline to public and back; never play an unrelated loaded MediaItem in response to saved-queue resume.
7. Repeat One: completed same item should seek/start locally without a new extractor network call. Simulate expired stream and verify no more than one automatic source refresh before explicit Retry. Confirm Shuffle, Repeat All, Previous >3s, queue skip and local file advance behave as before.
8. Force service destruction/background while resolution pending and confirm no stale callback installs media, leaves endless lookup indicator or accesses released player. Verify notification controls and phone power/audio focus events.
9. Inspect player, Home spotlight, mini-player and Retry UI at 320/360/412dp, tablet, large font, both themes, reduced motion and TalkBack. Verify a Retry message is clear and the content remains reachable while a stream lookup is delayed.
10. Regress private listening memory opt-in and erasure, local LRC lyrics, cloud snapshots/merge without destructive restores, saved playlists, local audio and artwork. Compare APK package/signing certificate with installed NightWave using `check-device-update.cmd` **before** an in-place installation; never uninstall or clear user data to bypass signer mismatch.

## Status
GitHub source, pure unit-test definitions, emulator-only tests and future manual CI gate committed. **No Android Gradle build, JUnit run, physical playback measurement, a11y screenshot review, signer validation or verified APK has been performed.** Keep as draft until actual on-device acceptance.
