# NW-04 — NightWave Premium Player Overhaul

**Branch:** `feature/nightwave-v2-nw04-premium-player`  
**Starting point:** NW-03 `f0b4667aa1adff29c7fb9b3a62383c1eef760f11`  
**Scope:** Focused Now Playing, artwork, seeking, mini-player and navigation refinement.  
**Evidence:** Source committed, regression tests added; **no verified full Android build, UI screenshot baseline or on-device acceptance yet**.

## Changes made

### Listening screen and seek control

- Replaced the Material 3 default tall slider thumb/track appearance with a slim, full-width, canvas-drawn seek timeline with a circular thumb and consistent primary/inactive track colors.
- Touch **tap**, touch **drag**, cancelled drag and accessibility `setProgress` actions are explicitly handled. Scrubbing previews the position locally and submits a bounded Media3 seek on release, not on every drag frame.
- Seek fractions are passed through the existing `PlayerProgressRules` duration checks, with a pure `NightWaveSeekGesturePolicy` for bounded pixel-to-fraction mapping.
- Playlist changes invalidate track-keyed drag previews; a cancelled gesture returns to the actual playback progress.
- Preserved Play/Pause, Next/Previous, repeat, shuffle, swipe-to-skip art, lyric positioning, sleep timer and queue controls.
- Reduced oversized artwork and transport spacing while retaining meaningful hit sizes and readable song details.

### Media artwork

- For **recognized low-resolution YouTube thumbnail URLs only** (e.g. `default.jpg`, `mqdefault.jpg`), requests a better `hqdefault.jpg` still.
- If an upgraded thumbnail URL fails, falls back to the original image. User-owned local art, other providers, already better-quality YouTube sizes and unrecognized URLs are never rewritten.
- No fabricated album covers, source substitutions, external image databases or private-content uploads.
- YouTube itself may offer only low-quality imagery for some videos; pixels absent from the source cannot be recovered automatically.

### Compact player and navigation

- Mini-player now has a shallower handle, 52dp content row, 38dp artwork, tighter typography and a slim progress strip. **Play/Pause and Hide maintain 48dp tap targets**, and hiding still never pauses playback or deletes the queue.
- Bottom navigation reduced from 66dp to 60dp, with **56dp destination touch areas**; button labels, selected state, tab gestures and page keys are preserved.
- Advanced audio controls now sit behind an optional **Sound & playback** disclosure. Queue and Lyrics remain visible in the main flow.
- Reworded queue label to **Queue · N tracks**, including correct singular usage, rather than inaccurately promising that the current song was upcoming.
- Updated legacy source validators to accept the intentional compact dimensions, instead of creating false failures.

## Automated regression coverage added

- Four JVM tests for safe YouTube artwork quality upgrades and preservation of local/provider artwork.
- Four JVM tests for seek-touch position mapping at endpoints, middle, invalid coordinates and narrow layouts.
- A new NW-04 source gate checks the seek timeline, accessibility semantics, control retention, compact sizes, thumbnail fallback and policy tests.
- The source-build workflow includes this branch and still requires `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.

**These tests have been written and committed. They have not been certified passing.** Static source checks, if they pass, do not establish that Compose works correctly on a phone.

## Deferred Android acceptance

After the user chooses to test the combined NW-01–NW-04 build:

1. Download a fresh ZIP for this branch; run `build-windows.cmd` and require a complete **BUILD SUCCESSFUL** including JVM tests, APK assembly and Android instrumentation-source compilation.
2. Confirm signing certificate compatibility with the existing installed NightWave; do **not** uninstall or clear app data to force an update.
3. Start a public-video and a locally imported offline song. The seek bar should be a single thin line with a round thumb; no separate vertical indicator, purple tail or clipped track.
4. Tap 25%, drag to 75%, release and cancel a drag. Seek should occur once on commit, display the correct duration, and restore actual progress after cancellation.
5. Change tracks during or immediately after a seek gesture. The old song's pending seek must not move the new song.
6. Test accessibility with TalkBack and touch exploration, plus narrow (320dp) and large screen widths. Ensure vertical scrolling the player does not accidentally seek, and seek gestures remain responsive.
7. View known low-resolution public-video artwork and confirm improved image quality where available, with safe fallback if the higher-quality still does not exist. Local art must remain unchanged.
8. Inspect Now Playing on the Samsung phone: artwork, typography, bottom controls, Queue/Lyrics, optional Sound & playback, mini-player and navigation must not overlap.
9. Hide and restore the mini-player, swipe up/down, switch tabs rapidly and verify that playback never stops unless Pause is pressed.
10. Retest NW-01 rapid public Shuffle, NW-02 Sound Studio and NW-03 optional Google Drive authorization for regressions.

## Still not implemented or certified

- Full Google identity sign-in and live multi-device account sync are outside NW-04.
- Spotify's licensed catalog, real-time lossless streaming, Smart Shuffle, AI DJ and Connect device handoff are not replicated.
- An end-to-end Android UI render/screenshot approval, play-service tests and a release-signed APK remain outstanding.

**Recommended next phase: NW-05 playback and UI integration stabilization** before adding more broad feature surfaces. The user prefers combined testing later; do not report a green release until real build and device evidence exists.
