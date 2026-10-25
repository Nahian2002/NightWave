# NW40 — Integration Stability and Build Verification

**Date:** 25 October 2026  
**Branch:** `feature/nightwave-nw40-stability-integration`  
**Based on:** `feature/nightwave-nw39-atomic-playlist-edits`  
**State:** Development source patch. **Android build and physical-device regression NOT VERIFIED.**

## Changes made

1. **Search history corruption preservation.** `SearchHistoryStore` now uses a nullable strict decoder. An oversized, malformed, non-string, duplicate or otherwise noncanonical stored history is treated as unreadable and is **never rewritten** by Record, Remove, Clear or Undo. The Search screen can still load; its other functions are unaffected. This intentionally prioritizes preserving data over silently repairing it.
2. **Isolated Android regression tests.** `SearchHistoryStoreInstrumentedTest.kt` adds tests for malformed history surviving every mutating operation and for normal record/remove/undo/clear behavior. Each test routes preferences to a private, uniquely suffixed test-only preference file and clears that file in `finally`. This test is written, not executed.
3. **Pinned playlists lifecycle refresh.** When the new Activity resumes after a classic library/playlist management screen, it refreshes the pinned-name snapshot against the current known playlist names.
4. **Offline-index failure containment.** The ordinary song options dialog now catches exceptions raised by `SearchTrackOfflineIndex.readySong`, treating unreadable offline status as not ready rather than crashing. No damaged index is repaired/deleted automatically.
5. **Bulk copy source validation.** Before showing destination options and again when chosen, selected URLs are verified against the still-existing source playlist; a stale selection aborts without touching the target. The single-writer guarded destination transaction remains unchanged.
6. **NW40 source-only preflight.** `tools/verify_nw40_stability.py` runs 17 integration checks on playlist transaction guards, UI callbacks, safe link sharing, preference isolation, recovery checkpoints and app identity. It accepts `--self-test` and `--json` modes. GitHub Actions calls both the self-test and audit before Gradle when a runner is allocated.

## Verification evidence and limitations

- **17/17 source integration invariants** were checked directly against the NW40 branch using GitHub file contents. These checks prove the expected symbols and guards exist, **not** that the Kotlin UI compiles or that the player works on-device.
- **Android CI not started:** [NightWave Actions NW40 run #40177499450](https://github.com/Nahian2002/NightWave/actions/runs/40177499450) finished with failure, `runner_id=0`, and **zero job steps**. This is an infrastructure-level block before script execution, compilation, tests or artifacts. It is not a Kotlin compiler error.
- The existing `build-windows.cmd`, `check-device-update.cmd`, and `diagnose-nightwave-build.cmd` remain available, unchanged, for full locally provisioned compilation and signing checks. No replacement APK has been produced.
- Earlier NW36–NW39 feature tests and NW40 instrumentation tests still **must run**; Android device QA and user-data preservation tests still need real evidence.

## Safe local acceptance path

1. Check out the exact NW40 branch, not the repo's older default `main` branch.
2. Run `python tools/verify_nw40_stability.py --self-test` and `python tools/verify_nw40_stability.py` on the checkout. This check only reads source.
3. Run `build-windows.cmd` in the repo root on Windows with the project's configured Android toolchain. Review `NightWave-build-full.log`, diagnostics and return code.
4. Run `check-device-update.cmd` to compare signer/app identity to the **currently installed** NightWave before installing any new APK. A different debug signer cannot safely update an existing app in place. **Never uninstall, clear data or overwrite private audio to force installation.**
5. After a successful full compile, exercise Spotify-style NW36–NW39 interactions (recent-play remove/clear, search history, downloaded-music history, playlist pin, bulk select/copy/remove, drag reorder and Undo), offline media under airplane mode, account state, queue, screen-off playback, and background service resilience.
6. The public-release gates remain separate: OAuth status 8, licensing for online-to-offline saving, permission/privacy disclosures, target SDK policy, signing and distribution checks.

## Out of scope for NW40

- This update **does not** fix Google OAuth status 8 or guarantee all YouTube searches can be downloaded for offline playback.
- No Google/Drive account credentials, signing certificate, playlist data or locally saved audio files were changed by the repository edits.
- No assertion of APK compilation success, 100% reliability or public-release readiness is made.
