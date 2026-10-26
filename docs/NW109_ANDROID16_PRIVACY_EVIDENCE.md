# NW109 — Android 16 / Privacy Hardening Evidence

Date: 27 October 2026. Parent: `72c38ff1869d695de4c22d2877883801070760f8`.
Development branch: `feature/nightwave-nw109-android36-privacy-hardening`.

## Reproduced source-level blockers before the upgrade

1. Android module compiled and targeted **API 35**. The repository's Android Gradle Plugin **8.9.2** officially supports only up to API 35. After 31 August 2026, new Play phone apps and updates generally need **target API 36**. A partial target-only bump would leave the build plugin and SDK installers mismatched.
2. The existing application set `android:allowBackup="false"`, but on some Android 12+ OEM devices this does not stop device-to-device app-data transfer. Private music, account preferences, user-imported lyrics and local databases require explicit extraction rules.

## Safe, coordinated fix

- `compileSdk = 36` and `targetSdk = 36`, AGP `8.10.1`. Google documents AGP 8.10.x supports API 36 and requires Gradle 8.11.1, already used by Windows/CI with JDK 17.
- Update the Windows SDK installer and GitHub Actions installation to `platforms;android-36`. Retain Build Tools `35.0.0`, a supported version for AGP 8.10; retain exact identity/version checks rather than resetting an installed app.
- Preserve `android:allowBackup="false"`, add `android:fullBackupContent="false"` for older API compatibility, and use `android:dataExtractionRules="@xml/nightwave_data_extraction_rules"`.
- The new extraction XML excludes private app/device roots, files, databases, preferences and external domains from both OS cloud backup and **device-to-device transfer**. This is separate from NightWave's explicit opt-in Drive appDataFolder snapshot feature, which remains unchanged.
- Retain explicit trusted exported launcher/share activity, media library service and widget provider only; no broad storage, package-query or SMS permissions added.
- `tools/verify_nw109_android36_privacy.py` contains eight independent invalid-configuration fixtures and a positive fixture. CI runs the negative tests plus checks against the exact source checkout. No APK automatically gets installed or published.

## Verification boundary

- Source manifest and backup-rules XML parse checks plus synthetic positive/negative tests can execute without an Android SDK; passing here **does not prove** merged-manifest behavior, app installation, Android resource compilation, runtime media service behavior or real OEM backup enforcement.
- API 36 behavior changes require specific device tests: edge-to-edge status/nav inset visibility, predictive back with Compose `BackHandler`, large-screen layout and rotation, media foreground service/background focus, notification permission, WorkManager quotas, private-data D2D rules, player/background playback and Google Drive account/restore.
- Google Play release also requires actual OAuth publication, a usable privacy policy/accurate Play Data Safety forms, no uncured music-source rights restrictions, signer continuity and full-device/manual acceptance.

## Release decision

**NO-GO.** Both the full Android/JUnit build and minimal hosted-runner canary have failed before executing jobs in recent milestones. That infrastructure failure is not a Kotlin compilation result. Android 16 compilation and runtime must still be validated on a working Windows/Android toolchain.

No change to `com.nightwave.player`, minSdk 26, versionCode 35, versionName 2.0.0-alpha5s, Google OAuth package/SHA, app signer, offline music, Play Next queue, library or credentials. Never uninstall/clear the existing installation to bypass a signer mismatch.

References:
- https://developer.android.com/build/releases/agp-8-10-0-release-notes
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/about/versions/12/behavior-changes-12
- https://developer.android.com/identity/data/autobackup
- https://support.google.com/googleplay/android-developer/answer/11926878
