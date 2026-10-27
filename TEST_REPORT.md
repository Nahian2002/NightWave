# Current verification — NW122 measured search/library performance

27 October 2026. Branch: `update/nightwave-nw122-measured-performance`. Parent: verified NW121 `10ea25315d9746ebbde463ef05067b50798c9614`.

## Executed locally

- JVM regression suite: **1,464 tests in 184 suites, zero failures/errors/skips**. Eleven new methods cover seeded occurrence-selection parity, prepared labels, stable public/library ordering, original object preservation, duplicate/alias precedence, edited/removed snapshots, shared suggestions and their 5,000-row boundary.
- Current structural/negative and Windows preflights: **50/50 PASS** (including the new benchmark-comparison self-test). Updated integration checks retain saved-score/URL guards and require both paths of the bounded suggestion reuse.
- An independent old grade-first oracle matches the new single-scan evidence across 1,200 seeded inputs in both typo modes. Existing Unicode/Bangla, multilingual aliases, playback, cloud, lyrics and library tests remain in the executed suite.
- Opt-in actual-Kotlin host benchmarks compare an unchanged detached NW121 checkout with this candidate, using identical harness, synthetic labels, runtime and heap. The comparison requires three fresh JVM processes per version and equal ordered-output hashes. See [NW122 protocol, raw receipts, measurements and limits](docs/NW122_PERFORMANCE_MEASUREMENT.md).
- Benchmark comparator positive/negative fixtures pass, rejecting changed runtime/harness/output/source, invalid timings and insufficient samples/processes. Timing thresholds are separate from correctness gates.

The complete one-click Windows builder passes production/Compose and Android instrumentation-source compilation, debug assembly, all JVM tests and APK validation. Independent source attestation matches **795 build-affecting files**. The candidate's certificate matches the saved NW121 APK; installed-phone continuity remains untested. Exact artifact hashes and final lint receipt are recorded in [NW122 evidence](docs/NW122_PERFORMANCE_MEASUREMENT.md).

- Final Android lint: **PASS — zero errors, 313 warnings, 0 hints**. No new suppression or warning baseline was introduced.

## Device boundary

Host-JVM numbers exclude Android ART, Compose frames, input-to-results/debounce, actual providers, decoding, startup and power conditions. No cold/warm startup, playback-next latency, Android allocation/jank/battery trace or 30-minute playback soak was executed. NW122 remains device-pending; small changes between direct-search timings may be noise.

Physical Samsung acceptance, installed-phone signer, production Google OAuth/restore/readback and rights/privacy approval remain open. Existing Android account-store tests are compiled only; no Android instrumentation tests were device-executed. Public release remains **NO-GO**. Next ordered source wave: **NW123 accessibility**.

Earlier results remain in [NW121 account/recovery evidence](docs/NW121_PREMIUM_ACCOUNT_RECOVERY_EVIDENCE.md), [NW120 audio evidence](docs/NW120_AUDIO_SESSION_EVIDENCE.md), [NW119 cleanup evidence](docs/NW119_CLEANUP_AND_LYRICS_EVIDENCE.md) and the [historical diagnostic inventory](docs/HISTORICAL_CI_SOURCE_CHECKS_20261027.md).
