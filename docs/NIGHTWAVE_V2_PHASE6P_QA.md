# NightWave 6P — Build Stabilization & Integration QA

This is a draft stacked on 6O4. Stable main and installed phone data were not modified.

## Current build status
GitHub Actions run 39100510248 (6O4) failed with no assigned runner (runner ID 0), zero executed steps and no job logs. This does not establish a Gradle/Kotlin source failure, and it does not prove an Android build passed. The external root cause (GitHub Actions availability, account/repository setting, quota/billing, etc.) is not confirmed. No runner repair is claimed.

## Integration issue corrected
The 6O4 Library Play list and Shuffle play commands supplied an ordered list but used the generic player method. PlaybackQueue.replace() intentionally preserved the previous Shuffle setting. Therefore a subsequent Next could unexpectedly randomize a user-requested saved-order queue.

6P adds a scoped Library play route to StudioPlaybackBridge. The only Library quick actions pass an explicit intent extra to the existing Media3 PlaybackService. The service calls PlaybackQueue.useExplicitDisplayOrder() on both its same-queue fast path and its replacement path, clearing the previous shuffle-cycle markers as well as the Shuffle flag. Ordinary song taps do not set the extra, so their previous Shuffle selection continues working. Repeat mode is not reset. Button enabled state now requires at least one valid canonical public watch entry.

Five JVM unit tests were authored for sequential queue advancement, stale-shuffle-cycle cleanup, unaffected normal replacement, retained repeat setting and empty queue handling. Tests have not yet run under Gradle.

## Fail-fast local verification
A Windows PowerShell 5.1-compatible local-only script, tools/check-phase6p-integration.ps1, checks key cross-phase source wiring and stable app identity with no Gradle/SDK/network or Android phone access. It runs before Java/Gradle provisioning through the existing build-windows.cmd -> tools/build-windows.ps1 route. Failure exits without publishing a new APK and uses the existing diagnostic output.

The existing PowerShell parser gate now parses this new script too. The GitHub Actions workflow also includes tools/verify_v2_phase6p.py after earlier phase source checks. None of these source gates replace Kotlin compilation or Android device tests.

## Combined QA when a build runner becomes available
1. Resolve GitHub Actions runner/account constraints in the repository Actions run UI, or build on Windows using the existing build-windows.cmd (no GitHub runner required). That script performs Gradle JVM tests, APK compilation, instrumentation-test-source compilation and signer/identity checks.
2. Inspect the local build status and the privacy-filtered NightWave-build-diagnostic.txt only if a build fails. Never install an old APK marked as the latest build.
3. Use the read-only check-device-update.cmd to compare signer compatibility with the currently installed com.nightwave.player app before any installation. Do not uninstall, clear data or bypass signing checks.
4. Run the deferred device batch across 6N2–6O4: delayed Next and retries, saved listening level, public search and source identity, creator views, sorted/filtered playlist playback and explicit Play list/Shuffle play under both previously enabled and disabled Shuffle.
5. Confirm that saved playlists, local offline imports, resume checkpoints, existing release signing and backup metadata survived without mutation.

## Unverified
Actual Kotlin/Gradle compilation, 6P JVM test execution, Android hardware audio/playback, debug signer continuity, installation/update, and release readiness. The draft branch must remain unmerged until the build/integration gates are confirmed.