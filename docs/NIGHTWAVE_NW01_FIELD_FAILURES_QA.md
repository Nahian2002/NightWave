# NightWave NW-01 — Phone-Reported Failures and Bounded Recovery

**Date:** 19 October 2026  
**Base commit:** `6960dc5d5b46581947589b7ab75e2ae0ae9397e1`  
**Repair branch:** `fix/nightwave-v2-nw01-shuffle-eq-google-recovery`  
**Status:** Source changes committed. **No fresh passing Android Gradle build or device acceptance evidence.**

## Reports from installed build 35

1. Public-video Shuffle (crossed arrows) responds slowly or settles on the wrong state.
2. Audio Equalizer has no audible effect or reports an unavailable/error state.
3. No Google Drive authorization prompt or actionable error appears.
4. User screenshots additionally show a cluttered Now Playing screen, questionable seek-bar rendering, oversized You-card sections and blurred public-video art. Those are **deferred UI QA items**, not addressed by this bounded logic patch.

## Changes in NW-01

### Shuffle — P0

Previously `StudioPlaybackBridge.toggleShuffle()` sent `ACTION_SHUFFLE` for public videos with no immediate state update, while `PlaybackService` toggled its current persisted boolean. Rapid taps could race against the bridge's stale queue snapshot.

- Synchronously reflect the requested shuffle boolean in the Compose state.
- Send explicit `EXTRA_SHUFFLE_ENABLED` and a unique bounded request ID on each public-video shuffle tap.
- Resolve service requests as idempotent explicit values; legacy clients with no extra keep toggling.
- Publish a minimal service acknowledgement after the requested queue state persists. Reconcile the UI only after the latest acknowledgement matches, or after bounded timeout.
- Added four pure JVM tests for explicit On/Off, idempotence, rapid On-Off-On and legacy toggling. These were **committed but not yet executed by a passing Gradle run**.

**Acceptance:** Play a public-video reference, tap Shuffle once, tap twice rapidly, and tap three times rapidly. The visual response must be immediate and the final saved mode must match the last requested state. Confirm Next uses the final queue mode.

### Equalizer — P0 diagnostic and route validation

Existing Android audio-effect code is attached to the Media3 ExoPlayer audio session. Android's equalizer API cannot guarantee that every output route applies an effect, even when initialization succeeds.

- Report when no active audio session exists instead of silently displaying a previous status.
- Check `Equalizer.hasControl()` before claiming that the player controls the engine; report a competing audio effect.
- Explain unsupported device presets, manual band levels and effect-enable failures.
- Successful status says **enabled on playback session** but does not claim a measured audible effect.

**Not yet solved:** Audible success on the reported Samsung device and replacement of the basic Android preset dialog with a dedicated premium Compose equalizer. These require a playback device test before any success claim.

**Acceptance:** Play a personal offline file before entering Audio equalizer; choose supported preset and compare Off/On at a fixed volume; try manual bands including a nonzero level. Observe the toast/status message. Repeat once with phone speaker and once with compatible wired/Bluetooth output to separate route limitations. Record status text and whether the sound changes. Avoid changing app/private music files.

### Google Drive — P1 onboarding/diagnostics

The existing action authorizes the narrow `drive.appdata` OAuth scope for backup. It is **not** full Spotify-like Google login and does not automatically synchronize a guest profile.

- Exposed a top-level **Google Drive · optional** connect card in You, rather than requiring a user to open the collapsed Manage backups area.
- Recover from synchronous Play-services authorization failure.
- Show actionable messages for failed/cancelled PendingIntent/result paths.
- Display Google Play-services numeric status code when provided by `ApiException`, without logging tokens or account IDs.
- Retain existing optional backup and guest-local storage semantics; no new OAuth scope and no backend.

**Acceptance:** Tap You → Connect Google Drive. A permission/account experience should appear when required; a previously granted scope may complete without another prompt. If unavailable, the app should show a visible error and diagnostic status rather than silently doing nothing. Record any numeric Google status code. A successful Drive connection does **not** convert the guest profile into a cloud-synced identity.

## What was validated, and what remains

| Gate | Result |
| --- | --- |
| GitHub branch and source commits | Committed |
| Source contract checks against the actual connected files | Checked; required markers present and no duplicate imports in edited Kotlin source |
| Four additional JVM tests | Authored; execution not confirmed |
| GitHub Actions | Two NW-01 push attempts failed almost immediately, with **zero job steps recorded**; this is not Kotlin/test failure evidence |
| Full JVM/Android test compilation | **Not verified** |
| Audible equalizer behavior, Google consent and exact shuffle responsiveness | **Awaiting device verification** |
| Existing APK signing compatibility | **Not checked** |

To validate, build the entire NW-01 branch from a clean extraction with `build-windows.cmd` and require a fresh `BUILD SUCCESSFUL` for `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`. Inspect `dist/NightWave-build-status.txt` before considering installation. Check signer compatibility with existing NightWave; **never uninstall or clear Android app data** to bypass a signer mismatch.

## Subsequent work order after these P0/P1 retests

1. **NW-02 Equalizer studio:** premium Compose bands/presets and reliable per-session, route-aware effect activation.
2. **NW-03 Google identity:** separate intentional Google sign-in / guest upgrade and explicit account-to-backup relationship; avoid implying that Drive appData consent is full account login.
3. **NW-04 Now Playing polish:** correct seeking visuals, public artwork quality, button hierarchy, compact settings/actions, and mini-player/content insets.
4. **NW-05 Feature validation:** playlist, LRC, on-device mixes, background audio and restore/recovery regression gates.

The test reports are a reason to prioritize these changes, **not proof that all three user-facing defects have been completely resolved**.
