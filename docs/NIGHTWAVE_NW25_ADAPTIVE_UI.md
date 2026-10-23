# NightWave NW25 — Adaptive premium UI (development branch)

**Date:** 24 October 2026. **Status:** DRAFT / full Android and device QA still pending.

Base: `feature/nightwave-v2-nw24-scalable-offline-library`. Branch: `feature/nightwave-v2-nw25-adaptive-premium-ui`.

## Delivered in the repository

- **New pure adaptive size policy:** `NightWaveAdaptiveChromePolicy.kt` bounds artwork dimensions safely when a split-screen/window reports a tiny, zero, infinite or invalid viewport; computes responsive navigation, mini-player, Home hero and native library row sizes for 100%, 130% and 160%+ font scaling. Includes clamped reduced-motion animation duration and horizontal padding.
- **Bottom navigation:** Four destinations and existing semantic test tags remain; the shell allocates taller navigation at accessibility text sizes and respects reduced-motion state.
- **Home:** Greeting card grows with large text, reserves more content width at 160%+ scaling and allows more lines instead of clipping the user's greeting.
- **Now Playing:** Album artwork uses nonnegative, viewport-bounded dimensions; content margins shrink on narrow windows and expand on tablets. Existing playback actions, artwork swipe gestures, queue controls and Media3 service are preserved.
- **Mini-player:** Track/action bar expands from 52dp to 64/78dp on large fonts, with slightly larger thumbnails; never hides or stops playback due to a layout change.
- **Search:** The independent licensed-offline-audio shortcut is now a full-width 52dp minimum button beneath the discovery header instead of competing for space with the heading on narrow screens. The search result source and rights remain unchanged; this is **not** YouTube-to-MP3.
- **Track and collection rows:** `heightIn(min = ...)` replaces fixed collection-row heights, letting taller accessibility text expand naturally. Track titles may show a second line when fonts are enlarged. Option buttons remain 48dp.
- **You/Profile:** Profile setting rows have a 60dp minimum touch height while retaining all existing toggles, account actions and cloud backup UI.
- **Native Offline Library, persistent Downloads and direct HTTPS download screens:** On tablets, content is centered and capped at 760dp instead of expanding into unwieldy full-width lines. Safe system-bar / display-cutout / keyboard insets remain applied. Library rows grow for large text.
- **Confirmed defensive fix:** Direct-link download progress now guards `total > 0` before division; a zero-length Content-Length can no longer cause an integer division by zero.
- **No theme assets, app name, Android package, source catalog, Google account state, playlists, private track IDs, offline index schema or APK signing were changed.**

## Executed evidence

The three **exact** Kotlin source files fetched from GitHub were independently reproduced locally, and each Git-blob SHA-1 matched:
- `NightWaveChromePolicy.kt`: `b09bcaadbeaf88fe627a820f6449b4cf6a2fafd2`
- `NightWaveAdaptiveChromePolicy.kt`: `60ce36c0b349226bc846527ce727f983aa64e743`
- `tools/kotlin/NW25AdaptiveChromeSmoke.kt`: `e94e80dbfb971b4ae55329e878d9b26cb92068d1`

`kotlinc` compiled all three into a standalone JVM jar, and `java -jar` returned:

```text
PASS: NW25 28 exact-production Kotlin adaptive chrome assertions
LIMIT: Android Compose, native layout and device screenshots not compiled
```

These tests check the pure Kotlin policy, NOT full Android UI compilation. Source integration checks and two Android instrumentation test definitions have also been authored; successful execution still depends on a working CI runner or full Windows Gradle build.

## New regression files

- `app/src/test/java/com/nightwave/player/v2/ui/NightWaveAdaptiveChromePolicyTest.kt`: JUnit viewport/font scale/reduced-motion boundaries
- `app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveAdaptiveChromeInstrumentedTest.kt`: two Compose 320dp / 200% font tests for four-tab selection and the Home greeting (not executed)
- `tools/kotlin/NW25AdaptiveChromeSmoke.kt` and `tools/nw25_adaptive_chrome_smoke.py`: exact pure-Kotlin JVM policy checks
- `tools/verify_nw25_ui.py`: read-only, fail-closed integration/source regression checks; wired to `.github/workflows/android.yml`

## Limitations and final device acceptance

- No claim of pixel-perfect display, visual comparison, no clipping on all devices or a Spotify-identical design: **UI screenshots and interaction tests are still required** on at least compact phones, S24 Ultra and tablet/split-screen sizes.
- Do not claim GitHub Actions failing with zero job steps is a Kotlin/Gradle compilation error; inspect run job steps before making that conclusion.
- Validate Android text scales 100%, 130%, 160% and 200%, display zoom, landscape, keyboard, safe insets, accessibility contrast, TalkBack and reduced motion.
- Verify the mini-player still appears after a background session, and that buttons/gestures retain the same Media3 behavior (including swipe-to-dismiss that does not stop audio).
- Real Google authorization, audio downloads, background work, Android notification permission and source-to-APK release certification remain pending from earlier phases.
- Preserve installed NightWave and all app-private files. Verify APK signing certificate continuity before any installation; do not uninstall/clear data to bypass a signature mismatch.

**Release gate: draft source only.**
