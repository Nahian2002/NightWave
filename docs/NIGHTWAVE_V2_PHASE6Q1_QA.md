# NightWave Phase 6Q1 — Smart Google Drive Snapshots

Stacked on 6P, stable main unchanged. This is the first bounded Google Drive Backup 3.0 increment; it does not create a cloud audio locker.

## Implemented
- Manual Back up now and periodic 24-hour WorkManager backups use the same \`NightWaveDriveClient.backUpIfChangedVerified\` operation.
- The client creates the intended bounded checksummed envelope, lists the latest appDataFolder snapshot and downloads it for comparison. \`CloudSnapshotPolicy\` validates both local and remote envelopes using the existing codec, then compares decoded data (profile/favorites/playlists/optional listening history). Unchanged contents reuse that *verified* Drive object; no new upload is necessary.
- When data changes, latest remote download fails, or the remote snapshot is corrupt, the client creates a new copy-on-write snapshot and reads the exact bytes back for verification through the existing \`uploadVerified\` path.
- A failed Drive list or new upload does not count as a successful backup. There is **no on-device-only hash shortcut** that could falsely claim remote backup success after a snapshot was deleted.
- Background uploads recheck that the account, enablement, listening-history opt-in and Wi-Fi-only preference have not changed after the worker built the payload. If changed, this stale job exits without uploading. The next scheduled run can use the latest settings.
- Manual backup also rechecks the account and listening-history opt-in immediately before starting its Drive operation; if the user changed the privacy choice while the backup was being prepared, it fails without uploading that stale payload.
- Manual backup shows **Cloud backup already current; nothing uploaded** for unchanged data, or the original verified-upload confirmation if a snapshot was created. Background status similarly distinguishes unchanged backups.
- Existing narrow \`drive.appdata\` Google scope, local privacy settings, 3 MB backup cap, checksum format, version 2 content compatibility, snapshot picker, confirmed restore, merge, Recovery snapshot and media restrictions remain.

## Verification status
14 new pure JVM test cases *authored* for exact/equivalent backups, changes to profile/favorites/playlists/history, privacy opt-out, corrupt/empty/invalid snapshots, and no source mutation. 6Q1 Python source regression check added after previous gates to Android CI. This is source-only verification; Kotlin compilation, test execution, Google OAuth and remote Drive round trips remain unverified unless a real runner/local Windows build is completed.

## Future phases, not included
- 6Q2: richer snapshot history, backup size and user-visible recovery details, optional lyrics/settings coverage with explicit versioned migration.
- 6R: **separate** Google Drive user-visible file storage and authorized audio uploads/imports, using a different Drive scope and resumable transfers. The existing hidden appDataFolder is only for app settings/library metadata, not MP3s.
- 6S: synchronized lyrics provider integration and local caching; offline LRC already exists.

No remote deletes, destructive pruning, automatic restore, APK installation, permissions expansion or streaming-media download capability is introduced.
