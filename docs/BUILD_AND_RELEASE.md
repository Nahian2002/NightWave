# Build and safe update

Updated 27 October 2026. **Current complete source for private final build: `release/nightwave-nw125-final-candidate`** (contains NW111–NW124, including NW123/NW124 changes not yet fully compiled). `main` is an older v0.8 snapshot. The last independently verified Android build belongs to NW122, not NW125. Follow [NW125 final candidate instructions](NW125_FINAL_CANDIDATE.md).

## Local Windows build

1. Clone or download the complete **`release/nightwave-nw125-final-candidate`** branch into a new writable folder. The present checkout is `C:\Users\jayed\NightWave-work`; Windows refused writes inside the original OneDrive workspace.
2. Run `build-windows.cmd`. Cached Gradle/SDK downloads are reused. Read and accept SDK licenses yourself when required.
3. Require `BUILD SUCCESSFUL` and a fresh `dist/NightWave-build-status.txt`. The builder runs source preflights, JVM tests, debug APK assembly and Android instrumentation-source compilation.
4. Review `dist/NightWave-jvm-test-proof.txt` and source fingerprint. Failed or interrupted attempts must not promote a stale APK.
5. Keep local outputs, keys and diagnostic logs outside Git.

The package remains `com.nightwave.player`, version code `35`, name `2.0.0-alpha5s`; compile/target API 36, minimum API 26. AGP 8.10.1, Gradle 8.11.1 and Kotlin 2.0.21 remain pinned.

## Before installing

Run `check-device-update.cmd` with the phone connected and verify the candidate signer against the installed app. A verified APK signature proves internal integrity; it does not prove installed-phone continuity unless that comparison ran. Do not uninstall NightWave, clear storage, replace keys or reset a Google account to force an update.

Use the existing [139-case acceptance matrix](NW90_EXPANDED_139_CASE_MATRIX.csv) for the consolidated phone cycle. Include small-screen/large-font layout, Bangla/TalkBack lyrics, playback/headset routes, queue restart, offline files and disposable Drive backup/readback/restore. No installation or device mutation is automated by this update.

## Automated verification

`python tools/verify_current.py` runs current Python structural/negative checks and, on Windows, the same source safety preflights used by the local builder. It invalidates stale source-check receipts before running. This is source validation only.

The current CI workflow, `current-android.yml`, runs those checks followed by all real Gradle JVM tests, APK assembly and Android test-source compilation. It requires at least 1,400 executed passing tests also runs Android lint and verifies package/version and APK cryptographic signature before retaining a debug candidate. GitHub's signer can differ from your local installed signer.

`android.yml` is now a manual historical diagnostic workflow. Its baseline [188-command inventory](HISTORICAL_CI_SOURCE_CHECKS_20261027.md) includes 98 failures, many caused by superseded exact strings or historic hashes; those reports are not relabeled as passes. Source checks and all original JVM/device tests are retained. Current Windows preflights and real Gradle tests remain required.

## Evidence and release

See [current test evidence](../TEST_REPORT.md), [NW125 handoff](NW125_FINAL_CANDIDATE.md) and [NW122 last compiled evidence](NW122_PERFORMANCE_MEASUREMENT.md). Earlier NW122 Android compilation was verified, but this final cumulative branch is **not yet compiled**; hosted runner allocation, physical-device behavior, installed signer, OAuth/Drive restoration and rights/privacy approval are independent gates. Public release remains **NO-GO**.

[Previous build notes](BUILD_AND_RELEASE_HISTORY_20261027.md) preserve older instructions and evidence for recovery.
