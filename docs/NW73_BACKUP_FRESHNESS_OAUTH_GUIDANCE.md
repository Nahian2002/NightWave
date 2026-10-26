# NW73 — Honest Google backup freshness and stage-aware error 8 recovery

**Parent:** `feature/nightwave-nw72-cloud-session-restore-safety`  
**Cumulative source:** `feature/nightwave-nw73-backup-freshness-error8-guidance`  
**Status:** Source committed. **Full Android Gradle/JUnit compilation and successful Google connection NOT verified. PUBLIC RELEASE: NO-GO.**

## Real source-level issues

1. `CloudAccountPresentation.stage(status)` previously called a three-argument UI helper that checked only `account`, `enabled`, and `lastSuccessMs`. If a user made a new privacy choice after a verified snapshot—such as enabling history or imported lyrics—those fields remained unchanged and the Account screen could still claim **Drive backup verified**. The old snapshot did not necessarily include the newly selected optional data.
2. Older verified installs have no record identifying the exact consent configuration for the last successful upload; a historical timestamp by itself cannot establish that the **current** choices were verified.
3. `CloudConnectionHelpPolicy` described Google internal status **8** as though the account picker had appeared regardless of whether the failure happened at `REQUEST` (before selection) or `ACCOUNT_PICKER`. This could confuse troubleshooting the existing Google connection problem.

## Implemented safeguards

- Added read-only `CloudBackupFreshnessPolicy` to classify local-only, connected/pending, previously verified but needing refresh, and verified for **exact current consent revision**.
- `CloudBackupState` saves `last_verified_consent_revision` only when a backup operation reports success after its original Drive copy-and-readback verification. The receipt is reset when disconnecting or switching Google accounts. Existing users without the new receipt conservatively see **Backup needs refreshing** until a new verified backup succeeds. Their previous Drive snapshots are never deleted.
- Changing an actual backup/privacy setting increments the existing revision and updates the status to **Backup settings changed; back up now to verify the current choices**. Repeatedly choosing an identical setting is a no-op and does not falsely invalidate a verified receipt. **Automatic backup scheduling is not turned off** by these changes.
- `CloudAccountPresentation.stage(status)` now distinguishes an older verified snapshot from one covering the current backup/privacy choices. The Account hub labels the earlier timestamp **Previous verified backup** and explains how to refresh it.
- Error **8** guidance now respects the reported stage: **before the account picker** versus **after account selection**. It does not claim that a visible picker proves Drive authorization and does not automatically relaunch sign-in or broaden the `drive.appdata` scope.
- Saved songs, playlists, offline files, Drive snapshot format and remote files, privacy defaults, package/signing identity and worker schedule remain unchanged.

## Actual verification and limits

- The exact committed `CloudBackupFreshnessPolicy.kt` was independently copied, compared by `git hash-object` to its GitHub blob SHA-1 `66a3cbcf1373fe65c68ba1bcce581d2aaa3d8ff0`, compiled with local Kotlin/JVM 1.9, and passed **8,009/8,009** standalone freshness/toggle/revision assertions. The first test harness had an enum-reference compile issue; the harness was corrected and rerun successfully. Production policy code was not changed to fix the harness.
- Added `CloudBackupFreshnessPolicyTest.kt`, enhanced `CloudAccountPresentationTest.kt` and `CloudConnectionHelpPolicyTest.kt`, and committed a reproducible `tools/NW73CloudStatusSmoke.kt` source.
- **Not established:** Android compilation, all project JUnit tests, Android UI rendering, true Google error-8 resolution, actual Drive consent/upload/readback, correctness of external Google Cloud client registration, or on-device recovery. Source guards and isolated Kotlin assertions are not a complete app build.

## Later Samsung acceptance and OAuth setup

1. With a verified compatible APK signer, connect a test Google account and make a small, explicitly consented metadata-only backup. Verify that the upload and Drive readback succeed before expecting **Drive backup verified**.
2. Enable optional history or imported-lyrics backup. The Account hub should say **Backup needs refreshing** and show the prior verified date, not falsely claim the previous snapshot covered newly enabled data.
3. Tap the same privacy option without changing its value (e.g., a redundant settings operation). It must not invalidate the existing receipt. Change Wi-Fi-only scheduling and confirm the revised preferences apply to future workers.
4. Run **Back up library now** again. Only after a successful readback should the Account stage return to **Drive backup verified**, referencing the current settings.
5. Reconnect the same account, disconnect, or switch accounts: stale backup state and restore previews must not be treated as current; no previously saved remote snapshot or locally downloaded song should be deleted.
6. Trigger a controlled `REQUEST`-stage Google failure and an `ACCOUNT_PICKER`-stage failure where feasible. Neither should incorrectly describe the other's stage. Google status 8 is **not** a proven code-only OAuth fix.
7. Run `build-windows.cmd`, `check-google-oauth.cmd` and `check-device-update.cmd`, then compare the installed APK package/SHA-1, Google Cloud Android OAuth client, Drive API enablement, consent test users and Play services state. **Do not uninstall NightWave or clear app data** to work around the signer or OAuth issue.

**Still blocking public distribution:** full Android build and instrumented tests, phone OAuth/Drive round-trip and privacy consistency, real playback/search/download QA, source availability/rights, and release compliance.
