# NW80 — Playback Engine Perfection: targeted reliability fixes

**Roadmap:** [NightWave NW76–NW90](CURRENT_CUMULATIVE_ROADMAP.md)  
**Cumulative parent:** `feature/nightwave-nw79-download-offline-reliability`  
**Branch:** `feature/nightwave-nw80-playback-engine-perfection`  
**Status:** Source changes and regression tests committed. **Full Gradle, Media3 runtime, background/Bluetooth, and Samsung stress testing remain pending under the owner's NW90 consolidated test plan.**

## Verified source problems addressed

### 1. Play Next could erase Shuffle's real listening history

`PlaybackQueue.insertNext()` used removal-index remapping followed by insertion remapping when moving a **previously existing song**. Removal deliberately discarded every matching entry in the `visited` stack and `shuffledSeen` set, even though the moved song still existed in the queue. A listener could hear A → C, move A to Play Next, then fail to navigate back through A.

New `remapForMove(from, to)` calculates the old-to-new identity-preserving slot mapping and updates `visited` and `shuffledSeen` in one step. Genuine queue-item deletion still uses the existing removal policy. New Play Next insertions still shift indices and preserve user priority ordering.

**Regression tests:** move an already-heard track behind current, preserve current song and metadata, explicit Next priority, Previous history and forward redo; move upcoming songs; retain queue size without duplicates.

### 2. Rapid seek could act against a different loaded MediaItem

Previously `StudioPlaybackBridge.seekToForTrack()` validated a controller's media ID and then invoked a second generic `seekTo()`, which retrieved a controller again. Rapid Next, Jump, queue changes or delayed UI callbacks could invalidate the initial read.

`PlaybackSeekIdentityPolicy.target()` now returns a bounded target only for the exact expected, displayed **and currently loaded** media ID, with a positive duration and no player error. Both explicit gesture seeks and the generic legacy seek path capture **one controller** and validate immediately before invoking `player.seekTo(target)`. No unrelated track is seeked merely because a Media3 player is connected.

**Regression tests:** matching public/offline ID; old expected ID after new media selection; missing ID/controller identity; errored player; invalid duration; negative/oversized targets.

### 3. Previous on the first/only song unnecessarily re-resolved a cached stream

When `PlaybackQueue.previous()` correctly retained the current first song, `PlaybackService.playPrevious()` still called `resolveAndPlay()`. That could re-extract/stream the identical track, causing lag or failure. The service now uses `PlaybackPreviousPolicy.canRestartLoaded()` to rewind a matching cached item. If the player was at `STATE_ENDED`, it prepares/replays the cached media rather than leaving an explicit Previous request stuck. If the previous selection is **different**, loaded media is mismatched or errored, the original validated selection-resolution pipeline is preserved.

**Regression tests:** same-song rewind, actual Previous selection, mismatched loaded player and error.

### 4. Gapless callbacks could be adopted from invalid selection index

`OfflineGaplessPolicy.transitionIndex()` formerly accepted an automatic linear transition from `currentIndex=-1` to slot 0 because `-1+1=0`. A deferred Media3 callback must not invent a selected offline track.

Automatic transitions now require a valid current index and a different target. Normal one-step auto, shuffle, explicit user seeks, and Repeat All last→first wrap remain supported.

**Regression tests:** stale -1 or out-of-range queue position, same-slot automatic shuffle, regular auto, shuffle target, repeat-all wrap.

## Source-level safeguards retained

- Saved queue identity and public canonical URL policy.
- Single selected track search playback; user-controlled Play Next and Add to Queue remain distinct.
- Genuine Repeat One manual-next semantics, linear Previous, shuffle redo order and explicit queue restore.
- Media3 locally verified gapless timeline and bounded fallback for missing offline files.
- Audio focus, foreground service, lock-screen/notification controls, Bluetooth events and existing crossfade settings are **not rewritten** in this patch.
- Installed app identity, local audio, library preferences, Google Drive authorization, offline files and download WorkManager jobs are untouched.

## Checks and limits

- New/extended JUnit tests: `PlaybackQueuePlayNextTest`, `PlaybackSeekIdentityPolicyTest`, `PlaybackPreviousPolicyTest`, `OfflineGaplessPolicyTest`.
- New runnable pure Kotlin policy smoke: `tools/NW80PlaybackSmoke.kt`. Build this smoke against exact checkout sources with the `kotlinc` command in its header.
- Read back committed source and compare against these requirements before any future branch.
- **Do not label these tests passed until they actually execute.** Previous NW75 Windows compiler and JVM evidence is historical and not proof of a green NW80 Android build.
- Source fixes cannot prove crossfade sound, Bluetooth interruptions, duration after buffering, headset media button behavior, real offline decoder timing or full queue recovery; those remain explicit NW90 matrix checks.
- NW77's rights/source blocker is unchanged: **public release remains NO-GO**.

## NW90 acceptance scenarios

1. Rapid Next/Previous/Play Next and Shuffle transitions (including moved previously played songs) across 1, 2 and 100-song queues.
2. Repeat Off/All/One at end of queue, restart from first, queued Play Next after Repeat One.
3. Seek during late media resolution, after Jump, after selecting offline/public, while errored or stalled, at duration bounds.
4. Offline gapless/skip and Repeat All wrap; shuffle with missing/deleted/private files; interrupted audio session and decode recovery.
5. Bluetooth headset/car/notification/lock-screen controls, simultaneous interruption/ducking, app process death and foreground service restart.
6. Real long-session playback, queue persistence, offline airplane mode, battery/stability and no altered Google/offline library data.

NW80 source work is a **targeted reliability milestone**; final end-to-end perfection and the 9.5+/10 target require the complete NW90 evidence.
