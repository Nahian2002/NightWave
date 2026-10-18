# NightWave 6V5 — Google Drive Music Vault browser and transfer visibility

## Foundation and safety
This phase extends **6V4D** and retains all 6V4A–6V4C reliability improvements.
NightWave remains **without its own backend server**. The audio vault continues
to use the user's explicit Google authorization with the existing
`drive.file` scope. Cloud JSON backup remains isolated in `drive.appdata`.
No new OAuth scopes, client ID, paid API, cloud media deletion, credential
storage, autonomous uploads, automatic MP3 extraction or mass downloading.

## Changes
- `LockerVaultBrowsePolicy.kt` is a pure, read-only presentation layer:
  case-insensitive local search, verified/needs-attention filters, stable
  name/size sorting, bounded 40-at-a-time cards, listed-byte summaries and
  complete-vs-partial disclosure.
- The Drive client lists **up to five pages of 100** metadata records per
  explicit Refresh, verifies every accepted song's ID/type/name/byte metadata,
  detects invalid/repeated page tokens, deduplicates file IDs and returns
  `LockerCloudInventory(tracks, hasMore, pagesRead)`.
- A listing with `hasMore=true` explicitly warns that further cloud files
  may exist. "Listed NightWave music" is **not** total Google Drive storage
  or a guarantee that every file in the folder is represented.
- A truncated inventory cannot be used to assert that a cloud upload does
  not already exist. New uploads fail closed instead of creating potential
  duplicates from an incomplete scan. This threshold is a known limitation
  until a later cursor-based/deduplicated remote-query phase.
- The vault's existing verified download flow still independently rechecks
  Drive parent membership, matching remote metadata, and SHA-256 + MD5 before
  importing app-owned offline audio. A file missing upload checksums is shown
  as "Needs attention" and remains blocked for download.
- Upload/download progress is displayed as a single **in-progress transfer**.
  The UI never calls a displayed 100% callback a saved local file before
  verified import finishes; final status is shown only on completed callbacks.
  This release does **not** implement concurrent jobs, background service
  transfer queues or persistent upload sessions.
- Google Drive data remains unchanged by sorting/filtering/show more; these
  operate strictly on fetched metadata and generate no extra search requests.
- Old source contracts for 6R1 and 6R2 are updated to require the new
  inventory interface rather than their previously hardcoded 2-page code
  string. The old upload/download integrity requirements remain checked.

## Added regression tests
`LockerVaultBrowsePolicyTest.kt` covers:
- Case-insensitive search and immutable inventory data.
- Both-hash verified download status and attention filtering.
- Stable name and numeric size sorting.
- Honest listed-byte totals and partial-results reporting.
- Bounded "Show more" across a large mocked inventory.
- Byte units, empty result handling, and complete-list marker.

## Release verification
The new Python source-gate and PowerShell preflight are *source checks*,
not Android builds. They are self-checking/registered in CI alongside the
real Gradle tasks. Only a **successful Gradle unit-test/build/Android-test
source compile**, successful APK signature comparison and a controlled
real-device smoke test can certify release readiness.

### Build on Windows
1. Download a **full ZIP for the 6V5 feature branch** (do not mix separate ZIPs).
2. Extract into a fresh project directory and double-click `build-windows.cmd`.
3. The builder must pass 6V4 and 6V5 integration gates, all JVM unit tests,
   `assembleDebug`, and `compileDebugAndroidTestKotlin`.
4. If `BUILD SUCCESS`, the new APK is `dist/NightWave-debug.apk`.
5. Use `check-device-update.cmd` *before* updating the existing app; do not
   uninstall or clear storage to work around signing mismatches.
6. On failure, run `send-build-report.cmd` and send only the redacted
   `dist/NightWave-build-diagnostic.txt`.

### Device acceptance (after verified build)
- Connect an authorized Google account and Refresh. Check the listed-only
  count, verified count, attention count, and listed storage estimate.
- Search part of a song name with uppercase/lowercase, clear search, filter,
  sort by size, and use Show More beyond 40 results.
- Verify a non-checksummed/legacy entry is visible but cannot be downloaded.
- Upload a **small, personally owned MP3**; ensure progress is shown and the
  final verified-completion message appears. Tap Refresh to find it.
- Download the verified test MP3, play it offline in airplane mode, and
  confirm original local music and lyrics are unchanged.
- Interrupt a test download, return to the screen, resume only with explicit
  confirmation; discard must affect only the temporary local file.
- Check an expired permission flow and failed Drive request leave existing
  songs and previous cloud data untouched.
- Validate previous-song restoration (6V4A), local unavailable warnings
  (6V4B), and pause/retry handling (6V4C) remain functional.

## Known limitations
- Scans are bounded to 500 folder records, and the file count/size figures
  are for the returned accepted audio records only; a Drive folder may hold
  more, including unsupported or hidden files.
- The underlying Drive service and OAuth are external; only NightWave itself
  needs no hosted server.
- This phase adds **browser and progress visibility**, not full Drive quota
  fetching, real-time lyric transcription, stream-to-MP3 conversion, or an
  always-on background download manager.
- Gradle compilation, runtime UI rendering, live Drive behavior and APK
  update compatibility have not been verified in this environment.
