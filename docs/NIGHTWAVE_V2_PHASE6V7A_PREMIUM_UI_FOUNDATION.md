# NightWave Phase 6V7A — Premium UI foundation

## Scope

A distinct visual-language iteration based on the full **6V6G** source,
without touching the music database, player service, queue ledger, Google
Drive Music Vault, private synced lyrics, backup payloads, or APK identity.

This is a **serverless client UI** improvement, not a replica of Spotify's
licensed catalog or music delivery infrastructure.

## Implemented

### Premium bottom navigation

- Replaces the previous selected-only expanding tab labels with a **balanced,
  equal-width four-destination navigation bar**, so Home, Search, Library,
  and You stay readable on compact phones.
- Introduces subtle, theme-aware selected pills, a restrained accent marker,
  fixed-height touch regions, and a softer floating surface.
- Preserves original UI test tags:
  `nightwave-tab-home`, `nightwave-tab-search`,
  `nightwave-tab-library`, and `nightwave-tab-profile`.
- Preserves the **existing HorizontalPager as the only navigation state**,
  swipe navigation, Back behavior, keyboard dismissal, and overlays. The new
  bar only dispatches the previous `navigateToTab` handler.
- Honors the user-selected **reduced-motion** preference by eliminating
  animated tab color changes in that mode. Selected semantics and a
  `Role.Tab` are retained for accessibility.

### Home refinement

- New **NightWaveHomeHero** replaces the loose greeting block with a more
  consistent 132dp card, soft gradient surface and static low-cost accent
  rings; no bitmap processing, network access, or artwork fabrication.
- Uses the same real local-hour greeting boundaries. Name presentation is
  trimmed and bounded, with a non-empty fallback on anonymous profiles.
- Keeps the existing Home shelves, recently played music, mixes, liked
  tracks and Spotify-style discovery shortcuts intact. These remain backed
  by the **same data and actions** as before.
- The hero has a stable `nightwave-home-hero` test tag.

### Mini-player finish

- Updates the existing mini-player shell's corner radii, gradient restraint
  and spacing while preserving the real artwork, progress, title and artist.
- The **Next** control is only shown if the queue has at least two songs and
  at least 365dp of internal width is available. At 320dp the player still
  exposes Play/Pause and Hide. Next remains reachable in full Now Playing.
- Retains vertical gestures, safe click targets, `Play/Pause`, `Next`,
  and the **Hide** action that does *not* pause or stop the service.
- Adds `nightwave-mini-player` for QA.

## New regressions

- `NightWaveChromePolicyTest`: 9 unit test declarations for hour
  boundaries, blank/long names, response at 320/365dp, NaN/infinite
  widths, selected-index bounds, and navigation minimum touch height.
- `NightWavePremiumChromeInstrumentedTest`: 3 isolated Compose UI test
  declarations covering all 4 clickable tabs on a 320dp layout, selection
  state and Home appearance with a blank or populated profile.
- Existing real Activity `NightWaveSwipeTabsInstrumentedTest` and
  `NightWaveMiniDismissInstrumentedTest` remain enabled and are required
  by the new source gate.
- `tools/verify_v2_phase6v7a.py` (CI; self-testing) and
  `tools/check-phase6v7a-integration.ps1` (Python-free Windows builder)
  validate source wiring, pager/overlay state and preserved playback
  actions before Android compilation.
- The actual **Kotlin CLI compiler** was used on an isolated copy of
  `NightWaveChromePolicy`; 17 layout/greeting assertions passed. This
  is **not an Android/Compose build**.

## Device QA when ready

1. Download the full, pinned source archive into a fresh directory.
2. Run `build-windows.cmd`; require **BUILD SUCCESSFUL**, all JVM tests,
   Android test-source compile, identity and signature verification.
3. Before overwriting an installed NightWave, use
   `check-device-update.cmd`. Do not uninstall/clear app data or replace
   audio files to resolve certificate mismatch.
4. Open Home on a 320–360dp screen; verify hero fits, bottom navigation
   labels remain legible, mini-player controls are not cut off.
5. Tap and swipe through Home, Search, Library and You; verify the same
   tab is selected, keyboard closes, and Back returns correctly.
6. Open full Now Playing and Queue, swipe horizontally and verify the
   background pager cannot unexpectedly change tabs under the overlays.
7. Play two songs: on 320dp the mini-player must retain Play/Pause and Hide
   without a cramped Next button; on sufficiently wide devices Next appears.
8. Hide the mini-player; verify playback **continues** and the compact
   restore affordance brings it back without clearing the queue.
9. Change dark/light themes and enable reduced motion. Check contrast,
   selection marker and instant tab color change in reduced-motion mode.
10. Regress offline music, queue restore, synced LRC, lyric export and
    Google Drive backup. None of those paths changed intentionally here.

## Current release state

The latest GitHub-hosted Actions workflows previously failed **before**
runner assignment, with no Gradle step executed. A new source gate is not
equivalent to an Android build. The 6V7A branch is intentionally a **draft
development branch**, not a certified APK or in-place installation.

## Next phase

**6V7B — Search, Library and artwork layout refinement**, followed by
**6V7C — Now Playing polish**. Keep these separate until a consolidated
verified Android build and device regression cycle is available.
