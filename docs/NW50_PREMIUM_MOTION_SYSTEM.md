# NW50 — Premium Motion System

**Parent:** `feature/nightwave-nw49-audio-lyrics-studio`  
**Branch:** `feature/nightwave-nw50-premium-motion`  
**Status:** SOURCE IMPLEMENTED. Exact motion-policy file compiled in isolated Kotlin smoke. Full Android build, JVM test execution, Compose instrumentation and on-phone visual QA **NOT YET VERIFIED**.

## Design outcomes

### Navigation
- Four fixed Home / Search / Library / You destinations retain readable labels, 48dp+ interactions and accessibility selected-state semantics.
- Selected indicator now scales and fades in/out, instead of abruptly appearing; background tint and icon spring use restrained durations.
- Android system haptics are requested only on intentional unselected-tab taps, using platform `KEYBOARD_TAP` feedback **without** bypassing device haptic preferences. Reduced Motion and reselecting the active tab suppress feedback.
- A new motion policy permits a 220ms controlled page transition only for immediate neighbors. Far-apart tabs **jump directly** rather than animating through expensive intermediate pages. A newer navigation cancels any unfinished previous navigation, avoiding stacked animation jobs under rapid tapping.
- Back navigation, swipe pager state, Search redirection and existing library navigation are preserved.

### Player
- Full-screen Now Playing opens with a bounded short translation (at most 240px) and fade over 260ms, closing within at most 200px and 180ms. Previous full-height slide replaced.
- Mini-player cover changes use a 190ms crossfade; artwork playing scale remains small (0.985–1.0), not a perpetual animation.
- Mini-player Expand, Play/Pause, Next and Hide perform optional user-action haptic feedback only when Reduced Motion is off. The click actions still call the original playback/hide callbacks. Feedback is never triggered by automatic transitions, buffering or timers.
- Full-player artwork and queue-swipe feedback now consume shared durations, with **zero-duration/no-transition** for Reduced Motion. Swipe thresholds and queue/player state ownership remain unchanged.

### Home listening shelf
- Recently-played tiles smoothly animate selected surface tint and subtle artwork emphasis when the actually playing track changes.
- Tap-to-play gets optional OS-respecting haptic feedback. Options and Remove stay independent controls; no additional feedback or destructive operation is invoked.
- Album art stays on the existing Coil URL/fallback path. No new continuous effect, frame loop, blur pass, internet request or background service was introduced.

## Accessibility and performance

- `NightWaveMotionPolicy` remains Compose-independent and contains central capped durations, haptic eligibility, page-distance navigation, and bounded player-entry offsets.
- Reduced-motion mode retains instant pager transitions, snaps indicator/cover/selection animations to zero and disables added haptics.
- Existing screen-reader labels, selected semantics, real button callbacks and 48dp controls remain. Android vibration settings are not bypassed.
- Large text, tablets and split screen still rely on the existing adaptive layout policies rather than pixel-specific positions.

## Test evidence

- **PASS — exact-file Kotlin smoke**: `NightWaveMotionPolicy.kt` compiled locally with `kotlinc`, Git blob hash `03c295aee1470f7ae256d2a5554f804070185ea0` verified against GitHub; smoke covered nearby/distant pages, invalid indices, reduced motion, haptic eligibility and min/max player offsets.
- Five new pure-policy test methods added to `NightWaveMotionPolicyTest` for navigation distance, invalid indices, reduced-motion/haptics, player transition bounds, and all capped durations.
- One new Android Compose navigation test in `NightWavePremiumChromeInstrumentedTest`: reselecting the current tab must not dispatch navigation twice.
- `tools/verify_nw50_motion.py --self-test` includes negative fixtures for active-tab tap suppression, motion bounds, stale pager job cancellation, reduced-motion entrance, and recent artwork transitions.
- GitHub Actions workflow invokes the new NW50 static gate before Gradle compilation/tests. Prior runs had zero executed steps on GitHub; no success is inferred until a runner actually compiles the Android project.

## Deferred combined-device acceptance after NW52

1. Compile the **stacked branch** with the established Windows build environment. Require green Kotlin compilation, unit tests, Compose androidTest-source compilation, and final APK creation. Do not uninstall the existing app or delete user data just to upgrade.
2. On the Galaxy S24 Ultra, navigate Home→Search→Library→You, then Home→You and rapidly alternate tabs; check destinations and Back remain correct with no flying through Search/Library when jumping far.
3. Test regular and Reduced Motion, including recent tile selection, artwork changes, mini-player expansion and full player entry/exit; no transition should block taps or alter playback.
4. Toggle Android touch-vibration/haptic preference; feedback must be optional, never vibrate on a background playback event, and never trigger on reselecting the selected tab.
5. Play/pause/skip from mini-player and queue. Dismiss and restore the mini-player; confirm playback does not stop, and no duplicate Next or wrong artwork appears under rapid taps.
6. Test 320–390dp phones, landscape/split-screen and font scale up to 2.0×, alongside talkback and dark/light themes.
7. Verify memory/CPU/battery against NW49 baseline, especially fast tab changes and image crossfades. One-shot animation is not a performance or battery guarantee without measurements.

**Next:** NW51 Performance & Reliability. NW52 release preparation follows; real-device acceptance remains mandatory.
