# NightWave Phase 6G — Playback Continuity & One-File Build Diagnostics

**Draft source branch:** `feature/nightwave-v2-phase6g-playback-continuity-diagnostics`, stacked on 6F. No music-file migration, cloud schema change, signing change, version bump or stable-main merge. **No verified Android build or real-device playback testing yet.**

## Correctness fixes

### 1. Exact selected-track resume from the Activity

Phase 6D protected service-side queue resume, but the 6F `StudioPlaybackBridge` could still bypass that check when a MediaController retained any non-null old `currentMediaItem`:

- A loaded public video could be replayed after a saved queue switched to a different public track.
- A loaded public video could be replayed after transitioning to imported offline audio (or vice versa).
- Clicking Home's **Pick up where you left off** could incorrectly act on loaded media instead of the selected queue entry.

Phase 6G adds pure `PlaybackCommandPolicy` with exact-ID checks. The Bridge now derives the intended public ID from **the selected saved-queue entry** (`state.queue[state.queueIndex]`), even when the Media3 player has no loaded MediaItem after a cold activity reopen. For selected offline audio, it uses the exact `offline:<id>` that the player state reports.

If loaded media matches the selected ID and there is no player error, the controller may seek/prepare/play directly. Otherwise the bridge requests `ACTION_RESUME_SAVED` or `ACTION_RESUME_OFFLINE` from the service, which revalidates queue/file ownership and its own persisted bookmark. It does not indiscriminately resume an unrelated media item, nor perform extra stream extraction for already-loaded matching media. An already playing song remains undisturbed by the idempotent Spotlight resume.

### 2. Explicit local-audio Retry after media loss

The service's `ACTION_RETRY_CURRENT` previously blindly did `prepare(); play()` for offline mode, even when the Media3 player had no media item or still held an unrelated item. The updated path checks that the loaded item is exactly `offline:` + the selected private audio ID. If not, it calls the preexisting **user-requested, validated** `resumeOfflineSession()` path, which accepts only IDs registered in the app-owned library and restores the saved checkpoint. No new file path, external URI or permission is introduced.

### 3. Single privacy-filtered Windows build report

- Run `send-build-report.cmd` in the root of the complete NightWave source ZIP after a local `build-windows.cmd` attempt.
- It runs the existing **offline, no-network, no-ADB** `tools/diagnose-build.ps1`, which reads ONLY local generated `dist/NightWave-build-status.txt`, `dist/NightWave-build-errors.txt` and `dist/NightWave-build-full.log`.
- It creates or refreshes **one** `dist/NightWave-build-diagnostic.txt` for support, explaining that missing logs are not proof of compilation success. Windows build-failure instructions now mention this one report.
- Source paths and Authorization headers are redacted as before; added common URL query credentials (`token`, `access_token`, `refresh_token`, `api_key`, `client_secret`, `signature`, `sig`, `code`) and direct token assignments. The built-in synthetic redaction self-test covers URL token handling.
- No automatic upload, analytics, phone access, account access, music collection, APK install or deletion of existing saved tracks. **Review the resulting text before sharing**; automated redaction cannot guarantee removal of every possible secret or sensitive path.

## Source and test changes

- New pure policy: `app/src/main/java/com/nightwave/player/v2/playback/PlaybackCommandPolicy.kt`.
- Source integrations: `StudioPlaybackBridge.kt` and `PlaybackService.kt`.
- Eight authored JUnit checks: `PlaybackCommandPolicyTest.kt`, covering exact public and offline song matching, mismatched cached media, error state and blank selections.
- New structural test gate `tools/verify_v2_phase6g.py` wired after the earlier 6F checks in the manual Android workflow.
- **Local isolated Kotlin smoke:** a command-line Kotlin compiler built a standalone copy of the pure ID policy and passed 14 assertions. This is **not** Gradle, a complete JUnit run or a device test.

## Deferred acceptance steps

1. Download the complete 6G branch and run `build-windows.cmd` on Windows. Read-only source/import checks run before Gradle/SDK. Confirm the actual `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` result from this **exact branch**.
2. If the Windows build fails, use **`send-build-report.cmd`**, review and send **`dist/NightWave-build-diagnostic.txt`**. The complete original build log stays local unless needed for targeted follow-up.
3. On a disposable emulator, start public song A, switch queue selection to B before A completes, and issue Resume or Play: A must never restart, and B must be selected. Repeat public/offline transitions in both directions and after service/activity recreation.
4. Start an imported offline song; interrupt Media3 so current item disappears, then tap Retry and verify only the registered selected offline ID is restored from checkpoint. A deleted/missing file must fail without copying or opening external paths.
5. Regress Home Spotlight idempotency, repeat-one seek, 6D extractor timeout and pending Play/Pause, mini-player X and swipe hide, 6E status panel, imported LRC lyrics, 6C/6C2 private listening-memory consent and erase, 5U/5Z backup and safe merge.
6. On 320dp/360dp/412dp phones and tablet, check that user-visible Retry and Reconnect remain reachable under fonts, accessibility settings and dark/light themes.
7. Before any phone upgrade, compare exact APK package/version/signer using `check-device-update.cmd`. Never uninstall or clear existing NightWave user data to solve a debug-signature mismatch.

## Build status

The most recent 6F GitHub Actions PR attempt (#43, run #39055899355) returned a job failure in around three seconds, with **no assigned runner, no executed job steps, and no accessible compiler logs**. That does **not** establish Android compilation failure or success. Phase 6G inherits the same gap until an actual Windows/Gradle run succeeds. This PR remains draft and stable main is untouched.
