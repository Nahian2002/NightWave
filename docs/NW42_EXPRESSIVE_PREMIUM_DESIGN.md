# NW42 — Expressive Motion and Artwork-Led NightWave

**Date:** 25 October 2026  
**Branch:** `feature/nightwave-nw42-expressive-motion-system` based on NW41  
**State:** SOURCE UPDATE ONLY. Full Android build, installed-device UI testing and public-release approval **NOT VERIFIED**.

## Input evidence

Reviewed the uploaded `Screen_Recording_20261025_155708.mp4` (~338 seconds, 1080×2340). It shows Home, Search, song results, Library/collections, the full player, Downloads, Offline Library and You. Prior NW41 recordings also show Google Drive account selection returning internal error 8. The screenshots/recordings support UI observations but **cannot establish** an OAuth client registration issue or a network-throughput root cause.

The issue is not that NightWave needs more cards. Too many equal-weight rounded panels, repeat-style gradient artwork, duplicate Search actions and tiny secondary text currently compete with music itself. The intended design is **editorial hierarchy, actual user music art, quiet supporting chrome and meaningful state transitions**.

## What changed

- **App-wide reduced-motion preference:** `NightWaveMaterialTheme` now provides the existing `MotionPreference.REDUCED` through `LocalNightWaveReduceMotion`. Existing navigation/Now Playing reduced-motion behavior remains.
- **One motion contract:** `NightWaveMotionPolicy` bounds visual motion to 0–450ms, has named duration tokens, and defines small tab/artwork state shifts. Animations run on state changes, not indefinitely or while idle.
- **Bottom navigation:** selected icons use interruptible spring motion when enabled, and no transform under Reduced Motion. Tab destinations, names, tags and pager navigation remain unchanged.
- **Song rows:** a currently playing song smoothly changes surface tint/outline and subtly emphasizes its artwork. This does not play/stop/seek/delete music or alter queue state.
- **Mini and full player:** a floating/elevated mini-player and artwork-state spring clarify current playback. Album art remains tied to the real thumbnail; the existing full-screen crossfade and gesture handlers remain.
- **Home:** the hero optionally displays genuine recent-track thumbnail art with an explicit scrim for foreground legibility. If no thumbnail is available, the previous privacy-preserving abstract fallback remains. The former two statistics cards become one concise Library overview.
- **Made-for-you mixes:** use the actual first saved song's artwork under a strong legibility scrim, not repeated rings. The abstract fallback remains for missing thumbnails; no recommendation order or track identity is invented or changed.
- **Search:** replaces the oversized discovery card with an editorial heading and compact, real offline-source shortcut; places more results above the fold. Consolidates duplicate long Search explanations while preserving the primary song search and the alternative YouTube action.
- **You / Settings:** shifts repeated category cards to slimmer titled, separated list groups with consistent tonal icon containers. All existing profile/account/storage options retain their callbacks.

### Design basis

- [Material 3 Expressive](https://developer.android.com/develop/ui/compose/designsystems/material3): emphasis via typography, tone, shape and hierarchy rather than identical blocks for every element
- [Android Compose animation guidance](https://developer.android.com/develop/ui/compose/animation/quick-guide): springs for interruptible motion, animate state only, avoid high-cost repeated recomposition/layout, respect motion choices

This NW42 pass reuses the already included Compose/Coil libraries; **no large paid font, background video, new server or new device permission** is needed to achieve premium UI. Increasing APK size by itself cannot improve design quality.

## Google Drive issue (separate from appearance)

Google authorization still **fails** in the provided user report (Google ApiException internal error 8). NW41 already exposes `You > Account & backup > Copy Google setup report`, reading the **actual installed APK SHA-1**, package and Play Services status, without tokens or email. NW42 intentionally does not replace Google's official AuthorizationClient flow or bypass OAuth.

Per [Google Android authorization guidance](https://developer.android.com/identity/authorization) and [Google APK certificate client-auth guidance](https://developers.google.com/android/guides/client-auth), verify in the relevant Google Cloud project:

1. The Android OAuth client package exactly matches `com.nightwave.player`.
2. Its signing SHA-1 exactly matches the APK currently installed (including the local debug signer or Play App Signing if applicable).
3. Google Drive API is enabled, OAuth consent/audience is appropriately configured, and the testing account is included as a test user where applicable.
4. Google Play services status in the copied report is healthy.

These are potential causes to check, **not verified root cause**. Registering/changing the Google Cloud project's OAuth client requires control of that project and cannot be accomplished by changing Kotlin UI. Do not publish client secrets or access tokens. A browser account chooser appearing does not establish that Drive permission was granted.

## Scope and safety

Preserved: existing Android package name and signing, library storage/Room transaction path, local music/audio, download jobs/WorkManager policies, Google token handling, search provider selection, playback service, sleep timer, queue, app theme options and previous NW41 redacted OAuth diagnostics.

The source changes touch only Compose presentation, related layout wiring, policy tests, README and CI branch eligibility. They do not claim real-time lyrics or universal offline download rights.

## Checks

- **12/12 targeted source-wiring invariants** passed (using exact NW42 GitHub file contents): theme/motion propagation, tab semantics, song/current playback state, real local artwork, Search/YouTube route, Offline catalog route, settings action groups and tests.
- **13/13 standalone Kotlin policy assertions PASSED.** Source `NightWaveMotionPolicy.kt` from NW42 was hashed locally to Git blob SHA `2a40cb5e18f4b3f208dfc240c7bfee8831af2ae6`, matching GitHub exactly, compiled with `kotlinc`, and executed. These checks cover durations, bounds, selected/idle transforms and reduced motion.
- `NightWaveMotionPolicyTest.kt` added for JUnit. **Android Gradle/Kotlin/Compose compilation is not validated** by the smoke.
- The existing GitHub Actions Android build is enabled for NW42, but runner availability and actual Kotlin/Compose results must be checked separately. Do not publish this as a certified APK until a real build passes.

## Device acceptance plan

After **LOCAL BUILD SUCCESS** on `build-windows.cmd` and successful `check-device-update.cmd` signing continuity:

1. Home top-to-bottom: artwork appears only for genuine recently played songs; text remains legible even for bright covers or failed images.
2. Search on 360dp phone + 1.5× / 2× font: primary search, alternative YouTube and offline shortcut work; history and results scroll without clipping or keyboard jumps.
3. Library: the saved mixes show real track artwork and maintain original track order; pinned lists, recent removal, drag reordering and Undo retain state.
4. Switch tabs repeatedly and lock/unlock screen; selected icon animation is smooth with no off-screen overflow.
5. Pause/play tracks: current-row/mini/full-player effects should transition without flickering album art, audio interruption or high CPU usage.
6. You > Appearance > Reduced motion ON: tab/artwork springs become instant; reduced mode persists on reopen.
7. Try the full player gestures, queue, offline airplane-mode playback, Download pause/retry and old song options, confirming no behavior regressions.
8. Use the installed-APK Google setup report to validate Google Cloud client registration **before** diagnosing any remaining status 8.

## Remaining work

This is one substantial **design-system integration**, not an impossible claim that the entire app is now 10/10. A subsequent pass still needs visual evidence on a real 360dp–430dp device for expanded audio/lyrics/queue screens, Search source errors, offline file/history UI, advanced Settings, motion at high font scale and OLED/pale themes. NW41's Google OAuth blocker and real resumable downloader support are separately tracked.
