# NW67 — Shuffle forward history and pause-safe offline decoder recovery

**Parent:** `feature/nightwave-nw66-offline-integrity-recovery`  
**Branch:** `feature/nightwave-nw67-playback-transition-reliability`  
**Status:** Source changes and regression tests committed. **Full Android Gradle/JUnit compilation, Media3 playback and physical-device QA NOT YET VERIFIED. Public release: NO-GO.**

## Source-level defects found

1. The **public PlaybackQueue shuffle path** remembered past tracks for Previous, but lacked a matching forward history. Pressing Previous after several shuffled tracks and then Next picked a random new candidate rather than the track just left. This could surprise listeners, particularly when a queue still had unseen songs.
2. **Offline Media3 decoder recovery** posted a deferred action with generation and media-ID checks, but did not recheck that the user still wanted playback or that the sleep timer had not paused it. Since Pause need not increment the selection generation, a delayed error handler could call `play()` after the user paused.

These are defects identified by reviewing source paths, **not a claim of a witnessed NW66 Samsung crash**.

## Source changes

- Added a bounded, session-only `forwardVisits` stack to public `PlaybackQueue`, mirroring its existing bounded `visited` stack. After Shuffle Previous, Next now retraces the actual departed song(s), rather than randomly selecting a new unseen entry. No queue or URL persistence changes.
- Manual **Play Next** still takes priority over forward history and invalidates that old forward trail. Reordering, removing, restoring, replacing, clearing, explicit display-order selection, shuffle toggle and a new Jump invalidate the trail as applicable. A same-item jump does not erase it. A queued song is never duplicated by the navigation implementation.
- Existing Shuffle cycle, Repeat Off/All/One, Play Next queue priorities, offline queue, bookmarks and public source resolution remain unchanged. Repeat One's automatic replay returns the current song without consuming forward history.
- Added `OfflineDecodeRecoveryPolicy.acceptsPostedRecovery` to require an unchanged playback generation, identical loaded and selected private media ID, bounded valid queue ownership, the originally failed ID, **current play intent**, and a non-paused sleep timer. `PlaybackService` now checks that policy immediately before advancing after a posted offline decoder failure.
- Media3 gapless timeline ownership, playback files, offline library, existing public stream retry policy, equalizer and player settings were **not changed**. This patch does not claim perfect gapless transitions, real-device shuffle fidelity or codec recovery without device tests.

## Regression coverage (committed; execution still pending)

- `ShuffleForwardNavigationTest.kt`: exact forward retrace, three-level back/forward, invalid versus valid Jump, Play Next priority, replacement/reset/shuffle toggle, and Repeat One with an unconsumed forward history.
- `OfflineDecodeRecoveryPolicyTest.kt`: deferred offline decoder recovery can only proceed with ongoing playback intent; pause, sleep timer, new generation, unexpected MediaItem, changed selection, absent failed ID and oversized queue all reject.
- Windows `tools/check-nw59-integration.ps1` requires the NW67 queue/recovery policy/test wiring, while retaining cumulative NW60–NW66 safety gates. This source checker is **not** an Android compilation or runtime test.
- Full Android/Kotlin Gradle and on-device playback tests were **not executed**, so no compile/build success, end-to-end playback PASS or precise percentage of musical perfection is asserted.

## Later Samsung acceptance (after a verified, signer-compatible Windows build)

1. Shuffle a 5+ song **public** queue, press Next three times, then Previous three times and Next three times; confirm the forward tracks match the actual listening order.
2. Repeat with user-enqueued **Play Next**, Replace Queue, Jump, Shuffle On/Off, Repeat One/All and app reopening. Confirm Play Next retains explicit priority and no phantom titles appear.
3. On a disposable local playlist containing an intentionally unplayable audio fixture, trigger the decoder failure, then quickly press Pause **before the queued recovery can run**. Confirm no automatic unpause. Test sleep-timer pause similarly.
4. While an offline decoder recovery is scheduled, change to a different song/queue or switch online; the old callback must not seek or restart outdated media.
5. Check Media3 offline gapless shuffle/Repeat All and manual Previous/Next independently. NW67 intentionally avoids modifying Media3's own prepared timeline.
6. Verify the existing NW60 bounded decode skip, NW43 queue priorities, NW62–NW66 downloads, lock-screen transport and previously saved offline tracks.
7. Before installing, require `build-windows.cmd` SUCCESS and signing verification with `check-device-update.cmd`. Do not uninstall or clear saved audio.

**Outstanding:** complete Android/JUnit build, physical-device audio/codec/notification tests, Google OAuth error 8, rights/Play release readiness, downloaded source eligibility. Public APK/Play: **NO-GO**.
