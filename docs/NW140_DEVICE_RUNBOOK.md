# NW140 device measurement runbook — NOT_RUN

This kit prepares measurement; it does not speed up or change the player. All production app/src/main files are unchanged from NW139. The Android diagnostic has not run. Its test APK is a separate test package, not the music app, and does not run by opening the player.

## Required owner input

Select an approved physical device and an isolated Android test user/profile, confirm the installed app/test APK identity and authorize the specific measurement slot. Keep the listener's original installation/data separate. No automation in this kit installs, uninstalls, resets, launches, force-stops or instruments a device. The owner controls any manual installation. No Google account or network/media playback is needed for the two synthetic diagnostics.

## What the supplied diagnostic measures

Run only class `com.nightwave.player.v2.ui.NW140PerformanceDiagnostic`, with instrumentation argument `nw140Measure=true`, on the selected test profile. The default correctness run skips these timing diagnostics. Do not run an unscoped full instrumentation suite as a substitute.

- ART runner: actual production library snapshot/index and query runner over 10,000 isolated synthetic references, four warmups and twenty samples for each of two language cases. Wall-clock timing runs outside Compose's test clock and includes the production 120ms debounce and worker dispatch. Index construction is outside the timed interval; this is not whole-screen latency.
- Compose fixture: actual Search screen/controller/index, clear/type/wait/scroll to the correct last-corpus row. Auto-advancing Compose clock, test-driver synchronization, scrolling and debug overhead make it a diagnostic rather than a real keystroke-to-display performance budget. Do not claim that this variant proves real debounce timing.
- Frame data: bounded copies of platform metrics for the whole fixture, including setup/warmups. Deadline availability is API-dependent. First-draw exclusions, callback drops, unknown values and truncation remain explicit. Insufficient or incomplete data produces an unknown rate, not zero jank. This is not the production app-wide frame budget.

The test uses unique preference namespaces; cleanup targets only those exact fixture names. Numeric receipts are written under the test target's `cache/nw140-diagnostics/` folder and announced in instrumentation output. They contain synthetic case IDs, raw timings and artifact/configuration hashes, not listener queries or account tokens. Copy only the announced receipt to a private workstation folder. No receipt exists before a successful approved run.

## Analyze privately

Use tools/nw140_measurements.py with --analyze RECEIPT --identity identity.json --output NEW_PRIVATE_RESULT. Output must be a new file outside the repository/kit directory. The prepared identity binds the actual app APK and test APK. For a matched comparison use --compare BEFORE AFTER --before-identity BASELINE_IDENTITY --identity CANDIDATE_IDENTITY --output NEW_PRIVATE_RESULT. Keep the identical harness and environment, and retain every sample; no best-run selection.

Analysis checks structure, hashes and the declared sample protocol. JSON cannot by itself authenticate that a device actually ran. Nearest-rank p50/p95, raw samples and descriptive deltas never imply release acceptance, a speedup, a 9.5 rating or a measured Spotify comparison.

## App-wide acceptance is separate

Follow docs/quality/NW140_DEVICE_PROTOCOL.json and the original roadmap for controlled cold TTID versus fully interactive TTFD, warm resume, actual 10k input-to-display, 100 local Next-to-independently captured audible start, Perfetto/allocations/retained memory, matched-power battery and a 60-minute integration soak. The final two-hour mixed reference soaks also remain. Baseline profiles/caches/IO changes need a reproducible trace and matched evidence first.

Reference guidance: [Android app benchmarking](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview), [startup states and display timing](https://developer.android.com/topic/performance/issues/launch-time), [Perfetto](https://developer.android.com/tools/perfetto), [FrameMetrics](https://developer.android.com/reference/android/view/FrameMetrics). External controlled Macrobenchmark and system traces have a different scope from this isolated debug diagnostic. Do not infer phone measurements from the retained NW122/NW129 host JVM benchmarks.

All runtime, physical and performance acceptance remains NOT_RUN until actual approved evidence is collected. Source preparation/compilation is the only completed work in this kit.
