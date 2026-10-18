# NightWave Phase 6V4B — Offline library integrity (non-destructive)

## Baseline
Extends `feature/nightwave-v2-phase6v4a-identity-safe-queue-recovery`.
Retains the v1 `nightwave_offline_v1` index format and all existing file IDs.
Does not change imported audio bytes, existing preferences, Google Drive content,
YouTube queues or the cloud recovery protocol.

## Problem
A file recorded in NightWave's offline index could be missing or have changed size,
yet `fileFor()` would still provide its path for playback, and the library UI
would label the row "Offline". A malformed index might also hide songs without
a clear user-facing explanation. We need to warn, not silently delete or "fix"
private media that may still be recoverable.

## Implementation
- A read-only `OfflineAudioLibrary.healthReport()` classifies recorded entries:
  `AVAILABLE`, `MISSING`, `SIZE_CHANGED`, `UNREADABLE`,
  `UNSAFE_METADATA`. If the JSON index is unreadable the report is
  `indexReadable=false` and **no index write is performed**.
- `fileFor()` now requires an exact indexed entry, a private canonical path,
  a non-symlink regular readable file, compatible extension/MIME,
  a bounded expected byte count and the exact expected file length.
- The existing import, cloud download and private file formats are unchanged.
- The normal explicit delete path refuses to remove indexed entries whose
  file status is not `AVAILABLE`. Corrupt/missing/changed entries are kept for
  manual recovery rather than purged automatically.
- The offline library screen displays file-specific warnings and an issue
  count, hides unsafe entries from the Play All queue, and blocks playback/
  upload of unavailable local files. For missing or altered entries, the
  overflow action explains that no data was deleted instead of offering an
  unsafe cleanup.
- Reads do not wait on the long-running import lock; each selected file is
  checked again by the action handler before playback, upload or deletion.
- No periodic cleanup service, index rewrite, hidden file deletion or
  background repair has been added.

## What this does NOT prove or do
- A matching byte length is not a content hash or proof the song decodes.
- A user-replaced file of exactly the same size cannot be detected by this
  metadata-only audit.
- Existing storage deletion, interruption/recovery and orphan-file
  reconciliation remain candidates for a later audited transaction phase.
- This does not automatically attach a newly imported file to an old missing
  entry. User-controlled restoration with a strong identity check is a
  separate feature and must not guess that two recordings are equivalent.
- This source branch is NOT a compiled, signed or phone-verified release.

## Regression coverage
JVM: `OfflineLibraryHealthPolicyTest`
- Recorded size, missing file, size mismatch, non-regular/unreadable files,
  invalid metadata and report status counts.

Android (compile and controlled emulator instrumentation only):
`OfflineLibraryHealthInstrumentedTest`
- Metadata audit of healthy versus truncated versus missing private files.
- Missing/altered entries remain in SharedPreferences.
- Normal delete refuses unavailable/unsafe entries.
- Unreadable/corrupt index bytes remain untouched.
- MIME-extension inconsistency blocks playback/cleanup.
- Tests use create-only UUID fixtures and restore original index settings.

## Build gate for future integrated release
1. Full Gradle `testDebugUnitTest`, `assembleDebug`,
   `compileDebugAndroidTestKotlin` must pass.
2. Do not install or invoke instrumentation on the user's personal device as
   part of build verification. Use an isolated Android emulator for runtime
   regression testing.
3. On a deliberately disposable offline library, import a song, damage the
   file length, restart and confirm the entry remains visible but unavailable.
4. Confirm imported originals, functioning tracks, playlists, previous-song
   restoration and Google Drive uploads/downloads remain intact.
5. Check signed update compatibility before any in-place APK installation.
