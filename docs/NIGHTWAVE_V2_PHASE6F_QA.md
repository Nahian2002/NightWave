# NightWave Phase 6F — Build Stabilization & Compatibility

**Development branch:** `feature/nightwave-v2-phase6f-build-stabilization`, stacked on Phase 6E. Stable `main` is unchanged. This phase is a non-destructive source/build verification project: it does NOT add music features, migrate files or require installing an APK.

## Verified issues from repository/history

- An **older Phase 5S Windows build log** provided in the conversation failed at `:app:compileDebugAndroidTestKotlin` because `androidx.compose.ui.test.assertExists` and `assertDoesNotExist` were imported as functions. They are node assertions and do not need those imports. **That log is not a build result for Phase 6E/6F.**
- Stacked source checks had become outdated despite later features being intentional: Phase 5Y expected the old Home `Made for your listening` heading, while 6A2D renamed it to `Made for you`; Phase 6C expected `prefs.edit().clear().commit()` instead of the later consent-revision-safe `clear().putLong("revision", next).commit()`, and looked for Profile-only refresh rather than Profile/Home.
- Current GitHub Actions PR filters previously covered Phase 5R–5U bases but **not Phase 6E**. As a result, stacked Phase 6E review PRs did not produce an actual Gradle verification run. Phase 6F now targets 6E and adds that base explicitly for this verification PR.
- No Gradle wrapper is committed. The existing Windows builder installs/verifies Gradle 8.11.1, Java 17+ and Android 35. CI uses those versions. This is intentional; a missing wrapper is not silently treated as build success.

## Phase 6F changes

1. Update inherited 5Y/6C source assertions to match **actual safe behavior**, without reverting UX improvements or loosening privacy guarantees.
2. Add `tools/verify_v2_phase6f.py` as a repo-wide, read-only source gate: scans every `app/src/**/*.kt` for the 5S-style invalid Compose assertion imports, illegal top-level `weight` import and duplicate imports; asserts app ID, version, Android test dependency, Gradle plugin versions and current functional invariants.
3. Add `tools/check-kotlin-compat.ps1` to provide the same essential fail-fast checks with **Windows PowerShell 5.1 only** (no Python needed). The existing `build-windows.cmd` → `tools/build-windows.ps1` now runs this preflight **before downloading Gradle/Android SDK**; builder syntax validation covers the new script.
4. Enable **Gradle compile verification** on pull requests targeting the current Phase 6E development branch. CI runs inherited source checks, `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, APK package/version inspection, and (only after success) uploads a short-lived development debug APK. This **does not** install on a phone, prove real playback correctness or assure signer compatibility with an installed copy.
5. Remove stale Phase 5I wording from the read-only `check-device-update.cmd` launcher.
6. Keep a full diagnostic trail and report **source checks vs actual compiler results** separately. Do not claim any APK is verified unless Gradle and identity checks actually succeed on this branch.

## CI caveats

GitHub Actions availability depends on the linked repository's quota and policy; a workflow can be queued, blocked or fail before Kotlin compilation. Report that status accurately. Runner-generated debug APKs are signed with a CI-specific identity and should **not** be installed over a user's existing data without an explicit read-only signer check. A successful compile also does not replace a real Android phone playback test.

## Device-signing safety

- The Android app identity remains **`com.nightwave.player`**, **versionCode 35**, **versionName 2.0.0-alpha5s**, with no data-store schema migration.
- Windows build script preserves previous APK/report on failure and does not uninstall or clear any app data.
- Before an upgrade attempt, run `check-device-update.cmd` on Windows with one authorized phone; inspect its signature comparison report. A mismatch is a **STOP**, not a reason to uninstall.
- To test on an emulator, use a disposable virtual device with no personal music accounts. The Android test suite includes emulator-only tests that modify private test preferences.
- Physical-device tests still require explicit approval and the proper APK signer.

## Acceptance checklist

1. Run `python3 tools/verify_v2_phase6f.py` and all inherited source checks; distinguish stale contract failures from Kotlin compiler failures.
2. On Windows, open the complete **6F branch ZIP** and run `build-windows.cmd`. Confirm preflight prints PASS before toolchain installation, then save `dist/NightWave-build-status.txt`, `dist/NightWave-build-errors.txt` and full log.
3. Run the CI compile/review PR and inspect `testDebugUnitTest`, `assembleDebug`, `compileDebugAndroidTestKotlin` and the APK package/version verification steps.
4. If Android compilation fails, repair exactly the erroring Kotlin/Java/Gradle lines, push to this branch and rerun tests. Never promote an unverified old APK as the new version.
5. Regress the previous 6E status/reconnect, 6D extractor timeout and pause, 6C2 trends, 6B private lyrics, 6A2 mini-player and 5U/5Z cloud controls after a successful Android build; preserving source files alone is not evidence of functional behavior.
6. Verify splash/relaunch/background music on a disposable emulator, then—only when approved—on a physical phone after signature comparison.

**Status:** 6F source changes and build gates committed. Consult the **actual GitHub Actions run** associated with the 6F head for build result; never infer compilation success from static inspections. Stable main remains untouched.
