# NW65 — Download recovery and honest pause states

**Parent:** `feature/nightwave-nw64-download-refresh-performance`  
**Branch:** `feature/nightwave-nw65-download-recovery-safety`  
**Status:** Source implemented; exact standalone Kotlin policy compiled and **72/72 smoke assertions passed**, with Git blob SHA-1 parity. **Full Android Gradle/JUnit/device QA has not run. Public release: NO-GO.**

## Problems observed in NW64 source

1. FAILED cards appended raw `WorkInfo.outputData` exceptions directly into a visible `TextView`. Catalog/direct worker strings can contain internal details, HTTP endpoints, signed URLs, file paths or other information that should not be displayed.
2. A persisted pause marker was treated as `pausedByUser` even if the WorkManager job subsequently became FAILED or SUCCEEDED. This could give a misleading resume action on a non-paused job.
3. The UI rendered `Save this song again` as soon as `WorkManager.State.SUCCEEDED` appeared, including while NW62's background file verification was pending or inconclusive. An existing saved recording could be offered for an unnecessary duplicate save.
4. Pause confirmation relied on the **old card's** WorkInfo state. The transfer could finish while the confirmation dialog remained open.
5. WorkManager's `ExistingWorkPolicy.KEEP` can retain an existing in-progress unique job. `Operation.result` SUCCESS alone **does not prove a new transfer started**, so the old `Save queued` message could mislead.

## Changes

- A pure Kotlin, presentation-only `DownloadRecoveryPolicy` class maps known download failures to short actionable categories: insufficient storage, access denied/permission check, rate limits, unsupported media, unavailable source, interrupted connection and search-index association problems. **No raw worker exception, signed media URL, path or token is interpolated into these status messages.** Unknown failures show a generic redacted explanation.
- Distinguishes **Pausing** (pending WorkManager cancellation), **Paused** (WorkManager CANCELLED *and* user pause marker retained), ordinary Cancelled, and FAILED/SUCCEEDED. Failed/succeeded jobs cannot be labelled paused even if an obsolete preference marker exists.
- The Pause dialog re-checks the latest known WorkManager state after the user confirms. If a job has already finished or changed, no pause marker/cancellation is sent.
- An explicit Cancel checks the latest known state and clears an existing pause marker first; if the marker cannot be cleared safely, cancellation is not sent and the screen reports that nothing changed.
- `Resume (restarts from zero)` is offered only when the old attempt is confirmed CANCELLED with a pause marker. Its confirmation checks this state again; all other finished searched-song attempts offer a normal Retry.
- A completed searched song only offers `Save this song again` **after** successful background file verification proves the local file is missing. When verification is pending/failed, the UI stays inconclusive rather than red/missing.
- After WorkManager accepts a unique-work request, the UI now reports that the request was accepted **and an existing active job may be reused**. No guarantee of a new transfer is made.
- Existing audio files, library IDs, playlists, transfer WorkManager keys, queue `KEEP` policy, Google Drive scopes, playback code, package identity, target API and media-source safety checks are **unchanged**.

## Verification actually executed

- GitHub source readback matched the exact local Kotlin blob `904f103ab02f39a3c2c1cf553dbb58df2acb62f7`.
- Exact production `DownloadRecoveryPolicy.kt` compiled with `kotlinc` and executed **72/72** standalone JVM assertions: all active/terminal pause-state combinations, eligibility, retry wording, redacted URL/path, storage, rate limit, HTTP denial, invalid file, permission, missing item, connection and post-save index-link diagnostics.
- Added JUnit tests in `DownloadRecoveryPolicyTest.kt`. These are committed but **not yet executed with Gradle**.
- Cumulative Windows preflight requires actual NW65 message/transition wiring and retains NW62–NW64 invariants. The Windows PowerShell gate is **not yet executed** in this environment.
- **Not verified:** final Android compilation, WorkManager cancellation race on a phone, local file-index consistency, real YouTube/source download eligibility, transfer throughput, Google error 8, signing continuity, UI accessibility and public distribution compliance.

## Deferred Samsung acceptance checklist (after full Windows build)

1. Start a permitted download; tap Pause, then confirm. Expect **Pausing** while WorkManager is still active, then **Paused** only after cancellation. Resume must **restart from zero**, not continue bytes.
2. Tap Cancel from an active download; it must not retain a misleading Paused state. Repeat if a pause request is in flight.
3. Open a Pause confirmation and let a small transfer finish before confirming. An already-completed file must not be labelled Paused or cancelled by a stale dialog.
4. Simulate denied-source, offline-network, rate-limit, missing-source, invalid-audio and storage-full cases with disposable permitted files. The UI must show safe, intelligible categories without exposing URLs or file paths.
5. While saved-file verification is pending or inconclusive, a SUCCEEDED job must **not** show a red missing-file warning or a `Save this song again` button. Recheck after a real disposable file is deleted.
6. Trigger retry twice quickly and confirm unique-work semantics: an existing active request may be kept. Check the actual history, rather than trusting the request-accepted message as proof of a new job.
7. Verify NW62 background health, NW63 Older/Newer paging and NW64 400 ms UI refresh are intact. Confirm existing offline music can still be played.
8. Keep the installed APK and app data untouched until `build-windows.cmd` succeeds and `check-device-update.cmd` confirms the signer. Never uninstall/clear NightWave to solve an OAuth or signing mismatch.

## Not included

True byte-range pause/resume, unsupported or restricted recordings, speed improvements to external hosts, a guaranteed Google OAuth fix, expanded cloud features, paid DRM bypass or public release authorization are not claimed.
