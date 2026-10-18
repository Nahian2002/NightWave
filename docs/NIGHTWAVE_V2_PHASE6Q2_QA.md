# NightWave 6Q2 — Cloud Backup History & Recovery Details

## Scope
Second bounded phase of Google Drive Backup 3.0. Stacked draft on 6Q1. Stable main, previous cloud JSON format, and installed music remain untouched.

## Improvements
- The app-only Drive snapshot listing collects at most **three 100-file pages** (up to 300 results), validates each expected NightWave v2 filename, deduplicates IDs, bounds page-token growth, and shows the **newest 50 selectable backups** rather than 20.
- Drive file size is an optional descriptive metadata field shown next to the saved-date label. Listing metadata is **not** proof of integrity; selected backups are downloaded, checksum-verified through CloudBackupCodec, and decoded before any confirmation.
- For **Restore**, the chosen cloud library is compared against a fresh copy of the phone's library **off the UI thread**. The confirmation dialog discloses cloud and local favorites/playlist counts, estimated local favorites/playlist removals, changed playlists and profile differences, plus optional history count.
- The dialog explicitly states that local offline audio and imported LRC lyrics are not carried by this metadata backup, and offers separate choices for library-only and library-plus-history restoration when opted-in history exists.
- When the user taps Restore, CloudRecoveryReport.expectedLocalBackup is checked inside the existing exclusive legacy library transaction, together with the existing protected restore's write. If favorites, playlists or profile changed after the preview, the operation returns STALE_PREVIEW without modifying any library data. The user must review again.
- Background application of confirmed restore is on Dispatchers.IO; the UI reports applied, stale or failed states distinctly. Account choice is rechecked before starting restore. Merge retains its previous conflict-aware behavior and no existing cloud file is deleted.
- The narrow Google OAuth drive.appdata scope and v2 envelope containing a v1 library archive are unchanged. No automatic cloud restore, broad Google Drive access, authorized audio file sync or lyrics backup is claimed.

## Verification
- 15 pure JVM CloudRecoveryPolicyTest methods authored, covering local data losses, name/case/order conflicts, profile changes, optional history, invalid/cloud-corrupt data and no mutation.
- A dedicated 6Q2 source preservation gate is added to the stacked Android workflow; older 5U gate updated for the richer explicit confirmed-restore callback.
- Source assertions are not a compiled Android app. CI runner scheduling, real Gradle/JUnit compilation, actual Drive OAuth/list/pagination and small-screen dialogs remain UNVERIFIED. Keep this phase draft until a real build and controlled restore test occur.

## Future work
6Q3 may extend the archive with *optional* backed-up imported lyrics and preferences using a new validated version, explicitly excluding private audio, requiring user opt-in and maintaining v2 restore compatibility. 6R is a separate personal cloud audio locker; the hidden appDataFolder cannot substitute for personal MP3 storage. Neither is part of this update.
