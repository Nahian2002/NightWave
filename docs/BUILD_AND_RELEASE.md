# Build and safe update

**Current evidence:** FINISH02 on `fix/nightwave-everyday-finish`, [draft PR #156](https://github.com/Nahian2002/NightWave/pull/156) stacked on #155. [TEST_REPORT](../TEST_REPORT.md) records exact build, JVM/Android/source/lint/signing results and limits. Read the [revised sequence and hourly schedule](quality/UPCOMING_UPDATES.md). Installed-phone signer and physical/release acceptance remain separate.

**Integrated source: `main`**, based on cumulative NW132. Later cumulative review work is on the exact branch above; old tags/branches remain historical. Follow the procedure below and retained [phone instructions](NW125_FINAL_CANDIDATE.md); require fresh receipts for the exact source built. FINISH02 is locally verified, physical acceptance pending; ART02 is next. Use the named review branch for the newest candidate.

## Local Windows build

1. Use the complete **`main`** branch for reviewed source, or the exact named review branch for a candidate. Work in a writable local folder. Preserve the existing working checkout and installed app data.
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

Repository Actions is deliberately disabled following public-source publication; no hosted CI pass is claimed. The retained workflow definition, `current-android.yml`, would run those checks followed by real Gradle JVM tests, APK assembly and Android test-source compilation. It requires at least 1,400 executed passing tests, runs lint and verifies package/version and APK signature. Artifacts are restricted to permitted private non-PR runs. GitHub's signer can differ from the installed signer.

`android.yml` is now a manual historical diagnostic workflow. Its baseline [188-command inventory](HISTORICAL_CI_SOURCE_CHECKS_20261027.md) includes 98 failures, many caused by superseded exact strings or historic hashes; those reports are not relabeled as passes. Source checks and all original JVM/device tests are retained. Current Windows preflights and real Gradle tests remain required.

## Evidence and release

See [current test evidence](../TEST_REPORT.md), [NW125 handoff](NW125_FINAL_CANDIDATE.md) and [NW122 historical benchmark evidence](NW122_PERFORMANCE_MEASUREMENT.md). The cumulative app is locally compiled/tested. Hosted runner allocation, physical-device behavior, installed signer, OAuth/Drive restoration and rights/privacy approval remain independent gates. Public release remains **NO-GO**.

[Previous build notes](BUILD_AND_RELEASE_HISTORY_20261027.md) preserve older instructions and evidence for recovery.

## Build attribution and test preparation

After SUCCESS, retain `dist/NightWave-build-provenance.json` with the APK and NW08 manifest. The builder validates the actual APK asset before promotion. App details shows the installed source ID and artifact/public certificate hashes for a new recording; revision context is informational. Follow the [NW126 protocol](NW126_BUILD_IDENTITY_BASELINE.md) to prepare a new APK-bound disposable acceptance pack. Device profile fields and actual installation/data-preservation outcomes remain pending until performed.
