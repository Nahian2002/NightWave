# NightWave Phase 6H — Release Gates and Exact-Track Resume

**Development branch:** `feature/nightwave-v2-phase6h-release-gates-resume-safety` stacked on 6G, NOT stable main. No APK install, app-data reset, library migration or cloud-account change. **Full Android compilation is still unverified.**

## Real defect fixed: Home Resume while the wrong track is playing

Earlier phases guarded direct `MediaController.play()` on a nonmatching loaded item. However, `StudioPlaybackBridge.resumeCurrent()` still returned unconditionally at `active.isPlaying -> Unit`. If public song A remained playing while the saved queue selection was B, Home's resume control could incorrectly report success and leave A active.

Phase 6H introduces `PlaybackCommandPolicy.resumeSatisfied(selectedId, loadedId, playerHasError, isPlaying)`: already-playing is considered complete **only when the loaded Media3 ID exactly matches the selected saved queue/offline track** and the player has no error. A mismatch continues through the existing verified service-based `ACTION_RESUME_SAVED` or `ACTION_RESUME_OFFLINE` path. It does not invent a stream, seek another track's position or erase any music. Play/Pause's existing semantics and on-screen mini-player X/swipe remain unchanged.

Two additional pure JUnit tests guard same/different public songs, public/offline transition, no loaded item, errored playback and paused state. The previous 6G source gate was updated to assert the stronger behavior rather than the obsolete unconditional shortcut.

## CI diagnostic and build-gate upgrade

- The stacked PR's GitHub Actions `pull_request` filter now explicitly includes the **6G branch** (the 6H base); previously the latest source branches did not receive automatic checks.
- Source-only verification runs **immediately after checkout**, before Java/Gradle/Android SDK installation. That means a stale source gate fails quickly instead of wasting an SDK download; it still requires an allocated GitHub Actions runner.
- After source checks, the existing Java 17/Gradle 8.11.1/Android 35 workflow runs the same actual Gradle unit tests, APK assembly and Android instrumentation-test **compilation**: `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
- New `tools/ci_compile_summary.py` is stdlib-only: reads only the last 4MB of the CI Gradle log and writes a diagnostic-only file with up to 65 redacted compiler/dependency failure lines. This includes common authorization headers, access-token URL parameters and user-home path masking. Its synthetic redaction/self-test runs during source checks.
- When the workflow runner **executes** and a Gradle step fails, the job uploads a short-lived **`NightWave-6H-build-diagnostic-NO-APK`** artifact. The output cannot be mistaken for a signed release. The normal short-lived debug APK artifact remains success-only and still needs installed-signature comparison.
- If GitHub Actions fails **before a runner is assigned**, it cannot run the source gate or generate any diagnostics. A missing artifact is not evidence of successful compilation. In that case run `build-windows.cmd` and `send-build-report.cmd` from the complete source ZIP on Windows.

## Tests actually performed for this implementation

- Fetched the pure `PlaybackCommandPolicy.kt` code from GitHub and compiled a local equivalent with the available **Kotlin/JVM 1.9 compiler**; **22 isolated assertions passed** for direct ID matching, cross-source mismatch, selected media absent, error and currently-playing state. This is a narrow policy check, not Android compilation.
- Reconstructed the committed stdlib `tools/ci_compile_summary.py` source locally, checked its Python syntax, ran its synthetic credential-redaction self-test, and verified its missing-log fallback generated a truthful **No terminal Gradle result** report.
- All Android activity/service/Media3 integrations, the complete Gradle/JUnit suites, APK signer continuity, emulator tests and phone playback remain **UNVERIFIED**.

## Next acceptance gates (non-destructive)

1. Check this branch's actual GitHub Actions status. If no runner executes, report an infrastructure block—not a Kotlin failure.
2. On Windows, download the latest 6H branch source ZIP, extract and run `build-windows.cmd`. Do not mix ZIP versions. If it fails, run `send-build-report.cmd`, review and share `dist/NightWave-build-diagnostic.txt` (private information may remain in unusual log formats).
3. Repair real compiler and test failures at their exact source lines and rerun until `testDebugUnitTest`, `assembleDebug`, `compileDebugAndroidTestKotlin` and APK identity checks succeed on the same commit.
4. Then test the A-playing/B-selected Home Resume defect on a disposable emulator, plus lost MediaController, 6D lookup timeout, public/offline switching, offline Retry, Repeat One, paused loading, 6B lyrics, mini X, 6C2 history privacy/erase and Google Drive backups.
5. **Before any physical-device installation**, use `check-device-update.cmd` to compare APK signatures with the installed NightWave. CI uses a potentially different debug signing key. **Never uninstall or clear app data to bypass a mismatch.**

**Status:** Phase 6H committed as a draft verification milestone, NOT a stable Android release. Real APK build success must be demonstrated by Windows or GitHub runner output.
