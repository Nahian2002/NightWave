# NightWave 6V5C — Scalable Music Vault and folder-level storage reporting

## Scope
Builds on 6V5B transfer-history/checkpoint recovery and previous 6V4 reliability.
Still no NightWave backend server. Google Drive is the user's external service;
the existing `drive.file` scope remains unchanged. No remote deletion, library
rewrite, automatic download, subscription or MP3 conversion is introduced.

## Improvements
1. **Explicit cursor paging beyond 500 songs.** The first Refresh still reads
   at most five pages of 100 file records. A visible `Load next Drive batch`
   control fetches at most five more pages per user action, preserving already
   scanned results, search/filter controls and show-more position. The total
   per-screen safety cap is 40 Drive pages / 4,000 records. A folder that
   exceeds this cap remains honestly labeled partial, not complete.
2. **In-memory-only continuation.** Cursor tokens and observed-token history
   stay in the current Activity state; they are never saved to disk or logs.
   A repeated, missing, oversized or stale cursor, a folder change, or an
   excessive request stops with a user-facing Refresh recommendation. A new
   app session begins with a fresh folder scan.
3. **Selected download verification no longer relies on the first 500.**
   On download/resume, the Drive client fetches the chosen file's *current*
   metadata by its authorized ID and refuses any change to name, MIME, size,
   SHA-256, MD5, original local ID, current NightWave Music folder membership
   or `canDownload`. The existing Range, checkpoint, SHA-256/MD5 and
   local-import checks are still mandatory.
4. **Safe large-vault uploads.** The previous global-scan limitation blocked
   new uploads once more than five pages existed. Upload now uses a targeted,
   folder-scoped Drive `appProperties` query against the local file's
   SHA-256. Each candidate must pass full audio metadata validation. Up to
   20 matching pages are permitted. If Drive refuses the query, returns a
   repeated/invalid token or still has more matching records at the limit,
   no new upload is started. This does **not** bypass verified Drive receipt
   checking, resumable upload reconciliation or local file verification.
5. **More useful music usage report.** Summary shows listed valid audio
   count, verified/unverified counts, byte totals for each group, up to four
   most common listed file types and the number of scanned Drive pages.
   These are **NightWave Music folder metadata estimates**, not the whole
   Google Drive account's quota or actual billing/usage metrics.

## Safeguards and compatibility
- Older source check scripts 6R1/6R2/6R3/6V5 now assert the new source
  contracts instead of the obsolete five-page-only snippets. Existing
  authentication and cloud receipt validations are left in place.
- Music, offline indexes, cloud backups, OAuth account storage, transfer
  checkpoints and transfer history formats are unchanged.
- All remote API operations in this phase are read-only Drive metadata
  queries until the user explicitly authorizes a manual upload/download.
- No Drive deletion, arbitrary folder search or token logging was added.
- Listing only retains validated song metadata, deduplicated by Drive ID.
- A failed `Load Next` does not replace the already scanned in-memory
  inventory; however it may be stale until refreshed.

## Added/updated tests
- `LockerVaultPagingPolicyTest`: opaque/repeated/bad cursor handling;
  folder-scoped SHA search/query injection rejection; exact checksum/size/
  MIME/MD5 matching; explicit continuation cap and state eligibility.
- `LockerVaultBrowsePolicyTest`: asserts the 4,000-row visibility bound
  and checked folder-level verified/unverified byte totals and file formats.
- `tools/verify_v2_phase6v5c.py --self-test` and
  `tools/check-phase6v5c-integration.ps1`: source integration gates.
  Source checks are *not* executed Kotlin or Google API tests.

## Build and emulator acceptance
1. Download a full 6V5C source ZIP to a fresh folder. Run
   `build-windows.cmd`; require all prior gates, 6V5C source preflight,
   `testDebugUnitTest`, `assembleDebug`,
   `compileDebugAndroidTestKotlin` and signer/identity verification.
2. Do not install over a working NightWave until
   `check-device-update.cmd` reports compatible package/signing.
3. On a disposable emulator and authorized test Drive folder, create or
   simulate more than 500 valid audio records. Refresh, Load Next, and
   verify no duplicate rows, correct cumulative totals, and song selection
   on subsequent pages. Search/filter/sort should work on all listed rows.
4. Attempt a previously uploaded test MP3 with a large folder; SHA-targeted
   deduplication must recognize the verified existing cloud file and never
   duplicate it. Force a query error/repeated token/excessive match result;
   ensure upload does not start.
5. Download a test song from beyond page five. Validate complete remote
   parent/metadata checks, byte checksum and final private offline import.
6. Simulate a stale/invalid cursor or changed folder. Ensure Load Next
   fails without modifying local files or previous in-memory results.
7. Regress transfer history, checkpoint resumption, cloud backups, queue
   recovery, offline file-health warnings, local playback and lyric files.
8. Never deliberately corrupt private audio on the personal phone.

## Known limitations
- Still a bounded 4,000-record **metadata-only** vault browse session;
  pagination is explicit and not persisted across app restarts.
- Google Drive `about.storageQuota` is not requested; app does not claim
  account-wide free/used space.
- A folder larger than the safe cap stays partial. A targeted search for
  one selected file, not exhaustive inventory, allows safe upload dedup.
- New path has not been proven by a live Google Drive API response, Android
  compiler, emulator instrumentation or APK-signing comparison in this
  assistant environment. Treat this branch as development source until the
  integrated Windows/CI and device checks are actually passed.
