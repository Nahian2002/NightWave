# NW66 — Verified Offline Completion and Interrupted-Transfer Storage Safety

**Parent:** `feature/nightwave-nw65-download-recovery-safety`  
**Branch:** `feature/nightwave-nw66-offline-integrity-recovery`  
**State:** Source integrated; complete Android Gradle compilation, Android JUnit/instrumentation and Samsung phone testing remain **UNVERIFIED**. Public release remains **NO-GO**.

## Findings in NW65

All three download types already created random private `.pending` staging files, validated the downloaded media, imported with a second verified private copy and cleaned up their own staging file after failure. For legitimate catalog downloads, the catalog worker also reserved initial storage space. However, **only searched-song downloads** checked storage both during transfer and before making the second copy. Unknown-length direct HTTPS transfers could consume disk without protecting copy headroom. The WorkManager workers also treated a completed import call as sufficient to declare SUCCESS without a final independent check of the actual indexed/readable offline file.

## NW66 improvements

- **All download routes** (catalog, direct HTTPS, searched audio) now apply `OfflineSaveStoragePolicy.canStart()` immediately after the server responds and before streaming starts. A server-declared size must leave space for both staged and indexed copies; an unknown length gets a safe minimum free-space floor.
- Every route now checks `OfflineSaveStoragePolicy.canContinue()` at ~4 MiB staging checkpoints and **after the final bytes** — including very short unknown-length transfers. It reserves room for the final second copy and a 24 MiB free-space margin. A fail-closed storage check may reject near-full devices; it does not delete existing offline songs.
- `OfflineTransferCompletionPolicy` requires that the worker has not stopped and the saved private-library ID appears in an **independently re-read** `OfflineAudioLibrary.availableIds()` snapshot. The existing library verifies index membership, safe file path, size, type and readability; the downloader's existing audio-format inspection and SHA-256 copy verification remain in effect.
- Catalog, direct and searched WorkManager workers now all apply the same post-import success rule. An inconsistent or missing private file returns an honest error suggesting Offline Library inspection rather than a false SUCCESS. It does **not** automatically delete or re-download audio.
- `DownloadRecoveryPolicy` now distinguishes a missing post-import verification receipt and insufficient private storage, with fixed, sanitized, user-actionable text. Raw exception details are still hidden.
- Existing WorkManager queued-job identities, backoff policies, retries, unique-work KEEP behavior, 250 MiB cap, user-consented source safety/DRM restrictions, Google scopes, UI/queue/history keys, personal audio/lyrics/playlists and app package/signing identity remain **unchanged**. Interrupted transfers still start from zero; this phase does **not** implement HTTP byte-range resume.

## Test coverage and limitations

- Compiled the exact committed `OfflineTransferCompletionPolicy.kt` and `DownloadRecoveryPolicy.kt` with local `kotlinc` and ran **109/109 standalone JVM smoke assertions**. Confirmed Git blob SHA-1 parity: completion `6518fed83fabfe6a5ca39fa4cff2355b7bf42ca0`, recovery `cc253779c590f3c870d36df3f489a716bd0db6a4`. The smoke exercises completion gates across stopped/available/missing combinations, lifecycle pause statuses and redacted/idempotent error categories. A failed intermediate smoke uncovered an already-sanitized recovery message losing its category; fixed and retested to 109/109 PASS.
- Added `OfflineTransferCompletionPolicyTest.kt` covering current valid ID, stopped job, missing ID, wrong indexed ID and ambiguous/unavailable verification status.
- Extended `DownloadRecoveryPolicyTest.kt` with safe post-import recovery, private-storage messages and idempotent worker/UI classification. These JUnit suites are **not yet run under Android Gradle**.
- Extended the Windows cumulative PowerShell preflight to assert that **all three workers** use the final file check; the downloader applies both free-space checkpoints without search-only gating; and the new policy/tests exist.
- **Source presence checks and GitHub readback** do not prove Android compilation, network behavior, file-system race safety, MediaExtractor behavior or full data durability. Actual Android Gradle/JUnit and physical-device QA remain mandatory.
- No automatic cleanup of other pending files was added. A stale file may belong to an active worker; deleting it without knowing ownership could corrupt a concurrent transfer. Each existing download continues to clean up only its own temporary file via `finally`.

## Later Samsung acceptance after a successful build

1. On a disposable device/test profile, try a properly licensed direct HTTPS audio file with low but sufficient storage, then one with deliberately insufficient free space. Verify the latter fails with storage guidance **without affecting existing offline audio**.
2. Repeat with an authorized catalog track and searched audio-only source. Confirm the same storage protections apply and that download limits are enforced.
3. Simulate a short unknown-length permitted stream and an interrupted response. Neither may appear as Ready unless the indexed complete private file exists.
4. Cancel a transfer mid-stream, verify its WorkManager terminal status and check that completed older songs still play.
5. Complete a permitted download, then check that the output ID resolves to a readable indexed private file; verify deleted/corrupted disposable files do not remain Ready after NW62 async verification.
6. Confirm NW63 history pages, NW64 coalesced refresh, NW65 pause/resume/error states and source eligibility are unchanged.
7. Confirm package/signer with `check-device-update.cmd` before any installation. Never uninstall NightWave or clear saved music to work around build, signer or OAuth errors.

**Outstanding blockers:** full NW66 Android compilation, real-device transfer and interrupted-storage testing, Google OAuth status 8, universal catalog rights/source availability, privacy/Play policy and distribution authorization. **Public APK / Play: NO-GO.**
