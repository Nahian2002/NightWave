# NW59 — Cumulative pre-build hardening for NW53–NW58

**Parent:** `feature/nightwave-nw58-player-overlay-polish`  
**Branch:** `feature/nightwave-nw59-integrated-build-gates`  
**Public release readiness:** **NO-GO**. Android compilation, full JVM tests, installer-signature match, Google OAuth, and phone rendering require actual confirmation.

## Why this batch exists

Earlier NW52 local builds repeatedly stopped before Gradle due to stale source-preflight rules, invalid Compose imports and parameter binding errors. The last provided Windows build reached 1,022 JVM tests with one failed cloud authorization assertion (subsequently corrected). None of the user logs confirm that a *later* NW53–NW59 APK has compiled or passed runtime tests.

NW53–NW58 added six source/UI batches, while the standard `build-windows.cmd` ran only the old NW52 Windows cumulative guard. New Python source guards existed in hosted CI, but the user's Windows builder must work **without Python** and hosted GitHub jobs have ended with zero steps/runner_id 0.

## NW59 improvements

- Introduce `tools/check-nw59-integration.ps1`, an ASCII-only Windows PowerShell 5.1-compatible **read-only** gate validating the presence and source wiring of all six NW53–NW58 upgrades. Missing files or broken required actions stop the build before downloading/launching Gradle.
- Add a standalone `-SelfTest` with positive, missing-invariant, forbidden-invariant and Home section-order fixtures. No source tree is accessed in self-test mode.
- Bind the gate to `tools/build-windows.ps1` after NW52's existing check and **before** NW08's full-source fingerprint, so the script and changes become part of the build snapshot.
- Add the NW59 script to `tools/check-builder-syntax.ps1` so PowerShell parses it and checks ASCII portability; verify project-root resolution is deferred until after parameter binding. Avoid repeating the earlier `Join-Path` empty-string preflight bug.
- Run both NW59 modes on the hosted PowerShell job whenever a runner is available. CI cannot be counted as passing when no steps execute.
- No Android permissions, authentication scopes, private music files, downloaded audio, queue algorithms, database formats, or application version numbers were changed in this batch.

## What the new gate checks

| Batch | Source contract |
|---|---|
| NW53 Google recovery | Stage-specific failure guidance; Google-owned account picker is not launched behind a NightWave modal |
| NW54 Search | Consumer headings without raw provider domain; selected search result keeps its single-song queue |
| NW55 Offline/Downloads | File-health check, actual saved-file verification, accessibility labels, minimum action height and honest restart semantics |
| NW56 Themes | Dark readable Pearl accent and foreground; Sound & Playback heading contrast |
| NW57 Home | One hero, actual listening history above statistics; Continue, Remove and Discover remain accessible |
| NW58 Now Playing | Opaque transition backdrop retained throughout enter/exit and bounded animation state |

## Evidence levels and no-go criteria

- **Source checks:** can validate required textual contracts and file presence, not Kotlin type inference, actual runtime screens, correct Google Cloud registration or music rights.
- **JVM/Gradle:** must be executed on the *exact* current NW59 ZIP: `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`. An APK from a failed run is not verified.
- **APK identity:** use `check-device-update.cmd` with the generated APK before installing. Preserve the old app install, signer identity and local music.
- **Device QA:** verify a real error-8 recovery/copy report, Search, Offline and Downloads, Pearl, first-fold Home, overlay motion, screen insets, real offline playback, Bluetooth/background audio and zero unexpected queue entries.
- **Public release:** no-go until Play SDK/rights, privacy, production signing and externally required setup have independently passed.

## Correct local invocation

Use the GitHub branch ZIP, extract to a **new** folder, then run `build-windows.cmd`. The build now runs NW59's self-test and actual source check automatically without needing Python or GitHub Actions.

If another issue occurs, supply this attempt's `dist/NightWave-build-diagnostic.txt`, `dist/NightWave-build-errors.txt`, `dist/NightWave-build-status.txt` and `dist/NightWave-build-full.log` if written. Do not uninstall NightWave or clear its data.
