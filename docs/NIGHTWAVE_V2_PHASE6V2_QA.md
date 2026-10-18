# NightWave 6V2 — Cross-Activity Offline Index Safety

Stacked on 6V1. No changes to Android app ID, Google Drive scopes, existing song data, APK signing, backup formats, playback or lyrics.

## Problem fixed

OfflineAudioLibrary originally used @Synchronized on its **instance** methods: manually importing files, importing Google Drive downloads and deleting audio. Multiple activities create different OfflineAudioLibrary objects, each with a different monitor. Each mutator reads and rewrites the same SharedPreferences "items" index. Overlapping operations could lose another activity's index update or leave an audio file unindexed.

## Changes

- Created a shared process-wide OfflineLibraryIndexLock.monitor used by all OfflineAudioLibrary instances.
- All three functions now hold this monitor for the entire read/check/create/move/commit/rollback operation: import(Uri), importVerifiedCloud(File, LockerRemoteTrack), and delete(OfflineSong).
- Cloud import still requires SHA-256 and MD5 verification and enforces audio limits, deduplication and original ID collision rules. A failed index commit removes the unindexed file. No remote Google Drive data changes.
- Added source regression checker tools/verify_v2_phase6v2.py to ensure all three mutators share the monitor and the prior Android/cloud features retain their safety contracts. Added it after 6V1 in CI.

## What was actually verified

- The pure Kotlin lock and concurrency semantics were compiled and executed in a local isolated JVM harness: lock identity, blocked concurrent entry, and 96 critical sections across eight competing threads passed without overlap.
- Three JUnit tests have been authored in OfflineLibraryIndexLockTest.kt, but have not executed under Gradle.
- The expression-body synchronized-lock return pattern was compiled in a minimal isolated Kotlin example.
- Full Android Kotlin/Gradle build, real multi-Activity SharedPreferences concurrency, Google Drive E2E, signed APK and phone installation are NOT verified; GitHub Actions previously failed before runner allocation.

## Limits

This is an in-process guard. The current app manifest does not specify android:process for services or activities. A future Room/DataStore migration should use proper database transactions instead of assuming an in-memory mutex handles other processes or devices. Do not replace the installed app with an unverified/differently signed APK.

The next priority remains a real combined Android build of the latest stacked branch.
