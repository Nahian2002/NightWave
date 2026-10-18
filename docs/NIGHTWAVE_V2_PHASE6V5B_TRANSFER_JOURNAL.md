# NightWave Phase 6V5B — Local transfer journal and recovery readiness

## Scope
Extends the complete Phase 6V5 Music Vault browser and the earlier Phase 6V4
playback, offline index and service-recovery safeguards. Adds no backend,
subscription, Google permission, remote mutation, background transfer worker,
MP3 conversion, or stream-to-file downloader.

## Implemented
- **On-device journal**: up to 30 recent transfer attempts in an app-private,
  versioned, bounded (16 KiB) `AtomicFile`. Only a random UUID, direction,
  sanitized human-readable title, file size, status and device time are stored.
  OAuth tokens, Google account/email, Drive file IDs, resumable session URLs,
  local file paths, signed stream URLs, cookies and exception texts are not
  written as journal fields.
- **Truthful lifecycle**: a transfer journal entry starts *only after*
  successful explicit Google authorization, before work begins. It is marked
  `COMPLETED` only after the actual verified Drive upload outcome or
  checksum-verified download **and successful offline import**. An existing
  matching verified copy is `REUSED`. Errors become `NOT_VERIFIED`.
  A process crash leaves `STARTED`, displayed as **Unconfirmed**, not as
  successful or necessarily failed.
- **Journal failures isolated**: transfer history is optional. A damaged
  journal is not silently overwritten. If journaling fails, the existing
  upload/download verification result still decides playback/data outcomes.
  A user-confirmed **Clear local transfer history** action deletes only
  the history file, never an offline song, Drive copy or download checkpoint.
- **Read-only interrupted-download preview**: a previously saved recovery
  checkpoint is classified for current account, temporary-file existence,
  symlink/canonical path safety, byte length and final-checksum-pending state.
  The preview does not open network sessions or rewrite/delete temp files.
  If an existing zero-checkpoint file or a shorter/missing committed file is
  found, **Resume is hidden** and the user is directed to explicitly discard
  that checkpoint, preserving data until they choose.
- **Explicit recovery**: a ready checkpoint can resume only after a user tap
  and renewed Google authorization. The downloader independently rechecks
  exact metadata, Range semantics and full SHA-256 + MD5 before import.
  No old corrupted or unverified file is treated as a completed download.
- **UI**: local journal entries with status, song title, byte size and local
  timestamp; five recent items by default, optional view of all 30; readiness
  explanation; and distinct statuses for canceled authorization versus
  interrupted transfer.

## Safety and privacy
- Original Google Drive Music `drive.file` permission is unchanged.
  Cloud JSON backup remains on separate `drive.appdata`.
- App-owned audio bytes, cloud recordings, playlist/index formats, existing
  checkpoint JSON schema and OAuth account preferences are unchanged.
- The transfer log is on-device only; it is **not** synchronized to Google
  and is **not** used to infer a transfer succeeded after a crash.
- Loss of journal data does **not** trigger a retry, delete, download,
  upload, auto-resume or music-library migration.
- Titles are locally sanitized to exclude URLs and common token patterns;
  a user who wants no song-title history can clear the journal at any time.
- Android instrumentation tests use unique disposable app-cache history files.
  They must **not** be run against the user's personal music installation.

## Added regression coverage
`LockerTransferHistoryPolicyTest`: 8 JVM cases for cap, deduplication,
crash-unknown status, completion fencing, title sanitation and malformed rows.

`LockerCheckpointReadinessTest`: 11 JVM cases covering unreadable/missing
checkpoint, account mismatch, absent/short/unsafe files, zero-offset leftovers,
uncommitted bytes and pending final hash checks.

`LockerTransferHistoryStoreInstrumentedTest`: 5 Android test cases for
reopen persistence, unconfirmed state, prohibited journal field patterns,
corrupt-file preservation and history-clear isolation.

The new source gates `tools/verify_v2_phase6v5b.py` and
`tools/check-phase6v5b-integration.ps1` are registered in CI and the local
Windows build. **A source preflight is not an executed test suite.**

## Milestone validation (future integrated build)
1. Extract a full ZIP from this feature branch and run `build-windows.cmd`.
   The existing build requires Gradle JVM tests, `assembleDebug` and
   `compileDebugAndroidTestKotlin`, then APK identity/signature verification.
2. On a disposable emulator, begin a permitted test download, interrupt
   after a recorded checkpoint, reopen Vault: show pending bytes, explicit
   Resume action and unconfirmed journal status. Resume and verify its
   final hash plus offline import.
3. On an emulator test fixture, remove/truncate staged data; make sure Resume
   is blocked and an explicit discard message appears. Never damage real
   personal audio files.
4. Test same-account resumption; switching to a different account must block
   resumption. A corrupted checkpoint should never be overwritten on refresh.
5. Test completed/reused upload and download journal rows after app restart.
   Clear history and verify checkpoint/local songs remain intact.
6. Check cancellation/failed authorization never creates a fake completed
   entry. Confirm existing Drive vault search, filters, uploads/downloads and
   prior queue/offline playback still work.
7. Check `check-device-update.cmd` before any in-place phone APK update.

## Known limitations
- No upload-session persistence or automatic background transfer retry.
- Journal status is best effort; it is an audit trail, not an independent
  verification receipt. An interrupted upload can finish server-side while
  the local row remains unconfirmed. Refresh Drive before retrying.
- Device-local timestamps can be affected by an incorrect device clock.
- Checkpoint preview verifies metadata and safe path conditions only;
  full audio hash verification remains part of the explicit download/import.
- Full Kotlin build, JVM execution, Android instrumentation, live Drive OAuth
  and phone installation have not been performed for this development branch.
