# NightWave 2.0 · Phase 5R — Accessibility and Android build gate

**Version:** `2.0.0-alpha5r` · **Build code:** `34`  
**Branch:** `feature/nightwave-v2-phase5r-accessibility-build-gates`  
**Status:** Draft. Do not merge into stable `main` or install an unverified signing identity.

## Changes

### Density-stable player gestures

The mini-player used **70 raw pixels** for expand/hide, and the full player used **65 raw pixels** for drag-to-dismiss. On a high-density phone these become physically shorter motions, making unintended gestures more likely. Both use a consistent **72 dp** threshold with a shared pure-Kotlin `PlayerGestureRules.decideVerticalSwipe` policy. It rejects NaN/infinite inputs, invalid thresholds and accidental short movements.

Swipe up on the mini-player to expand; swipe down to hide. Swipe down on the expanded player's header to minimize. Explicit tap controls are still present. The mini-player's Next target is increased from 44 to **48 dp**. Album-art left/right and queue swipe-to-remove/Undo retain independent gesture detectors, and background tab swiping stays disabled while player or Up Next is open.

### Actual Android build validation (not source-only)

The existing GitHub workflow was **manual only** (`workflow_dispatch`), so it could not provide a PR build result. This phase adds a narrowly scoped `pull_request` trigger against the Phase 5Q base. When GitHub Actions is enabled and an Actions runner is available, the workflow:

1. Checks out the Phase 5R code and prepares **JDK 17**, **Gradle 8.11.1**, **Android SDK 35**.
2. Runs `tools/verify_source.py` and `tools/verify_v2_phase5r.py`.
3. Executes `:app:testDebugUnitTest`, `:app:assembleDebug` and **`:app:compileDebugAndroidTestKotlin`**. This compiles Android Compose/Kotlin and the instrumented-test source without needing an emulator; running those instrumented tests on an emulator remains a separate gate.
4. Requires a nonempty debug APK with `com.nightwave.player`, `2.0.0-alpha5r` and code `34`, then reports SHA-256.
5. Uploads a **five-day, clearly labeled UNVERIFIED-FOR-DEVICE debug artifact** only on full success.

**GitHub CI debug signing is NOT guaranteed to match the signing certificate of your installed phone app.** Even a green CI job/valid artifact does not authorize an in-place update. Use `check-device-update.cmd` on Windows with exactly one authorized phone to verify installed-vs-candidate signing/version compatibility. Never uninstall or clear data to bypass a mismatch.

The source-only verifier is also updated to require all three separate Gradle tasks, rather than accepting an outdated text fragment.

### Quality gates

- Pure Kotlin vertical gesture policy is covered by **three added JUnit tests** in `PlayerGestureRulesTest.kt`, which now has nine tests in total. They test 1×/2×/3.5× display densities, exact threshold boundaries, invalid float values, and gesture independence.
- `tools/verify_v2_phase5r.py` asserts version agreement, source policy, the CI build contract, preserved playback/queue navigation and storage safeguards.
- Historic Phase 5O static gesture expectations were updated to the density-based behavior.
- User content, local offline imports, queue, libraries, saved profiles, database read-only mirror, launcher, network/search provider logic and Android permissions are unchanged.

## Required acceptance before release

1. **CI:** Wait for the Phase 5R draft PR check. If Actions are unavailable or fail, label the build **unverified** and inspect the exact Gradle/compiler failures. Do not silently publish an older APK. CI success confirms compilation/unit tests, **not** emulator or phone stability.
2. **Windows local APK:** Download a fresh full source ZIP and run `build-windows.cmd`. Require `dist/NightWave-build-status.txt` SUCCESS, version 34, `2.0.0-alpha5r`, fingerprint and SHA-256.
3. **Installed signing:** Run `check-device-update.cmd` with the real phone before installing; continue only if signing identity and upgrade version are compatible. Preserve the old music library. No data reset or uninstall.
4. **Emulator/disposable data:** Run `connectedDebugAndroidTest` to actually execute the existing default launcher, profile, swipe-page, Up Next queue and diagnostics Android test cases.
5. **Phone navigation:** Cold launch must go to premium Home, not classic Early Access. Swipe horizontally through Home/Search/Library/You; vertical lists and profile chips must not accidentally switch pages. Enable Reduced Motion and repeat.
6. **Gesture thresholds:** On S24 Ultra, test mini-player swipe-up and swipe-down short/long drags, expanded-player header dismissal, album-art track skipping, Up Next swipe-to-remove and Undo. Short drags must not activate actions.
7. **Layouts and accessibility:** Test portrait/landscape rotation, 1.3× and 1.5× font scaling, gesture navigation/system insets, TalkBack controls, bright/dark themes and accessibility touch targets. Confirm the 48dp mini-player Next control.
8. **Playback and data:** Test imported offline music, prior track resume after app reopen, media notification, background/lock screen/Bluetooth controls, favorites and saved playlists. Keep app storage untouched.
9. **Search and crashes:** Verify saved-local search with internet disabled, public fallback with internet, shared YouTube watch URLs, crash diagnostics and app exit-history categories. If crashes recur, generate `dist/NightWave-crash-summary.txt` with the read-only capture tool.

**Do not merge this draft into stable `main` until the real Android compile, emulator and on-phone playback/navigation acceptance are complete.**
