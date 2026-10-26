# NW87 — Transfer Center & Offline Library UX

**Authoritative roadmap:** [NW76–NW90](CURRENT_CUMULATIVE_ROADMAP.md) · Wave 3 / NW87  
**Cumulative parent:** `feature/nightwave-nw86-library-playlists-you`  
**Branch:** `feature/nightwave-nw87-transfer-offline-ux`

## Shipped source changes

1. **Live transfer counts without view churn.** Transfer Center's existing All / Active / Ready / Issues tabs now show bounded (99+) counts, refreshing their labels in place rather than rebuilding navigation for each WorkManager progress event. Active counts follow actual WorkManager state, while Ready and Issues display `…` whenever local saved-file verification is pending or unavailable. A successful WorkManager job is **never automatically** considered playable: `DownloadFileVerificationPolicy.ready()` still requires an available private audio file.
2. **Honest progress overview.** The header separately shows active, verified ready, attention, and total visible transfer-history entries. The transfer history remains paged in 60-row chunks, source-URL retries remain deduplicated by existing rules, and hidden history entries do **not** delete downloaded files. A small help line explicitly explains that search Pause is cancellation-based and **Resume restarts the transfer from zero**, not from a saved partial file.
3. **One-tap offline recheck.** Offline Library now exposes **Recheck saved files**. It runs `OfflineAudioLibrary.healthReport()` via the existing background worker and publishes the finished result on the UI thread only while the Activity remains alive. Nothing is automatically deleted, rewritten or repaired. If the index remains unreadable, its safety warning remains prominent; failed checks do not trigger another synchronous scan.
4. **Visible recovery guidance.** When any indexed file needs attention, Offline Library shows a clear issues banner that navigates to the Issues filter. Per-song options use the exact health classification—missing, size-changed, unreadable or unsafe index metadata—to explain the safest recovery options. `⋮` is labelled **Recovery options** rather than misleadingly labelled Remove on unplayable audio.
5. **User-controlled trusted re-import.** The issue dialog can launch the existing system document picker for user-owned/licensed audio. A re-import produces a **separate verified entry** and does not silently replace or relink the old indexed entry. Ready offline audio still retains its original, explicitly confirmed deletion action.

## Scope held unchanged

- WorkManager workers, permission gates, source restrictions, background network transfers, byte tracking and true pause limitations.
- Private audio storage/index and playback queues, authorization/licensing rules, valid file checks, and file deletion logic.
- NightWave Google account/Drive data and backup privacy settings.
- Existing remote source catalogs, download eligibility, Android manifest and signing.
- No paid API, server, new unrestricted YouTube extraction, auto-repair, forced playback of corrupt audio or unlimited cache writes.

## Regression source and tests

- New `DownloadCenterDashboardPolicy.kt` and `DownloadCenterDashboardPolicyTest.kt` cover WorkManager state counts, verified/indeterminate Ready and Issues, 99+ display caps, and honest pause/history descriptions.
- New `OfflineLibraryRecoveryGuide.kt` and `OfflineLibraryRecoveryGuideTest.kt` cover five file-health states, detailed no-delete guidance, one/multiple issue wording and the explicit recheck action.
- `tools/NW87TransferSmoke.kt`: pure Kotlin test runner for the three real presentation policies, using a compile-only file-health enum stub in `tools`. Run against the exact branch source with the `kotlinc` command in the file header.
- `tools/verify_nw87_transfer_offline.py`: read-only source-contract checker for UI integrations, verified-file gating, worker-based recheck and trusted-import action.

**Evidence:** Source files and JUnit/Kotlin test sources are committed to GitHub. Source readback/structural checks are not proof that tests ran. **A complete clean Gradle build, current JUnit suite, Android instrumented tests, live WorkManager jobs and device validation have not run during this phase.** These remain part of the NW90 full quality gate.

## NW90 acceptance scenarios

Check 0/1/1000 WorkManager records, active/queued/retrying/completed/failed/cancelled/pause-requested states, stale rates, downloaded bytes versus verified playable audio, large files, app restarts, Wi-Fi transitions and low storage. Confirm history Hide never deletes local files. Verify recheck during long imports and on Activity pause/rotation, missing/deleted files, unreadable index, changed file size, damaged private metadata, and approved re-import with separate resulting song identity. Use airplane mode to validate only verified owned/licensed audio plays offline.

**Public distribution NO-GO:** NW77's content-source/rights concerns and NW90's full device certification remain unresolved.
