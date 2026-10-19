# NightWave NW-05 — Playback & UI Integration Stabilization

**Date:** 19 October 2026  
**Base:** NW-04 `43962109d9a1e5da160d0345ce26d247b2762894`  
**Branch:** `fix/nightwave-v2-nw05-integration-stability`  
**Status:** Development source committed. **Not a verified build or phone-tested release.**

## Why this phase exists

NW-01 to NW-04 changed public queue controls, Android equalizer interaction,
Google Drive account presentation and Now Playing gestures. This pass focuses on
cross-feature correctness rather than another purely visual redesign.

## Implemented corrections

### 1. Public-video queue identity

Previously `PlaybackQueue.replace()` preserved the requested song URL before
removing duplicate queue entries, but `PlaybackService` still chose the
same-queue fast-path item by its **post-deduplication numeric position**.
If a duplicate occurred earlier in the list, the service could misidentify the
current item, skip its reuse optimization or resolve the wrong track.

- New pure `PublicQueueSelectionPolicy.selectedIndex()` resolves the requested
  original URL against the deduplicated list, with bounded index fallback.
- Both selected-entry lookup and already-loaded fast-path checks now use
  the resolved index.
- Four JVM tests cover duplicates ahead of selection, tapping a duplicate,
  out-of-range input and lists without duplicates.

### 2. Seek gestures cannot jump the next song

Previously the NW-04 timeline scoped a visual preview to the current song, but
the final touch callback sent a generic `seekTo()` against whichever song the
Media3 controller owned when the gesture finished.

- Tap, drag and accessibility seek callbacks carry the original track ID.
- Pointer input restarts when track identity changes.
- `StudioPlaybackBridge.seekToForTrack()` verifies the originating ID equals
  both the displayed selection and the actual loaded Media3 `mediaId` before
  delegating to the existing clamped seek.
- Four JVM tests cover public/local identifiers, old-song gestures and absent
  player metadata.
- Existing lyrics/media transport seeks are not modified.

### 3. Device manual EQ bounds agree with service validation

NW-02 exposed device-reported gain ranges, but the playback service enforces
manual gains in **-1500 to +1500 millibels** (±15 dB). On devices advertising
wider ranges, Sound Studio could display slider values it would always reject.

- Added `SoundStudioPolicy.safeManualRange()` to intersect device bounds
  with the service input limit.
- Slider, manual draft, reset action and apply eligibility use that intersection.
- Empty/unsupported ranges stay unavailable without crashing the UI.
- A regression test verifies the intersection and invalid gaps.

This phase **does not** claim EQ is audible through every output route,
nor does it replace Android's platform audio-effect engine.

### 4. CI and source-contract alignment

- Added NW-05 branch and `tools/verify_nw05_integrated_stability.py` to the
  Android workflow.
- Removed an accidental duplicate NW-04 verifier invocation.
- Preserved earlier NW-01 to NW-04 source gates and the complete
  `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`
  Gradle build gates.

**Evidence boundary:** The new tests and source contracts were added to the
repository; they do not count as passing JVM or instrumentation tests until
a fresh Gradle run completes. CI previously queued for extended periods.

## Deferred integrated acceptance — user will test later

1. Download the **NW-05** source from its exact audited commit, extract to a
   fresh Windows directory and run `build-windows.cmd`. All required Gradle
   tasks and `dist/NightWave-build-status.txt` must report success.
2. Verify the APK application ID, SHA-256 and signing certificate against
   the installed build. **Do not uninstall or clear app data to bypass signing.**
3. Play four public saved tracks, including repeated URLs ahead of the tapped
   track. Verify tapping song C actually selects C without resetting an
   unrelated buffered stream.
4. Start scrubbing Song A and switch to Song B, then release the gesture:
   B must **not** jump to A's seek position. Test tap, drag, cancel and
   TalkBack progress adjustments.
5. Play imported local audio and test Sound Studio manual bands, preset changes,
   unavailable routes and a device with ranges narrower or wider than ±15 dB.
6. Retest public Shuffle rapid-tap state, offline Repeat/Previous, background
   playback, Sleep Timer, artwork and mini-player dismissal.
7. Test You → Account & backup Drive authorization, backup verification,
   cancelled restoration, disconnected state and guest-library retention.
8. Record any crash/ANR and screenshots of narrow UI overlaps. Privacy-filter
   logs before sharing.

## Remaining blockers

- A full passing Android build and executed JVM tests.
- Physical Samsung audio route, Google Drive authorization and UI gesture QA.
- Real-world artwork resolution and layout testing at multiple screen widths.
- APK signer compatibility and protected data migration acceptance.
- Dedicated Google identity login and Spotify-scale catalog/discovery remain
  out of scope; Drive `appdata` authorization is not a NightWave cloud account.

**Disposition:** Engineering stabilization source is committed. This is a
development checkpoint, not a production-readiness certificate.
