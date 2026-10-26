# NW75 Windows build hotfix 2 — NW34 offline readiness preflight

**Base:** `fix/nightwave-nw75-phase6p-preflight`  
**Cumulative branch:** `fix/nightwave-nw75-nw34-offline-gate`  
**Public release:** NO-GO. Full Android compilation and Samsung QA remain unverified.

## User's actual build outcome

The corrected Phase 6P branch passed Phase 6P and later source checks through NW07, then stopped at **NW34 offline storage and source safety preflight**:

- `FAIL: offline contract missing: app\\src\\main\\java\\com\\nightwave\\player\\offline\\CatalogDownloadsActivity.kt`
- `FAIL: NW34 offline contract preflight: 1 error(s).`
- Android Gradle **never started**; no new APK was published or verified. The prior `NightWave-debug-previous.apk` must not be mistaken for an NW75 build.

## Cause

The original NW34 contract expected the exact source call `OfflineDownloadStatusPolicy.isReady(` to appear **directly in `CatalogDownloadsActivity.kt`**. Later NW62 implementation improved accuracy by moving file-health status behind a generation-fenced asynchronous policy:

1. `CatalogDownloadsActivity.kt` reads real `OfflineAudioLibrary(applicationContext).availableIds(ids)` off the UI thread and checks `DownloadFileVerificationPolicy.ready(...)`.
2. `DownloadFileVerificationPolicy.ready()` requires `!checking && !verificationFailed` and delegates to `OfflineDownloadStatusPolicy.isReady(...)`.
3. `OfflineDownloadStatusPolicy.isReady()` only marks a succeeded WorkManager transfer ready if its saved ID is still in `currentlyAvailableIds`.

The later implementation is **stronger**, and the NW34 direct-call source-text check had become obsolete.

## Fix

Updated ONLY `tools/check-nw34-offline-integrity.ps1` production-check expectations (plus these docs). The updated NW34 checker validates all **20/20** current source contracts, covering the old storage/transfer/source restrictions AND the actual NW62 delegated private-file health safeguards. No app Kotlin source, media ID, download file, signing key, cloud scope, library index or data file was changed.

## Validation performed

- Re-read exact GitHub committed NW34 checker and all **15** referenced source files; **20/20 required source tokens present**, zero missing files.
- Reviewed the next read-only NW40 and NW52 gates against **36 sampled required tokens** from **13** source files; **36/36 found**, but these were **not full Windows PowerShell executions** and do not establish all later NW59 checks.
- No Windows PowerShell 5.1 execution, complete Gradle run, instrumentation or device test was performed from the assistant runtime.

## Safe Windows rebuild

1. Download the **complete latest branch ZIP** and extract to a **fresh** Windows folder; do not merge into the prior failed build folder.
2. Run `build-windows.cmd`. This reruns every source contract, then Gradle/JUnit/Android compilation if all gates pass.
3. If it stops again, run `send-build-report.cmd` and share the **new** `NightWave-build-diagnostic.txt`, `NightWave-build-errors.txt` and `NightWave-build-status.txt`. Include the full build log if Gradle starts.
4. Never install `NightWave-debug-previous.apk` as NW75; do not uninstall/clear the installed app or delete existing offline recordings to bypass an error. Run `check-device-update.cmd` before attempting a signed APK update.

**Release remains NO-GO until a current, complete Android Gradle build and Samsung acceptance tests succeed.**
