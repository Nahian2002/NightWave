# NW51 — Performance & Reliability

**Parent:** `feature/nightwave-nw50-premium-motion`  
**Branch:** `feature/nightwave-nw51-performance-reliability`  
**Status:** SOURCE IMPLEMENTED. Source-contract inspection passed. Full Android build, JVM execution, instrumentation, runtime profiling and phone QA **remain unverified**.

## Changes and why they matter

### 1. Avoid blocking the UI with offline library reads
- Prior `NightWaveStudioActivity.refresh()` was called on every track-identity update. It synchronously loaded favorites, recent, playlists and folders, invalidated the optional Room preview, and checked an offline resume file using `OfflineAudioLibrary.fileFor()` on the UI thread.
- NW51 leaves full `refresh()` for explicit lifecycle/library refreshes, replacing the per-track call with a **targeted, asynchronous recent-history refresh**. The Room preview is no longer reset merely because a song changes.
- Offline resume's index membership and private-file metadata checks now run under `Dispatchers.IO`. **Clarification:** `fileFor()` in this version validates index membership, regular file metadata and location; it does NOT hash an entire audio file. Avoiding synchronous disk access still matters for responsive navigation.
- Offline Search now loads the bounded, at-most-400-song local index via Compose `produceState` + `withContext(Dispatchers.IO)`, rather than parsing it in a synchronous `remember(...)` block during Search composition.
- Only current Activity generations may publish results. Changing songs, refreshing, backgrounding the Activity or destroying it cancels pending UI-only reads. Failed/malformed offline index parsing remains non-destructive and falls back to an empty UI list; no deletion or automatic repair occurs.
- An async Search index can briefly display an empty set until loaded; a real UI test is still needed to confirm the loading experience remains acceptable on a slower phone.

### 2. Bound optional listening-statistics writes
- Replaced an unbounded `Executors.newSingleThreadExecutor()` backlog with a single `ThreadPoolExecutor` worker and **32 pending writes maximum**. Saturation cannot accumulate unlimited queued optional listening pulses in RAM.
- `RejectedExecutionException` is caught on the Media3 callback path; when storage falls behind, an optional statistics sample is skipped instead of blocking or crashing audio playback.
- Warning logs are rate-limited to approximately once per minute, avoiding noisy logs and needless work in prolonged storage stalls.
- Playback volume, service ownership, consent checks and the memory store's existing revision fence are unchanged. Listening insights are optional; under pathological storage backlog the count may under-report rather than jeopardize music.

### 3. Coalesce fast queue operations
- The UI bridge previously posted two anonymous delayed refresh callbacks for **each** Play Next/Add or full-queue change. Repeated fast taps could enqueue many redundant UI refreshes on the main thread.
- Four reusable, removable queue-refresh runnables now provide the same early/late service reconciliation timing, coalesced to at most one of each type.
- UI queue refresh work stops when the screen leaves the foreground, and `onStart`/controller reattachment already re-establishes the latest persisted queue. No queue items or service playback logic are discarded, reordered or automatically added by this change.

## Tests and evidence
- Two pure unit cases in `LibraryUiRefreshPolicyTest` cover stale generation and destroyed/finishing Activity denial.
- Two pure unit cases in `ListeningDispatchPolicyTest` cover the bounded queue size and monotonic-time warning rate limit.
- `tools/verify_nw51_reliability.py --self-test` guards async IO placement, service queue backpressure, receipt checks, no per-tap callback pileups, existing Media3 source safety and essential UI callbacks. Five mutation fixtures must fail closed.
- Android CI workflow contains the NW51 verification step plus existing Gradle compilation and unit-test tasks.
- **Source-contract audit: PASS** in the connected GitHub branch. No duplicate imports found in the touched Kotlin sources.
- **Not established:** a successful Android build, that the Python source gate actually ran on CI, new JUnit test execution, measured reduction in frame stutter or memory growth, battery improvement or elimination of all device crashes.
- Earlier GitHub Actions failures had no executed job steps. That suggests build infrastructure must be investigated separately; it does not prove the patched sources compile or fail.

## Device acceptance after NW52
1. On the final stacked branch, run an actual Android Gradle debug build and all local JVM tests, plus Android instrumentation test-source compilation. Never wipe user data just to update.
2. Rapidly skip 20–30 tracks; monitor frame responsiveness and no wrong recent track labels. Confirm Play Next still precedes automatic suggestions and queue Undo works.
3. Open Search with 0/50/400 saved entries; local results must appear after async load without a crash or permanent false `No results`. Search should remain functional when the index is malformed (test on disposable data only).
4. Rotate and background/foreground during an offline resume check; no stale title/position may replace the current session. Offline playback from an eligible real private file must remain intact.
5. Tap Add to Queue or Play Next repeatedly in quick succession; verify each permitted intent reaches the **service**, with no duplicate UI queue items and no dozens of redundant UI reconciliation callbacks.
6. Simulate slow listening-insights storage on disposable data. Main music playback must continue; optional statistics may skip pulses under queue saturation. Consent revocation must still prevent late recording.
7. Run Android Studio memory/CPU profiler and collect device crash logs, including Bluetooth disconnect, screen lock, network loss, download retry and rotation. Compare metrics on the same device/network/build.

**Next:** NW52 — public-release hardening, integration checks and publication readiness. NW52 source completion alone will not certify a release.
